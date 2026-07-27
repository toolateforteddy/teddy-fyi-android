package fyi.teddy.android.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooGenericExceptionCaught")
class TokenStorage(
    private val defaultDataStore: DataStore<Preferences>? = null
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var isInitialized: Boolean = false

    suspend fun getAccessToken(context: Context, dataStore: DataStore<Preferences>? = null): String? = mutex.withLock {
        val targetDs = dataStore ?: defaultDataStore
        if (!isInitialized) {
            loadTokensFromDiskLocked(context, targetDs)
        }
        cachedAccessToken
    }

    suspend fun getRefreshToken(context: Context, dataStore: DataStore<Preferences>? = null): String? = mutex.withLock {
        val targetDs = dataStore ?: defaultDataStore
        if (!isInitialized) {
            loadTokensFromDiskLocked(context, targetDs)
        }
        cachedRefreshToken
    }

    suspend fun saveTokens(
        context: Context,
        accessToken: String?,
        refreshToken: String?,
        dataStore: DataStore<Preferences>? = null
    ) = mutex.withLock {
        val targetDs = dataStore ?: defaultDataStore
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        isInitialized = true

        val store = if (targetDs != null) EncryptedDataStore(context, targetDs) else EncryptedDataStore(context)
        try {
            store.saveAllEncrypted(
                mapOf(
                    "access_token" to accessToken,
                    "refresh_token" to refreshToken
                )
            )
            AuthTelemetry.logBreadcrumb(
                "TOKEN_SAVE_SUCCESS",
                "Access: ${AuthTelemetry.maskToken(accessToken)}, Refresh: ${AuthTelemetry.maskToken(refreshToken)}"
            )
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb(
                "TOKEN_SAVE_EXCEPTION",
                "Exception: ${e.javaClass.simpleName}"
            )
        }
    }

    suspend fun loadTokensFromDisk(
        context: Context,
        dataStore: DataStore<Preferences>? = null
    ): Pair<String?, String?> = mutex.withLock {
        val targetDs = dataStore ?: defaultDataStore
        loadTokensFromDiskLocked(context, targetDs)
        Pair(cachedAccessToken, cachedRefreshToken)
    }

    private suspend fun loadTokensFromDiskLocked(
        context: Context,
        dataStore: DataStore<Preferences>?
    ) {
        val store = if (dataStore != null) EncryptedDataStore(context, dataStore) else EncryptedDataStore(context)
        val loadedAccess = readKeyWithRetry(store, "access_token")
        val loadedRefresh = readKeyWithRetry(store, "refresh_token")

        if (loadedAccess != null || loadedRefresh != null || !isInitialized) {
            cachedAccessToken = loadedAccess ?: cachedAccessToken
            cachedRefreshToken = loadedRefresh ?: cachedRefreshToken
            isInitialized = true
        }
    }

    private suspend fun readKeyWithRetry(
        encryptedStore: EncryptedDataStore,
        keyName: String
    ): String? {
        return try {
            val value = encryptedStore.getDecrypted(keyName)
            AuthTelemetry.logBreadcrumb("STORAGE_READ_SUCCESS", "Key: $keyName")
            value
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb(
                "STORAGE_READ_EXCEPTION",
                "Key: $keyName, Exception: ${e.javaClass.simpleName}, retrying in 50ms"
            )
            delay(50.milliseconds)
            try {
                val value = encryptedStore.getDecrypted(keyName)
                AuthTelemetry.logBreadcrumb("STORAGE_READ_RETRY_SUCCESS", "Key: $keyName")
                value
            } catch (retryEx: Exception) {
                AuthTelemetry.logBreadcrumb(
                    "STORAGE_READ_RETRY_FAILED",
                    "Key: $keyName, Exception: ${retryEx.javaClass.simpleName}"
                )
                null
            }
        }
    }

    suspend fun clear(
        context: Context,
        dataStore: DataStore<Preferences>? = null
    ) = mutex.withLock {
        cachedAccessToken = null
        cachedRefreshToken = null
        isInitialized = false
        val targetDs = dataStore ?: defaultDataStore
        val store = if (targetDs != null) EncryptedDataStore(context, targetDs) else EncryptedDataStore(context)
        try {
            store.saveAllEncrypted(
                mapOf(
                    "access_token" to null,
                    "refresh_token" to null
                )
            )
            AuthTelemetry.logBreadcrumb("TOKEN_STORAGE_CLEAR", "Tokens cleared from storage and cache")
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb("TOKEN_STORAGE_CLEAR_EXCEPTION", "Exception: ${e.javaClass.simpleName}")
        }
    }

    fun updateInMemoryTokens(accessToken: String?, refreshToken: String?) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        isInitialized = true
    }
}
