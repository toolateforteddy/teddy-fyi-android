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
    lateinit var session: UserSession

    var client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L
            connectTimeoutMillis = 60000L
            socketTimeoutMillis = 60000L
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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
                            session.accessToken = tokens.access_token
                            session.refreshToken = tokens.refresh_token
                            BearerTokens(tokens.access_token, tokens.refresh_token)
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
