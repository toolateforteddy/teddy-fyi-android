package fyi.teddy.android.network

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

data class SyncCounts(
    val todoLists: Int = 0,
    val todoItems: Int = 0,
    val groceryLists: Int = 0,
    val groceryMembers: Int = 0,
    val stores: Int = 0,
    val categories: Int = 0,
    val groceryItems: Int = 0,
    val storeInfos: Int = 0
)

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    private suspend fun recordSyncLog(
        status: String,
        startTime: Long,
        errorMessage: String? = null,
        sent: SyncCounts = SyncCounts(),
        received: SyncCounts = SyncCounts()
    ) {
        val durationMillis = System.currentTimeMillis() - startTime
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            db.syncLogDao().insert(
                SyncLog(
                    status = status,
                    durationMillis = durationMillis,
                    errorMessage = errorMessage,
                    todoListsSent = sent.todoLists,
                    todoItemsSent = sent.todoItems,
                    groceryListsSent = sent.groceryLists,
                    groceryMembersSent = sent.groceryMembers,
                    storesSent = sent.stores,
                    categoriesSent = sent.categories,
                    groceryItemsSent = sent.groceryItems,
                    storeInfosSent = sent.storeInfos,
                    todoListsReceived = received.todoLists,
                    todoItemsReceived = received.todoItems,
                    groceryListsReceived = received.groceryLists,
                    groceryMembersReceived = received.groceryMembers,
                    storesReceived = received.stores,
                    categoriesReceived = received.categories,
                    groceryItemsReceived = received.groceryItems,
                    storeInfosReceived = received.storeInfos
                )
            )
            val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            db.syncLogDao().pruneLogs(cutoff)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sync log to database", e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = syncMutex.withLock {
        NetworkClient.initialize(applicationContext)
        val startTime = System.currentTimeMillis()
        val workerId = id.toString().take(8)

        Log.d(TAG, "[$workerId] Starting synchronization worker...")

        if (shouldSkipMeteredSync(workerId)) return Result.success()

        val session = NetworkClient.session
        session.load(applicationContext)

        if (session.accessToken.isNullOrBlank()) {
            Log.w(TAG, "[$workerId] No auth token found, skipping sync.")
            recordSyncLog("FAILURE", startTime, "No auth token found.")
            return Result.failure()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val sessionUserId = session.userId ?: ""
        val lastSyncedAt = getOrMigrateLastSyncedAt(db, sessionUserId, workerId)
        val clientId = session.clientUuid
        if (clientId == null) {
            // session.load() normally mints one; if it somehow did not, crashing here would
            // put the worker into an endless WorkManager retry loop.
            Log.e(TAG, "[$workerId] No client UUID on the session, skipping sync.")
            recordSyncLog("FAILURE", startTime, "No client UUID on the session.")
            return Result.failure()
        }

        val isFirstSync = lastSyncedAt == null
        val sentCounts = collectLocalChanges(db, isFirstSync)

        val syncRequest = buildSyncRequest(db, isFirstSync, lastSyncedAt, clientId)

        Log.d(TAG, "[$workerId] Sending sync payload. Counts: $sentCounts")

        val response = try {
            NetworkClient.client.post(ApiRoutes.SYNC) {
                contentType(ContentType.Application.Json)
                setBody(syncRequest)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.toString()
            Log.e(TAG, "[$workerId] Network connection error during sync. Requesting retry.", e)
            recordSyncLog(
                status = "RETRY",
                startTime = startTime,
                errorMessage = errorMsg,
                sent = sentCounts
            )
            return Result.retry()
        }

        return handleServerResponse(response, db, sessionUserId, startTime, sentCounts, workerId)
    }

    private suspend fun shouldSkipMeteredSync(workerId: String): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isMetered = cm.isActiveNetworkMetered
        val isPeriodic = tags.contains("PERIODIC_SYNC")

        if (isPeriodic && isMetered) {
            val db = AppDatabase.getDatabase(applicationContext)
            val lastSuccess = db.syncLogDao().getLastSuccessTimestamp()
            val hoursSinceLastSync = (System.currentTimeMillis() - lastSuccess) / (1000 * 60 * 60)
            if (hoursSinceLastSync < 24) {
                Log.d(TAG, "[$workerId] Skipping periodic sync: Metered network and last success was $hoursSinceLastSync hours ago.")
                return true
            }
        }
        return false
    }

    private suspend fun getOrMigrateLastSyncedAt(db: AppDatabase, sessionUserId: String, workerId: String): String? {
        var lastSyncedAt = db.userSyncMetadataDao().getLastSyncedAt(sessionUserId)
        if (lastSyncedAt == null && sessionUserId.isNotBlank()) {
            val sharedPrefs = applicationContext.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
            val legacyLastSyncedAt = sharedPrefs.getString("last_synced_at", null)
            if (legacyLastSyncedAt != null) {
                Log.d(TAG, "[$workerId] Migrating legacy last_synced_at for user $sessionUserId")
                lastSyncedAt = legacyLastSyncedAt
                db.userSyncMetadataDao().upsert(fyi.teddy.android.data.UserSyncMetadata(sessionUserId, lastSyncedAt))
                sharedPrefs.edit { remove("last_synced_at") }
            }
        }
        return lastSyncedAt
    }

    private suspend fun collectLocalChanges(db: AppDatabase, isFirstSync: Boolean): SyncCounts {
        return SyncCounts(
            todoLists = TodoSyncManager.collectLocalListChanges(db, isFirstSync).size,
            todoItems = TodoSyncManager.collectLocalChanges(db, isFirstSync).size,
            groceryLists = GrocerySyncManager.collectLocalListChanges(db, isFirstSync).size,
            groceryMembers = GrocerySyncManager.collectLocalListMemberChanges(db, isFirstSync).size,
            stores = GrocerySyncManager.collectLocalStoreChanges(db, isFirstSync).size,
            categories = GrocerySyncManager.collectLocalCategoryChanges(db, isFirstSync).size,
            groceryItems = GrocerySyncManager.collectLocalChanges(db, isFirstSync).size,
            storeInfos = GrocerySyncManager.collectLocalStoreInfoChanges(db, isFirstSync).size
        )
    }

    private suspend fun buildSyncRequest(
        db: AppDatabase,
        isFirstSync: Boolean,
        lastSyncedAt: String?,
        clientId: String
    ): SyncRequest {
        return SyncRequest(
            lastSyncedAt = lastSyncedAt,
            clientId = clientId,
            todoListChanges = TodoSyncManager.collectLocalListChanges(db, isFirstSync),
            todoChanges = TodoSyncManager.collectLocalChanges(db, isFirstSync),
            groceryListChanges = GrocerySyncManager.collectLocalListChanges(db, isFirstSync),
            groceryListMemberChanges = GrocerySyncManager.collectLocalListMemberChanges(db, isFirstSync),
            storeChanges = GrocerySyncManager.collectLocalStoreChanges(db, isFirstSync),
            categoryChanges = GrocerySyncManager.collectLocalCategoryChanges(db, isFirstSync),
            groceryChanges = GrocerySyncManager.collectLocalChanges(db, isFirstSync),
            groceryItemStoreInfoChanges = GrocerySyncManager.collectLocalStoreInfoChanges(db, isFirstSync)
        )
    }

    @Suppress("TooGenericExceptionCaught", "LongParameterList", "LongMethod")
    private suspend fun handleServerResponse(
        response: io.ktor.client.statement.HttpResponse,
        db: AppDatabase,
        sessionUserId: String,
        startTime: Long,
        sentCounts: SyncCounts,
        workerId: String
    ): Result {
        val session = NetworkClient.session
        if (response.status.isSuccess()) {
            val syncResponse = response.body<SyncResponse>()
            val receivedCounts = SyncCounts(
                todoLists = syncResponse.remoteTodoListChanges.size,
                todoItems = syncResponse.remoteTodoChanges.size,
                groceryLists = syncResponse.remoteGroceryListChanges.size,
                groceryMembers = syncResponse.remoteGroceryListMemberChanges.size,
                stores = syncResponse.remoteStoreChanges.size,
                categories = syncResponse.remoteCategoryChanges.size,
                groceryItems = syncResponse.remoteGroceryChanges.size,
                storeInfos = syncResponse.remoteGroceryItemStoreInfoChanges.size
            )

            val isFirstSync = db.userSyncMetadataDao().getLastSyncedAt(sessionUserId) == null
            try {
                db.withTransaction {
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
                    db.userSyncMetadataDao().upsert(
                        fyi.teddy.android.data.UserSyncMetadata(
                            userId = sessionUserId,
                            lastSyncedAt = syncResponse.serverTimestamp
                        )
                    )
                }
                session.save(applicationContext)
                recordSyncLog(
                    status = "SUCCESS",
                    startTime = startTime,
                    sent = sentCounts,
                    received = receivedCounts
                )
                return Result.success()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val dbErrorMsg = "DB transaction failed: ${e.localizedMessage ?: e.toString()}"
                Log.e(TAG, "[$workerId] Database transaction failed during sync response processing.", e)
                recordSyncLog(
                    status = "RETRY",
                    startTime = startTime,
                    errorMessage = dbErrorMsg,
                    sent = sentCounts,
                    received = receivedCounts
                )
                return Result.retry()
            }
        } else {
            val errorBody = try { response.bodyAsText() } catch (_: Exception) { "could not read error body" }
            val errorMsg = "HTTP ${response.status.value}: $errorBody"
            Log.e(TAG, "[$workerId] Sync failed: $errorMsg")
            recordSyncLog("RETRY", startTime, errorMsg, sent = sentCounts)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "SyncWorker"
        private val syncMutex = Mutex()

        private fun isCharging(context: Context): Boolean {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        @Suppress("MagicNumber")
        private fun getBackoffCriteria(context: Context): Pair<BackoffPolicy, Long> {
            return if (isCharging(context)) {
                // When charging, we can afford a more frequent retry (start at 30s)
                BackoffPolicy.EXPONENTIAL to 30000L
            } else {
                // When on battery, use a much more aggressive exponential backoff (start at 5m)
                // to prevent battery drain when the backend is unreachable.
                BackoffPolicy.EXPONENTIAL to TimeUnit.MINUTES.toMillis(5)
            }
        }

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val (backoffPolicy, backoffDelay) = getBackoffCriteria(context)

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy,
                    backoffDelay,
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
         * Enqueues a sync with a debounce delay.
         * Longer delay when on battery to batch more changes and save power.
         */
        fun enqueueDebounced(context: Context) {
            val delaySeconds = 10L // Fixed 10s debounce for better responsiveness during active use
            enqueueDelayed(context, delaySeconds)
        }

        fun enqueueDelayed(context: Context, delaySeconds: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val (backoffPolicy, backoffDelay) = getBackoffCriteria(context)

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    backoffPolicy,
                    backoffDelay,
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

            val (backoffPolicy, backoffDelay) = getBackoffCriteria(context)

            val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                2, TimeUnit.HOURS
            )
                .addTag("PERIODIC_SYNC")
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy,
                    backoffDelay,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PeriodicSyncWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicSyncRequest
            )
        }

        /**
         * Checks if there are any local unsynced changes and enqueues a sync if so.
         */
        suspend fun enqueueIfNecessary(context: Context) {
            val session = NetworkClient.session
            session.load(context)
            if (session.accessToken.isNullOrBlank()) {
                Log.d(TAG, "No auth token found, skipping enqueueIfNecessary.")
                return
            }

            val db = AppDatabase.getDatabase(context)
            val sessionUserId = session.userId ?: ""
            val lastSyncedAt = if (sessionUserId.isBlank()) null else db.userSyncMetadataDao().getLastSyncedAt(sessionUserId)
            val isFirstSync = lastSyncedAt == null
            
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

        fun cancelAllSyncWork(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_NAME)
            wm.cancelUniqueWork("PeriodicSyncWorker")
        }
    }
}
