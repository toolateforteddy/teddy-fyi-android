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
        userId = encryptedStore.getDecrypted("user_id")
        userName = encryptedStore.getDecrypted("user_name")
        idToken = encryptedStore.getDecrypted("id_token")
        profilePictureUri = encryptedStore.getDecrypted("profile_pic")
        accessToken = encryptedStore.getDecrypted("access_token")
        refreshToken = encryptedStore.getDecrypted("refresh_token")
        clientUuid = encryptedStore.getDecrypted("client_uuid")
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
        userId = null
        userName = null
        idToken = null
        profilePictureUri = null
        accessToken = null
        refreshToken = null
        clientUuid = null
    }
}
