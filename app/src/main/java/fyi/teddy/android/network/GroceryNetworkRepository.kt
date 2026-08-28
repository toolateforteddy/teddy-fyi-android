@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class, kotlinx.serialization.InternalSerializationApi::class)
package fyi.teddy.android.network

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteResponse(
    @SerialName("code") val code: String
)

@Serializable
data class JoinRequest(
    @SerialName("code") val code: String
)

@Serializable
data class JoinResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("list_id") val listId: String? = null
)

@Suppress("TooGenericExceptionCaught", "SwallowedException")
object GroceryNetworkRepository {
    suspend fun createInvite(listId: String): String? {
        return try {
            val response = NetworkClient.client.post(ApiRoutes.LIST_INVITE) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("list_id" to listId))
            }
            if (response.status.isSuccess()) {
                response.body<InviteResponse>().code
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun joinList(code: String): String? {
        return try {
            val response = NetworkClient.client.post(ApiRoutes.LIST_JOIN) {
                contentType(ContentType.Application.Json)
                setBody(JoinRequest(code))
            }
            if (response.status.isSuccess()) {
                response.body<JoinResponse>().listId
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
