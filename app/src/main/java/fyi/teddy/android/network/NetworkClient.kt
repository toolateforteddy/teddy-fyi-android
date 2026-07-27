package fyi.teddy.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fyi.teddy.android.auth.AuthTelemetry
import fyi.teddy.android.auth.UserSession
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber", "TooGenericExceptionCaught", "LongMethod", "CyclomaticComplexMethod", "NewApi")
object NetworkClient {
    private var appContext: Context? = null
    private val refreshMutex = Mutex()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getAuthTimeoutSecs(context: Context): Long {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isOnWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return if (isOnWifi) 60L else 3600L
    }

    var session = UserSession()

    val syncJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    var refreshClientFactory: () -> HttpClient = {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(syncJson) }
        }
    }

    var client = createClient()
    var loginClient = createLoginClient()

    fun resetClient() {
        client.close()
        client = createClient()
        loginClient.close()
        loginClient = createLoginClient()
    }

    private fun createLoginClient() = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(syncJson)
        }
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
                    val ctx = appContext
                    val access = if (ctx != null) session.tokenStorage.getAccessToken(ctx) ?: session.accessToken else session.accessToken
                    val refresh = if (ctx != null) session.tokenStorage.getRefreshToken(ctx) ?: session.refreshToken else session.refreshToken
                    AuthTelemetry.logBreadcrumb(
                        "LOAD_TOKENS",
                        "Access: ${AuthTelemetry.maskToken(access)}, Refresh: ${AuthTelemetry.maskToken(refresh)}"
                    )
                    if (!access.isNullOrBlank()) {
                        BearerTokens(access, refresh ?: "")
                    } else {
                        null
                    }
                }
                sendWithoutRequest { true }
                refreshTokens {
                    performRefreshToken(oldTokens?.accessToken)
                }
            }
        }
        defaultRequest {
            session.clientUuid?.let { header("X-Client-UUID", it) }
        }
    }

    suspend fun performRefreshToken(failedAccessToken: String? = null): BearerTokens? {
        return refreshMutex.withLock {
            val ctx = appContext
            val currentAccess = if (ctx != null) session.tokenStorage.getAccessToken(ctx) ?: session.accessToken else session.accessToken
            val currentRefresh = if (ctx != null) session.tokenStorage.getRefreshToken(ctx) ?: session.refreshToken else session.refreshToken

            // 1. Stale token check: if token was already refreshed by another thread while waiting
            if (!failedAccessToken.isNullOrBlank() && !currentAccess.isNullOrBlank() && currentAccess != failedAccessToken) {
                AuthTelemetry.logBreadcrumb(
                    "REFRESH_SKIPPED_TOKEN_UPDATED",
                    "Stored token (${AuthTelemetry.maskToken(currentAccess)}) differs from failed token (${AuthTelemetry.maskToken(failedAccessToken)})"
                )
                return@withLock BearerTokens(currentAccess, currentRefresh ?: "")
            }

            // 2. Validate payload integrity: refresh token must not be null or blank
            if (currentRefresh.isNullOrBlank()) {
                AuthTelemetry.logBreadcrumb(
                    "REFRESH_SKIPPED_EMPTY_REFRESH_TOKEN",
                    "Refresh token is null or blank. Cannot perform refresh call."
                )
                return@withLock null
            }

            AuthTelemetry.logBreadcrumb(
                "OUTGOING_REFRESH_REQUEST",
                "Thread ID: ${Thread.currentThread().threadId()}, Refresh Token: ${AuthTelemetry.maskToken(currentRefresh)}"
            )

            val refreshClient = refreshClientFactory()

            try {
                val response = refreshClient.post("https://api-rust.teddy.fyi/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RefreshRequest(
                            userId = session.userId ?: "",
                            clientUuid = session.clientUuid ?: "",
                            refreshToken = currentRefresh,
                            expiresInSecs = ctx?.let { getAuthTimeoutSecs(it) } ?: 3600L,
                        )
                    )
                }

                val status = response.status
                val bodySnippet = try { response.bodyAsText().take(100) } catch (_: Exception) { "" }
                AuthTelemetry.logBreadcrumb(
                    "REFRESH_RESPONSE",
                    "HTTP Status: ${status.value}, Body: $bodySnippet"
                )

                when {
                    status.value in 200..299 -> {
                        val tokens = response.body<TokenResponse>()
                        session.updateTokens(tokens.accessToken, tokens.refreshToken)
                        if (ctx != null) {
                            session.tokenStorage.saveTokens(ctx, tokens.accessToken, tokens.refreshToken)
                        }
                        AuthTelemetry.logBreadcrumb("REFRESH_SUCCESS", "New Access: ${AuthTelemetry.maskToken(tokens.accessToken)}")
                        BearerTokens(tokens.accessToken, tokens.refreshToken)
                    }
                    status == HttpStatusCode.BadRequest || status == HttpStatusCode.Unauthorized -> {
                        val reason = "Refresh token rejected by server (HTTP ${status.value})"
                        AuthTelemetry.logBreadcrumb("LOGOUT_TRIGGERED", reason)
                        if (ctx != null) {
                            session.clear(ctx, reason = reason)
                        } else {
                            session.updateTokens(null, null)
                        }
                        null
                    }
                    else -> {
                        // 5xx or other non-revocation status code - network/server glitch
                        AuthTelemetry.logBreadcrumb(
                            "REFRESH_SERVER_ERROR",
                            "HTTP ${status.value} received from refresh endpoint. Session retained."
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                AuthTelemetry.logBreadcrumb(
                    "REFRESH_NETWORK_ERROR",
                    "Exception during refresh: ${e.javaClass.simpleName} - ${e.message}. Session retained."
                )
                null
            } finally {
                refreshClient.close()
            }
        }
    }
}
