package fyi.teddy.android.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit

class UserSession {
    var userId by mutableStateOf<String?>(null)
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
    var profilePictureUri by mutableStateOf<String?>(null)
    var accessToken by mutableStateOf<String?>(null)
    var refreshToken by mutableStateOf<String?>(null)
    var clientUuid by mutableStateOf<String?>(null)

    val isLoggedIn: Boolean
        get() = accessToken != null

    suspend fun save(context: Context) {
        val encryptedStore = EncryptedDataStore(context)
        encryptedStore.saveEncrypted("user_id", userId)
        encryptedStore.saveEncrypted("user_name", userName)
        encryptedStore.saveEncrypted("id_token", idToken)
        encryptedStore.saveEncrypted("profile_pic", profilePictureUri)
        encryptedStore.saveEncrypted("access_token", accessToken)
        encryptedStore.saveEncrypted("refresh_token", refreshToken)
        encryptedStore.saveEncrypted("client_uuid", clientUuid)
    }

    suspend fun load(context: Context) {
        val encryptedStore = EncryptedDataStore(context)
        val loadedUserId = encryptedStore.getDecrypted("user_id")
        val loadedUserName = encryptedStore.getDecrypted("user_name")
        val loadedIdToken = encryptedStore.getDecrypted("id_token")
        val loadedProfilePic = encryptedStore.getDecrypted("profile_pic")
        val loadedAccessToken = encryptedStore.getDecrypted("access_token")
        val loadedRefreshToken = encryptedStore.getDecrypted("refresh_token")
        val loadedClientUuid = encryptedStore.getDecrypted("client_uuid")

        // Only overwrite if currently null or if the loaded value is not null
        // This avoids race conditions where a background load might overwrite 
        // a freshly set in-memory value during login with an old null from disk.
        if (loadedUserId != null) userId = loadedUserId
        if (loadedUserName != null) userName = loadedUserName
        if (loadedIdToken != null) idToken = loadedIdToken
        if (loadedProfilePic != null) profilePictureUri = loadedProfilePic
        if (loadedAccessToken != null) accessToken = loadedAccessToken
        if (loadedRefreshToken != null) refreshToken = loadedRefreshToken
        if (loadedClientUuid != null) clientUuid = loadedClientUuid

        // Ensure we always have a client UUID if we are loaded
        if (clientUuid == null) {
            val sharedPrefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
            val legacyId = sharedPrefs.getString("client_id", null)
            if (legacyId != null) {
                clientUuid = legacyId
            } else {
                clientUuid = java.util.UUID.randomUUID().toString()
                // Save it back immediately so it's persisted in the encrypted store
                save(context)
            }
        }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
        // Also clear sync metadata which might contain old client IDs or timestamps
        context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE).edit().clear().apply()

        userId = null
        userName = null
        idToken = null
        profilePictureUri = null
        accessToken = null
        refreshToken = null
        clientUuid = null
    }
}
