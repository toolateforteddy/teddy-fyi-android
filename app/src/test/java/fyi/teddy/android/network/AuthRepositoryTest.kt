package fyi.teddy.android.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.auth.UserSession
import io.mockk.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var session: UserSession

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = UserSession()
        mockkObject(NetworkClient)
        every { NetworkClient.getAuthTimeoutSecs(any()) } returns 3600L
    }

    @Test
    fun testLoginSuccess() = runBlocking {
        // Mocking the network client
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"access_token":"new_access", "refresh_token":"new_refresh"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        NetworkClient.client = mockClient
        NetworkClient.loginClient = mockClient
        NetworkClient.session = session
        session.userId = "test_user_id"
        
        val success = AuthRepository.login(context, session, "google_token_123")
        
        assertTrue(success)
        assertEquals("new_access", session.accessToken)
        assertEquals("new_refresh", session.refreshToken)
        assertNotNull(session.clientUuid)
    }

    @Test
    fun testLoginFailure() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond("", status = HttpStatusCode.Unauthorized)
        }
        val mockClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        NetworkClient.client = mockClient
        NetworkClient.loginClient = mockClient
        NetworkClient.session = session
        
        val success = AuthRepository.login(context, session, "bad_token")
        
        assertFalse(success)
        assertNull(session.accessToken)
    }
}
