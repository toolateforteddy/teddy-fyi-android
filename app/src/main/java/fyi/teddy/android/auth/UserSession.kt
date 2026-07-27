package fyi.teddy.android.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

@Suppress("TooGenericExceptionCaught")
class UserSession(
    val tokenStorage: TokenStorage = TokenStorage()
) {
    var userId by mutableStateOf<String?>(null)
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
    var profilePictureUri by mutableStateOf<String?>(null)

    private var _accessToken by mutableStateOf<String?>(null)
    private var _refreshToken by mutableStateOf<String?>(null)

    var accessToken: String?
        get() = _accessToken
        set(value) {
            _accessToken = value
            tokenStorage.updateInMemoryTokens(_accessToken, _refreshToken)
        }

    var refreshToken: String?
        get() = _refreshToken
        set(value) {
            _refreshToken = value
            tokenStorage.updateInMemoryTokens(_accessToken, _refreshToken)
        }

    var clientUuid by mutableStateOf<String?>(null)

    @Suppress("unused")
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    fun updateTokens(access: String?, refresh: String?) {
        _accessToken = access
        _refreshToken = refresh
        tokenStorage.updateInMemoryTokens(access, refresh)
    }

    suspend fun save(context: Context, dataStore: DataStore<Preferences>? = null) {
        val encryptedStore = if (dataStore != null) EncryptedDataStore(context, dataStore) else EncryptedDataStore(context)
        try {
            encryptedStore.saveAllEncrypted(
                mapOf(
                    "user_id" to userId,
                    "user_name" to userName,
                    "id_token" to idToken,
                    "profile_pic" to profilePictureUri,
                    "access_token" to accessToken,
                    "refresh_token" to refreshToken,
                    "client_uuid" to clientUuid
                )
            )
            tokenStorage.saveTokens(context, accessToken, refreshToken, dataStore)
            AuthTelemetry.logBreadcrumb("USER_SESSION_SAVE", "UserSession saved to disk and TokenStorage")
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb("USER_SESSION_SAVE_EXCEPTION", "Exception: ${e.javaClass.simpleName}")
        }
    }

    suspend fun load(context: Context, dataStore: DataStore<Preferences>? = null) {
        val encryptedStore = if (dataStore != null) EncryptedDataStore(context, dataStore) else EncryptedDataStore(context)
        val loadedUserId = tryGetDecrypted(encryptedStore, "user_id")
        val loadedUserName = tryGetDecrypted(encryptedStore, "user_name")
        val loadedIdToken = tryGetDecrypted(encryptedStore, "id_token")
        val loadedProfilePic = tryGetDecrypted(encryptedStore, "profile_pic")

        val (loadedAccessToken, loadedRefreshToken) = tokenStorage.loadTokensFromDisk(context, dataStore)
        val loadedClientUuid = tryGetDecrypted(encryptedStore, "client_uuid")

        if (loadedUserId != null) userId = loadedUserId
        if (loadedUserName != null) userName = loadedUserName
        if (loadedIdToken != null) idToken = loadedIdToken
        if (loadedProfilePic != null) profilePictureUri = loadedProfilePic
        if (loadedAccessToken != null) _accessToken = loadedAccessToken
        if (loadedRefreshToken != null) _refreshToken = loadedRefreshToken
        if (loadedClientUuid != null) clientUuid = loadedClientUuid

        tokenStorage.updateInMemoryTokens(_accessToken, _refreshToken)

        if (clientUuid == null) {
            val sharedPrefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
            val legacyId = sharedPrefs.getString("client_id", null)
            if (legacyId != null) {
                clientUuid = legacyId
            } else {
                clientUuid = java.util.UUID.randomUUID().toString()
                save(context, dataStore)
            }
        }
    }

    private suspend fun tryGetDecrypted(store: EncryptedDataStore, key: String): String? {
        return try {
            store.getDecrypted(key)
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb("SESSION_READ_EXCEPTION", "Key: $key, Exception: ${e.javaClass.simpleName}")
            null
        }
    }

    suspend fun clear(context: Context, dataStore: DataStore<Preferences>? = null, reason: String = "User session cleared") {
        AuthTelemetry.flushBreadcrumbs(reason)

        val preservedUuid = clientUuid
        val sharedPrefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        val preservedLegacyId = sharedPrefs.getString("client_id", null)

        val ds = dataStore ?: context.dataStore
        try {
            ds.edit { it.clear() }
        } catch (e: Exception) {
            AuthTelemetry.logBreadcrumb("CLEAR_DATASTORE_EXCEPTION", "Exception: ${e.javaClass.simpleName}")
        }
        sharedPrefs.edit { clear() }

        userId = null
        userName = null
        idToken = null
        profilePictureUri = null
        _accessToken = null
        _refreshToken = null
        tokenStorage.clear(context, ds)

        if (preservedUuid != null) {
            clientUuid = preservedUuid
            save(context, ds)
        } else if (preservedLegacyId != null) {
            clientUuid = preservedLegacyId
            save(context, ds)
        } else {
            clientUuid = null
        }

        if (preservedLegacyId != null) {
            sharedPrefs.edit(commit = true) { putString("client_id", preservedLegacyId) }
        }
    }
}
