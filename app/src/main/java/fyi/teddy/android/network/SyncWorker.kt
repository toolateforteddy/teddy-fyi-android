package fyi.teddy.android.network

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.*
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.data.AppDatabase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting synchronization worker...")

        // 1. Load authenticated user session
        val session = UserSession()
        session.load(applicationContext)
        val idToken = session.idToken
        if (idToken == null) {
            Log.w(TAG, "No auth token found, skipping sync.")
            return Result.failure()
        }

        // 2. Fetch last synced timestamp
        val sharedPrefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        val lastSyncedAt = sharedPrefs.getString("last_synced_at", null)

        val db = AppDatabase.getDatabase(applicationContext)
        val todoDao = db.todoDao()

        // 3. Collect local unsynced mutations inside a read block
        val isFirstSync = lastSyncedAt == null
        val unsyncedItems = if (isFirstSync) {
            todoDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedItems()
        }
        val unsyncedLists = if (isFirstSync) {
            todoDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedLists()
        }

        val syncRequest = SyncRequest(
            last_synced_at = lastSyncedAt,
            todo_changes = TodoChangesDto(
                items = unsyncedItems.map { it.toDto() },
                lists = unsyncedLists.map { it.toDto() }
            ),
            grocery_changes = GroceryChangesDto() // Placeholder for future grocery sync integration
        )

        Log.d(TAG, "Sending sync payload with ${unsyncedItems.size} items and ${unsyncedLists.size} lists.")

        // 4. Execute the network transaction
        val response = try {
            NetworkClient.client.post("https://api-rust.teddy.fyi/api/sync") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(syncRequest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network connection error during sync. Requesting retry.", e)
            return Result.retry()
        }

        // 5. Handle response and update local database
        if (response.status.isSuccess()) {
            val syncResponse = response.body<SyncResponse>()
            Log.d(TAG, "Sync succeeded. Server time: ${syncResponse.server_time}")

            try {
                db.withTransaction {
                    // Transition successfully uploaded items back to sync_state = SYNCED
                    unsyncedItems.forEach { localItem ->
                        if (localItem.isDeleted) {
                            todoDao.hardDeleteItem(localItem.id)
                        } else {
                            // Check if the server returned an updated version of this item
                            val remoteItem = syncResponse.remote_changes.todo_changes.items.find { it.id == localItem.id }
                            if (remoteItem == null) {
                                todoDao.insertItem(localItem.copy(syncState = "SYNCED"))
                            }
                        }
                    }

                    unsyncedLists.forEach { localList ->
                        if (localList.isDeleted) {
                            todoDao.hardDeleteList(localList.id)
                        } else {
                            val remoteList = syncResponse.remote_changes.todo_changes.lists.find { it.id == localList.id }
                            if (remoteList == null) {
                                todoDao.insertList(localList.copy(syncState = "SYNCED"))
                            }
                        }
                    }

                    // Upsert incoming remote_changes into local Room DB
                    syncResponse.remote_changes.todo_changes.items.forEach { itemDto ->
                        if (itemDto.is_deleted) {
                            todoDao.hardDeleteItem(itemDto.id)
                        } else {
                            todoDao.insertItem(itemDto.toEntity().copy(syncState = "SYNCED"))
                        }
                    }

                    syncResponse.remote_changes.todo_changes.lists.forEach { listDto ->
                        if (listDto.is_deleted) {
                            todoDao.hardDeleteList(listDto.id)
                        } else {
                            todoDao.insertList(listDto.toEntity().copy(syncState = "SYNCED"))
                        }
                    }

                    // Overwrite local last_synced_at metadata key
                    sharedPrefs.edit()
                        .putString("last_synced_at", syncResponse.server_time)
                        .commit()
                }
                Log.d(TAG, "Local database transaction successfully completed.")
                return Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Database transaction failed during sync response processing.", e)
                return Result.retry()
            }
        } else if (response.status.value == 401) {
            Log.e(TAG, "Sync failed: 401 Unauthorized token.")
            return Result.failure()
        } else {
            Log.e(TAG, "Sync failed with status code ${response.status.value}. Requesting retry.")
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "SyncWorker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS, // 10 seconds
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }
    }
}
