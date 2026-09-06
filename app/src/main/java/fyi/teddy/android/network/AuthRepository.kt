@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class, kotlinx.serialization.InternalSerializationApi::class)
package fyi.teddy.android.network

import android.content.Context
import fyi.teddy.android.auth.AuthUtils
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
    @SerialName("google_auth_token") val googleAuthToken: String,
    @SerialName("expires_in_secs") val expiresInSecs: Long? = null,
)

@Serializable
data class RefreshRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("client_uuid") val clientUuid: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in_secs") val expiresInSecs: Long? = null,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    /**
     * The account's surrogate id. Optional on the wire and omitted by a server that has not
     * shipped it, so null means "not told" rather than "the account has none". See
     * [fyi.teddy.android.data.UserIdMigration].
     */
    @SerialName("user_uuid") val userUuid: String? = null
)

@Suppress("TooGenericExceptionCaught")
object AuthRepository {
    suspend fun login(context: Context, session: UserSession, googleToken: String): Boolean {
        return try {
            val clientUuid = session.clientUuid ?: UUID.randomUUID().toString()
            
            // Ensure we have a valid userId from the token if not already in session
            val userIdValue = session.userId ?: AuthUtils.extractUserIdFromToken(googleToken)
            if (userIdValue == null) {
                android.util.Log.e("AuthRepository", "Could not extract userId from token")
                return false
            }

            // Use a separate client for login to avoid "priming" the main NetworkClient's
            // Auth plugin with a null token before we've actually logged in.
            val loginClient = NetworkClient.loginClient
            
            val response = loginClient.post(ApiRoutes.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(
                    LoginRequest(
                        userId = userIdValue,
                        clientUuid = clientUuid,
                        googleAuthToken = googleToken,
                        expiresInSecs = NetworkClient.getAuthTimeoutSecs(context)
                    )
                )
            }
            
            if (response.status.value in 200..299) {
                val tokens = response.body<TokenResponse>()
                session.userId = userIdValue
                // What the server is told, kept apart from the local key it is currently equal
                // to; see [UserSession.authUserId].
                session.authUserId = userIdValue
                tokens.userUuid?.takeIf { it.isNotBlank() }?.let { session.userUuid = it }
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
