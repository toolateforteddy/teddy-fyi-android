package fyi.teddy.android.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.auth.UserSession
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

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var session: UserSession

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = UserSession()
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
        NetworkClient.client = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        NetworkClient.session = session
        
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
        NetworkClient.client = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        NetworkClient.session = session
        
        val success = AuthRepository.login(context, session, "bad_token")
        
        assertFalse(success)
        assertNull(session.accessToken)
    }
}
