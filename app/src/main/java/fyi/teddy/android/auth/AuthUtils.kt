package fyi.teddy.android.auth

import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.MessageDigest

object AuthUtils {
    private const val TAG = "AuthUtils"

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.lowercase().trim().toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun extractUserIdFromToken(idToken: String): String? {
        try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payloadRaw = parts[1]
            val decodedBytes = Base64.decode(payloadRaw, Base64.URL_SAFE)
            val payload = String(decodedBytes, Charset.forName("UTF-8"))
            val json = JSONObject(payload)
            val sub = json.optString("sub", "")
            return if (sub.isNotEmpty()) sub else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract sub from token", e)
        }
        return null
    }

    fun extractPictureFromToken(idToken: String): Uri? {
        try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payloadRaw = parts[1]
            val decodedBytes = Base64.decode(payloadRaw, Base64.URL_SAFE)
            val payload = String(decodedBytes, Charset.forName("UTF-8"))
            val json = JSONObject(payload)
            
            // 1. Try 'picture' claim
            val picture = json.optString("picture", "")
            if (picture.isNotEmpty()) {
                return Uri.parse(picture)
            }

            // 2. Try Gravatar fallback if email is present
            val email = json.optString("email", "")
            if (email.isNotEmpty()) {
                val hash = md5(email)
                return Uri.parse("https://www.gravatar.com/avatar/$hash?d=identicon&s=200")
            }

            // 3. Try 'sub' claim fallback
            val sub = json.optString("sub", "")
            if (sub.isNotEmpty()) {
                return Uri.parse("https://www.google.com/s2/photos/profile/$sub")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract picture from token", e)
        }
        return null
    }
}
