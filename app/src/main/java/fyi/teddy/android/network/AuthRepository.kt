package fyi.teddy.android.network

import android.content.Context
import fyi.teddy.android.auth.UserSession
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LoginRequest(val user_id: String, val client_uuid: String, val google_auth_token: String)

@Serializable
data class TokenResponse(val access_token: String, val refresh_token: String)

object AuthRepository {
    suspend fun login(context: Context, session: UserSession, googleToken: String): Boolean {
        return try {
            val clientUuid = session.clientUuid ?: UUID.randomUUID().toString()
            val userId = session.userId ?: "unknown"
            
            val response = NetworkClient.client.post("https://api-rust.teddy.fyi/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(userId, clientUuid, googleToken))
            }
            
            if (response.status.value in 200..299) {
                val tokens = response.body<TokenResponse>()
                session.accessToken = tokens.access_token
                session.refreshToken = tokens.refresh_token
                session.clientUuid = clientUuid
                session.save(context)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
