package fyi.teddy.android.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.auth.AuthTelemetry
import fyi.teddy.android.auth.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuardedRefreshTest {

    private lateinit var context: Context
    private lateinit var session: UserSession

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = UserSession()
        NetworkClient.initialize(context)
        NetworkClient.session = session
        AuthTelemetry.clear()
    }

    @Test
    fun testStaleTokenCheckSkipsRefreshCall() = runBlocking {
        session.updateTokens("access_v2", "refresh_v1")

        // Passing "access_v1" as the failed token, while session has "access_v2"
        val result = NetworkClient.performRefreshToken("access_v1")

        assertNotNull(result)
        assertEquals("access_v2", result?.accessToken)
        assertTrue(AuthTelemetry.getBreadcrumbs().any { it.contains("REFRESH_SKIPPED_TOKEN_UPDATED") })
    }

    @Test
    fun testEmptyRefreshTokenSkipsRefreshCall() = runBlocking {
        session.updateTokens("access_v1", "")

        val result = NetworkClient.performRefreshToken("access_v1")

        assertNull(result)
        assertTrue(AuthTelemetry.getBreadcrumbs().any { it.contains("REFRESH_SKIPPED_EMPTY_REFRESH_TOKEN") })
    }

    @Test
    fun testNetworkErrorRetainsSession() = runBlocking {
        session.updateTokens("access_old", "refresh_valid")

        // Mock 500 Server Error
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }
        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        NetworkClient.client = mockClient
        NetworkClient.refreshClientFactory = { mockClient }

        val result = NetworkClient.performRefreshToken("access_old")

        assertNull(result)
        // Ensure session was NOT cleared!
        assertEquals("access_old", session.accessToken)
        assertEquals("refresh_valid", session.refreshToken)
        assertTrue(AuthTelemetry.getBreadcrumbs().any { it.contains("REFRESH_SERVER_ERROR") })
    }

    @Test
    fun testAuthRevocationClearsSession() = runBlocking {
        session.updateTokens("access_old", "refresh_revoked_123")

        // Mock 401 Unauthorized from /refresh endpoint
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"invalid_grant"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        NetworkClient.client = mockClient
        NetworkClient.refreshClientFactory = { mockClient }

        val result = NetworkClient.performRefreshToken("access_old")

        assertNull(result)
        // Ensure session WAS cleared on explicit 401 auth revocation!
        assertNull(session.accessToken)
        assertNull(session.refreshToken)
        assertTrue(AuthTelemetry.getBreadcrumbs().any { it.contains("LOGOUT_TRIGGERED") })
    }

    @Test
    fun testConcurrentRefreshRequestsExecutedOnce(): Unit = runBlocking {
        session.updateTokens("access_old", "refresh_valid_12345")

        var refreshCallCount = 0
        val mockEngine = MockEngine { _ ->
            refreshCallCount++
            respond(
                content = """{"access_token":"access_new_999", "refresh_token":"refresh_new_999"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        NetworkClient.client = mockClient
        NetworkClient.refreshClientFactory = { mockClient }

        // Fire 5 concurrent refresh attempts with the same old access token
        val deferreds = (1..5).map {
            async(Dispatchers.IO) {
                NetworkClient.performRefreshToken("access_old")
            }
        }
        val results = deferreds.awaitAll()

        assertEquals(1, refreshCallCount)
        results.forEach { tokens ->
            assertNotNull(tokens)
            assertEquals("access_new_999", tokens?.accessToken)
        }
    }
}
