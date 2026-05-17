package fyi.teddy.android.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UserSession {
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
    var profilePictureUri by mutableStateOf<String?>(null)

    fun save(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", userName)
            putString("id_token", idToken)
            putString("profile_pic", profilePictureUri)
            apply()
        }
    }

    fun load(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userName = sharedPref.getString("user_name", null)
        idToken = sharedPref.getString("id_token", null)
        profilePictureUri = sharedPref.getString("profile_pic", null)
    }

    fun clear(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        userName = null
        idToken = null
        profilePictureUri = null
    }
}
