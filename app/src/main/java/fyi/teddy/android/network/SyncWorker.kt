package fyi.teddy.android.network

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.*
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.data.AppDatabase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private fun recordSyncSuccess() {
        val prefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_successful_sync_time_millis", System.currentTimeMillis())
            .putString("last_sync_status", "SUCCESS")
            .remove("last_sync_error_message")
            .putLong("last_sync_attempt_time_millis", System.currentTimeMillis())
            .apply()
    }

    private fun recordSyncFailure(errorMessage: String) {
        val prefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_sync_status", "ERROR")
            .putString("last_sync_error_message", errorMessage)
            .putLong("last_sync_attempt_time_millis", System.currentTimeMillis())
            .apply()
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting synchronization worker...")

        // 1. Load authenticated user session
        val session = UserSession()
        session.load(applicationContext)
        val idToken = session.idToken
        if (idToken == null) {
            Log.w(TAG, "No auth token found, skipping sync.")
            recordSyncFailure("No auth token found.")
            return Result.failure()
        }

        // 2. Fetch last synced timestamp and client ID
        val sharedPrefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        val lastSyncedAt = sharedPrefs.getString("last_synced_at", null)
        var clientId = sharedPrefs.getString("client_id", null)
        if (clientId == null) {
            clientId = java.util.UUID.randomUUID().toString()
            sharedPrefs.edit().putString("client_id", clientId).apply()
        }

        val db = AppDatabase.getDatabase(applicationContext)

        // 3. Collect local unsynced mutations from each domain independently
        val isFirstSync = lastSyncedAt == null
        val todoChanges = TodoSyncManager.collectLocalChanges(db, isFirstSync)
        val groceryChanges = GrocerySyncManager.collectLocalChanges(db, isFirstSync)

        val syncRequest = SyncRequest(
            last_synced_at = lastSyncedAt,
            client_id = clientId,
            todo_changes = todoChanges,
            grocery_changes = groceryChanges
        )

        Log.d(TAG, "Sending sync payload with ${todoChanges.size} todo changes and ${groceryChanges.size} grocery changes.")

        // 4. Execute the network transaction
        val response = try {
            NetworkClient.client.post("https://api-rust.teddy.fyi/api/sync") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(syncRequest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network connection error during sync. Requesting retry.", e)
            recordSyncFailure(e.localizedMessage ?: e.toString())
            return Result.retry()
        }

        // 5. Handle response and update local database
        if (response.status.isSuccess()) {
            val syncResponse = response.body<SyncResponse>()
            Log.d(TAG, "Sync succeeded. Server time: ${syncResponse.server_timestamp}")

            try {
                db.withTransaction {
                    // Process both domains independently inside a single atomic local transaction
                    TodoSyncManager.handleSyncSuccess(db, syncResponse.success_ids, syncResponse.remote_todo_changes, isFirstSync)
                    GrocerySyncManager.handleSyncSuccess(db, syncResponse.success_ids, syncResponse.remote_grocery_changes, isFirstSync)

                    // Overwrite local last_synced_at metadata key
                    sharedPrefs.edit()
                        .putString("last_synced_at", syncResponse.server_timestamp)
                        .commit()
                }
                Log.d(TAG, "Local database transaction successfully completed.")
                recordSyncSuccess()
                return Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Database transaction failed during sync response processing.", e)
                recordSyncFailure("DB transaction failed: ${e.localizedMessage ?: e.toString()}")
                return Result.retry()
            }
        } else if (response.status.value == 401) {
            Log.e(TAG, "Sync failed: 401 Unauthorized token.")
            recordSyncFailure("HTTP 401 Unauthorized token.")
            return Result.failure()
        } else {
            val errorBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                response.status.description
            }
            Log.e(TAG, "Sync failed with status code ${response.status.value}. Response: $errorBody")
            recordSyncFailure("HTTP ${response.status.value}: $errorBody")
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
