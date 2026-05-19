package fyi.teddy.android.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Suppress("DEPRECATION")
class UserSession {
    var userId by mutableStateOf<String?>(null)
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
    var profilePictureUri by mutableStateOf<String?>(null)

    private fun getEncryptedSharedPreferences(context: Context) = EncryptedSharedPreferences.create(
        context,
        "user_session_encrypted",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun migrateIfNecessary(context: Context) {
        val oldPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        if (oldPref.all.isNotEmpty()) {
            val newPref = getEncryptedSharedPreferences(context)
            oldPref.all.forEach { (key, value) ->
                when (value) {
                    is String -> newPref.edit().putString(key, value).apply()
                }
            }
            oldPref.edit().clear().apply()
        }
    }

    fun save(context: Context) {
        val sharedPref = getEncryptedSharedPreferences(context)
        with(sharedPref.edit()) {
            putString("user_id", userId)
            putString("user_name", userName)
            putString("id_token", idToken)
            putString("profile_pic", profilePictureUri)
            apply()
        }
    }

    fun load(context: Context) {
        migrateIfNecessary(context)
        val sharedPref = getEncryptedSharedPreferences(context)
        userId = sharedPref.getString("user_id", null)
        userName = sharedPref.getString("user_name", null)
        idToken = sharedPref.getString("id_token", null)
        profilePictureUri = sharedPref.getString("profile_pic", null)
    }

    fun clear(context: Context) {
        val sharedPref = getEncryptedSharedPreferences(context)
        sharedPref.edit().clear().apply()
        userId = null
        userName = null
        idToken = null
        profilePictureUri = null
    }
}
