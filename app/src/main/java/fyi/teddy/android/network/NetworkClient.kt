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
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fyi.teddy.android.auth.UserSession

object NetworkClient {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getAuthTimeoutSecs(context: Context): Long {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isOnWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return if (isOnWifi) 60L else 3600L
    }

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
            json(syncJson)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = session.accessToken
                    if (!token.isNullOrBlank()) {
                        BearerTokens(token, session.refreshToken ?: "")
                    } else {
                        null
                    }
                }
                sendWithoutRequest { true }
                refreshTokens {
                    // Try to refresh the token using an internal client instance to avoid recursion
                    val refreshClient = HttpClient(OkHttp) {
                        install(ContentNegotiation) { json() }
                    }
                    
                    try {
                        val response = refreshClient.post("https://api-rust.teddy.fyi/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                RefreshRequest(
                                    userId = session.userId ?: "",
                                    clientUuid = session.clientUuid ?: "",
                                    refreshToken = session.refreshToken ?: "",
                                    expiresInSecs = appContext?.let { getAuthTimeoutSecs(it) } ?: 3600L,
                                )
                            )
                        }

                        if (response.status.value in 200..299) {
                            val tokens = response.body<TokenResponse>()
                            session.accessToken = tokens.accessToken
                            session.refreshToken = tokens.refreshToken
                            BearerTokens(tokens.accessToken, tokens.refreshToken)
                        } else {
                            if (response.status.value == 401) {
                                // Terminal auth failure: clear tokens in memory
                                session.accessToken = null
                                session.refreshToken = null
                            }
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
