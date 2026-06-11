package fyi.teddy.android.network

import android.content.Context
import fyi.teddy.android.auth.UserSession
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LoginRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("client_uuid") val clientUuid: String,
    @SerialName("google_auth_token") val googleAuthToken: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

object AuthRepository {
    suspend fun login(context: Context, session: UserSession, googleToken: String): Boolean {
        return try {
            val clientUuid = session.clientUuid ?: UUID.randomUUID().toString()
            val userIdValue = session.userId ?: "unknown"
            
            val response = NetworkClient.client.post("https://api-rust.teddy.fyi/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(userIdValue, clientUuid, googleToken))
            }
            
            if (response.status.value in 200..299) {
                val tokens = response.body<TokenResponse>()
                session.accessToken = tokens.accessToken
                session.refreshToken = tokens.refreshToken
                session.clientUuid = clientUuid
                session.save(context)
                true
            } else {
                android.util.Log.e("AuthRepository", "Login failed with status: ${response.status}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Login exception", e)
            false
        }
    }
}
