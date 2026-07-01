package fyi.teddy.android.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import fyi.teddy.android.auth.UserSession

object NetworkClient {
    val session = UserSession()

    val syncJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    var client = createClient()

    fun resetClient() {
        client.close()
        client = createClient()
    }

    private fun createClient() = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000L
            connectTimeoutMillis = 30000L
            socketTimeoutMillis = 30000L
        }
        install(ContentNegotiation) {
            json(syncJson)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    session.accessToken?.let { BearerTokens(it, session.refreshToken ?: "") }
                }
                refreshTokens {
                    // Try to refresh the token using an internal client instance to avoid recursion
                    val refreshClient = HttpClient(OkHttp) {
                        install(ContentNegotiation) { json() }
                    }
                    
                    try {
                        val response = refreshClient.post("https://api-rust.teddy.fyi/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf(
                                "user_id" to (session.userId ?: ""),
                                "client_uuid" to (session.clientUuid ?: ""),
                                "refresh_token" to (session.refreshToken ?: "")
                            ))
                        }

                        if (response.status.value in 200..299) {
                            val tokens = response.body<TokenResponse>()
                            session.accessToken = tokens.accessToken
                            session.refreshToken = tokens.refreshToken
                            BearerTokens(tokens.accessToken, tokens.refreshToken)
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    } finally {
                        refreshClient.close()
                    }
                }
            }
        }
        defaultRequest {
            session.clientUuid?.let { header("X-Client-UUID", it) }
        }
    }
}
