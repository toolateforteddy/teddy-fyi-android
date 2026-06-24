package fyi.teddy.android.network

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.*
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.data.SyncLog
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private suspend fun recordSyncLog(
        status: String,
        startTime: Long,
        errorMessage: String? = null,
        todoChangesSent: Int = 0,
        groceryChangesSent: Int = 0,
        todoChangesReceived: Int = 0,
        groceryChangesReceived: Int = 0
    ) {
        val durationMillis = System.currentTimeMillis() - startTime
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            db.syncLogDao().insert(
                SyncLog(
                    status = status,
                    durationMillis = durationMillis,
                    errorMessage = errorMessage,
                    todoChangesSent = todoChangesSent,
                    groceryChangesSent = groceryChangesSent,
                    todoChangesReceived = todoChangesReceived,
                    groceryChangesReceived = groceryChangesReceived
                )
            )
            // Prune logs older than 7 days
            val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            db.syncLogDao().pruneLogs(cutoff)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sync log to database", e)
        }
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "Starting synchronization worker...")

        // 1. Load authenticated user session
        val session = NetworkClient.session
        session.load(applicationContext)

        val idToken = session.idToken
        if (idToken == null) {
            val errorMsg = "No auth token found."
            Log.w(TAG, "No auth token found, skipping sync.")
            recordSyncLog("FAILURE", startTime, errorMsg)
            return Result.failure()
        }

        // 2. Fetch last synced timestamp and client ID
        val sharedPrefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        val lastSyncedAt = sharedPrefs.getString("last_synced_at", null)
        var clientId = sharedPrefs.getString("client_id", null)
        if (clientId == null) {
            clientId = java.util.UUID.randomUUID().toString()
            sharedPrefs.edit { putString("client_id", clientId) }
        }

        val db = AppDatabase.getDatabase(applicationContext)

        // 3. Collect local unsynced mutations from each domain independently
        val isFirstSync = lastSyncedAt == null
        val todoListChanges = TodoSyncManager.collectLocalListChanges(db, isFirstSync)
        val todoChanges = TodoSyncManager.collectLocalChanges(db, isFirstSync)
        val groceryListChanges = GrocerySyncManager.collectLocalListChanges(db, isFirstSync)
        val groceryListMemberChanges = GrocerySyncManager.collectLocalListMemberChanges(db, isFirstSync)
        val storeChanges = GrocerySyncManager.collectLocalStoreChanges(db, isFirstSync)
        val categoryChanges = GrocerySyncManager.collectLocalCategoryChanges(db, isFirstSync)
        val groceryChanges = GrocerySyncManager.collectLocalChanges(db, isFirstSync)
        val groceryItemStoreInfoChanges = GrocerySyncManager.collectLocalStoreInfoChanges(db, isFirstSync)

        val todoListChangesSent = todoListChanges.size
        val todoChangesSent = todoChanges.size
        val groceryListChangesSent = groceryListChanges.size
        val groceryListMemberChangesSent = groceryListMemberChanges.size
        val storeChangesSent = storeChanges.size
        val categoryChangesSent = categoryChanges.size
        val groceryChangesSent = groceryChanges.size
        val groceryItemStoreInfoChangesSent = groceryItemStoreInfoChanges.size

        val syncRequest = SyncRequest(
            lastSyncedAt = lastSyncedAt,
            clientId = clientId,
            todoListChanges = todoListChanges,
            todoChanges = todoChanges,
            groceryListChanges = groceryListChanges,
            groceryListMemberChanges = groceryListMemberChanges,
            storeChanges = storeChanges,
            categoryChanges = categoryChanges,
            groceryChanges = groceryChanges,
            groceryItemStoreInfoChanges = groceryItemStoreInfoChanges
        )

        Log.d(
            TAG,
            "Sending sync payload. Counts: [todoLists: $todoListChangesSent, todoItems: $todoChangesSent, groceryLists: $groceryListChangesSent, groceryMembers: $groceryListMemberChangesSent, stores: $storeChangesSent, categories: $categoryChangesSent, groceryItems: $groceryChangesSent, storeInfos: $groceryItemStoreInfoChangesSent]"
        )

        // 4. Execute the network transaction
        val response = try {
            NetworkClient.client.post("https://api-rust.teddy.fyi/api/sync") {
                // Remove manual Authorization header; Ktor's Auth plugin will handle it
                contentType(ContentType.Application.Json)
                setBody(syncRequest)
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.toString()
            Log.e(TAG, "Network connection error during sync. Requesting retry.", e)
            recordSyncLog(
                status = "RETRY",
                startTime = startTime,
                errorMessage = errorMsg,
                todoChangesSent = todoChangesSent,
                groceryChangesSent = groceryChangesSent
            )
            return Result.retry()
        }

        // 5. Handle response and update local database
        if (response.status.isSuccess()) {
            val syncResponse = response.body<SyncResponse>()
            val todoListChangesReceived = syncResponse.remoteTodoListChanges.size
            val todoChangesReceived = syncResponse.remoteTodoChanges.size
            val groceryListChangesReceived = syncResponse.remoteGroceryListChanges.size
            val groceryListMemberChangesReceived = syncResponse.remoteGroceryListMemberChanges.size
            val storeChangesReceived = syncResponse.remoteStoreChanges.size
            val categoryChangesReceived = syncResponse.remoteCategoryChanges.size
            val groceryChangesReceived = syncResponse.remoteGroceryChanges.size
            val groceryItemStoreInfoChangesReceived = syncResponse.remoteGroceryItemStoreInfoChanges.size

            Log.d(
                TAG,
                "Sync succeeded. Server time: ${syncResponse.serverTimestamp}. Received: [todoLists: $todoListChangesReceived, todoItems: $todoChangesReceived, groceryLists: $groceryListChangesReceived, groceryMembers: $groceryListMemberChangesReceived, stores: $storeChangesReceived, categories: $categoryChangesReceived, groceryItems: $groceryChangesReceived, storeInfos: $groceryItemStoreInfoChangesReceived]"
            )

            try {
                db.withTransaction {
                    // Process both domains independently inside a single atomic local transaction
                    TodoSyncManager.handleSyncSuccess(
                        db = db,
                        successIds = syncResponse.successIds,
                        remoteChanges = syncResponse.remoteTodoChanges,
                        remoteListChanges = syncResponse.remoteTodoListChanges,
                        isFirstSync = isFirstSync
                    )
                    GrocerySyncManager.handleSyncSuccess(
                        db = db,
                        successIds = syncResponse.successIds,
                        remoteChanges = syncResponse.remoteGroceryChanges,
                        remoteStoreChanges = syncResponse.remoteStoreChanges,
                        remoteCategoryChanges = syncResponse.remoteCategoryChanges,
                        remoteListChanges = syncResponse.remoteGroceryListChanges,
                        remoteListMemberChanges = syncResponse.remoteGroceryListMemberChanges,
                        remoteStoreInfoChanges = syncResponse.remoteGroceryItemStoreInfoChanges,
                        isFirstSync = isFirstSync
                    )

                    // Overwrite local last_synced_at metadata key
                    sharedPrefs.edit(commit = true) {
                        putString("last_synced_at", syncResponse.serverTimestamp)
                    }
                }
                Log.d(TAG, "Local database transaction successfully completed.")
                session.save(applicationContext)
                recordSyncLog(
                    status = "SUCCESS",
                    startTime = startTime,
                    todoChangesSent = todoChangesSent,
                    groceryChangesSent = groceryChangesSent,
                    todoChangesReceived = todoChangesReceived,
                    groceryChangesReceived = groceryChangesReceived
                )
                return Result.success()
            } catch (e: Exception) {
                val dbErrorMsg = "DB transaction failed: ${e.localizedMessage ?: e.toString()}"
                Log.e(TAG, "Database transaction failed during sync response processing.", e)
                recordSyncLog(
                    status = "RETRY",
                    startTime = startTime,
                    errorMessage = dbErrorMsg,
                    todoChangesSent = todoChangesSent,
                    groceryChangesSent = groceryChangesSent,
                    todoChangesReceived = todoChangesReceived,
                    groceryChangesReceived = groceryChangesReceived
                )
                return Result.retry()
            }
        } else if (response.status.value == 401) {
            val unauthMsg = "HTTP 401 Unauthorized token."
            Log.e(TAG, "Sync failed: $unauthMsg")
            recordSyncLog(
                status = "FAILURE",
                startTime = startTime,
                errorMessage = unauthMsg,
                todoChangesSent = todoChangesSent,
                groceryChangesSent = groceryChangesSent
            )
            return Result.failure()
        } else {
            val errorBody = try {
                response.bodyAsText()
            } catch (_: Exception) {
                response.status.description
            }
            val httpErrorMsg = "HTTP ${response.status.value}: $errorBody"
            Log.e(TAG, "Sync failed with status code ${response.status.value}. Response: $errorBody")
            recordSyncLog(
                status = "RETRY",
                startTime = startTime,
                errorMessage = httpErrorMsg,
                todoChangesSent = todoChangesSent,
                groceryChangesSent = groceryChangesSent
            )
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

        /**
         * Enqueues a sync with a short debounce delay (30 seconds).
         * Subsequent calls will replace the existing work, effectively pushing back the timer.
         */
        fun enqueueDebounced(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }

        /**
         * Schedules a periodic sync to ensure remote changes are pulled even if no local changes occur.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PeriodicSyncWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
        }

        /**
         * Checks if there are any local unsynced changes and enqueues a sync if so.
         */
        suspend fun enqueueIfNecessary(context: Context) {
            val db = AppDatabase.getDatabase(context)
            val sharedPrefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
            val isFirstSync = sharedPrefs.getString("last_synced_at", null) == null
            
            val hasChanges = if (isFirstSync) true else {
                db.todoDao().getUnsyncedItems().isNotEmpty() ||
                db.todoDao().getUnsyncedLists().isNotEmpty() ||
                db.groceryDao().getUnsyncedItems().isNotEmpty() ||
                db.groceryDao().getUnsyncedLists().isNotEmpty() ||
                db.groceryDao().getUnsyncedStores().isNotEmpty() ||
                db.groceryDao().getUnsyncedCategories().isNotEmpty() ||
                db.groceryDao().getUnsyncedListMembers().isNotEmpty() ||
                db.groceryDao().getUnsyncedStoreInfos().isNotEmpty()
            }

            if (hasChanges) {
                Log.d(TAG, "Unsynced changes detected on startup, enqueuing sync.")
                enqueue(context)
            } else {
                Log.d(TAG, "No unsynced changes detected on startup.")
            }
        }
    }
}
