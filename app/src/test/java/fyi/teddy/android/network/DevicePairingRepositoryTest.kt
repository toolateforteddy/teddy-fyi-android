package fyi.teddy.android.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pairing sign-in, which is the only way in on a device with no Google account of its own.
 *
 * Robolectric because the user id is read out of the access token with `android.util.Base64`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DevicePairingRepositoryTest {

    /** `{"sub":"paired_user_42"}` as the payload of an otherwise unsigned JWT. */
    private val accessToken = "header.eyJzdWIiOiJwYWlyZWRfdXNlcl80MiJ9.signature"

    private fun clientReturning(
        handler: (HttpRequestData) -> Pair<String, HttpStatusCode>
    ): Pair<HttpClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += String(request.body.toByteArray())
            val (content, status) = handler(request)
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return client to bodies
    }

    private fun useClient(client: HttpClient) {
        NetworkClient.client = client
        NetworkClient.loginClient = client
    }

    @Test
    fun `start names the app so the API can answer with the right page`() = runBlocking {
        val (client, bodies) = clientReturning {
            """{"device_code":"dc","user_code":"H4KP9TQR",
               "verification_uri":"https://teddy.fyi/link","expires_in":600,"interval":5}""" to
                HttpStatusCode.OK
        }
        useClient(client)

        val started = DevicePairingRepository.start("client-uuid-1", "TEDDY_FYI_GROCERY")

        assertEquals("dc", started?.deviceCode)
        assertEquals("H4KP9TQR", started?.userCode)
        assertEquals("https://teddy.fyi/link", started?.verificationUri)
        assertEquals(600L, started?.expiresIn)
        // The API keys the redemption page off this, and the two products redeem on different
        // sites, so it is not a free-form label.
        assertTrue(bodies.single().contains("TEDDY_FYI_GROCERY"))
        assertTrue(bodies.single().contains("client-uuid-1"))
    }

    @Test
    fun `start falls back to the link page when the response leaves it out`() = runBlocking {
        val (client, _) = clientReturning {
            """{"device_code":"dc","user_code":"H4KP9TQR"}""" to HttpStatusCode.OK
        }
        useClient(client)

        val started = DevicePairingRepository.start("client-uuid-1", "TEDDY_FYI")

        assertEquals(ApiRoutes.DEVICE_LINK_PAGE, started?.verificationUri)
        assertEquals(
            DevicePairingRepository.DEFAULT_INTERVAL_SECONDS,
            started?.interval,
        )
    }

    @Test
    fun `start reports nothing rather than a code when the API refuses`() = runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        useClient(HttpClient(engine) { install(ContentNegotiation) { json() } })

        assertNull(DevicePairingRepository.start("client-uuid-1", "TEDDY_FYI"))
    }

    @Test
    fun `an unredeemed code is pending, not a failure`() = runBlocking {
        val (client, _) = clientReturning { """{"status":"pending"}""" to HttpStatusCode.Accepted }
        useClient(client)

        assertEquals(
            DevicePairingRepository.PollResult.Pending,
            DevicePairingRepository.poll("dc", "client-uuid-1"),
        )
    }

    @Test
    fun `a redeemed code yields a session named by the access token`() = runBlocking {
        val (client, bodies) = clientReturning {
            """{"access_token":"$accessToken","refresh_token":"refresh-1"}""" to HttpStatusCode.OK
        }
        useClient(client)

        val result = DevicePairingRepository.poll("dc", "client-uuid-1")

        val paired = result as DevicePairingRepository.PollResult.Paired
        assertEquals(accessToken, paired.session.accessToken)
        assertEquals("refresh-1", paired.session.refreshToken)
        // The poll answers with tokens and nothing else, so the user id has to come out of the
        // access token's `sub` — a session we cannot name would sync the wrong rows.
        assertEquals("paired_user_42", paired.session.userId)
        assertTrue(bodies.single().contains("dc"))
    }

    @Test
    fun `a session with no user id in it is refused rather than guessed at`() = runBlocking {
        val (client, _) = clientReturning {
            """{"access_token":"not-a-jwt","refresh_token":"refresh-1"}""" to HttpStatusCode.OK
        }
        useClient(client)

        assertTrue(
            DevicePairingRepository.poll("dc", "client-uuid-1")
                is DevicePairingRepository.PollResult.Failure
        )
    }

    @Test
    fun `a spent or expired code is expired, and asking again is the only way on`() = runBlocking {
        val (client, _) = clientReturning { "" to HttpStatusCode.Gone }
        useClient(client)

        assertEquals(
            DevicePairingRepository.PollResult.Expired,
            DevicePairingRepository.poll("dc", "client-uuid-1"),
        )
    }

    @Test
    fun `polling too fast is not a failure the screen should show`() = runBlocking {
        val (client, _) = clientReturning { "" to HttpStatusCode.TooManyRequests }
        useClient(client)

        assertEquals(
            DevicePairingRepository.PollResult.TooFast,
            DevicePairingRepository.poll("dc", "client-uuid-1"),
        )
    }

    @Test
    fun `a poll that never landed is retried rather than reported`() = runBlocking {
        val engine = MockEngine { throw java.io.IOException("no network in the hallway") }
        useClient(HttpClient(engine) { install(ContentNegotiation) { json() } })

        // Walking to another room for a phone takes a tablet through dead spots; a dropped
        // packet must not make anybody start again.
        assertEquals(
            DevicePairingRepository.PollResult.Pending,
            DevicePairingRepository.poll("dc", "client-uuid-1"),
        )
    }

    @Test
    fun `awaitPairing keeps polling until the code is redeemed`() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls += 1
            if (calls == 1) {
                respond(
                    content = """{"status":"pending"}""",
                    status = HttpStatusCode.Accepted,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    content = """{"access_token":"$accessToken","refresh_token":"refresh-1"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        useClient(HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        })

        val request = DeviceStartResponse(
            deviceCode = "dc",
            userCode = "H4KP9TQR",
            verificationUri = ApiRoutes.DEVICE_LINK_PAGE,
            expiresIn = 30,
            interval = 1,
        )

        val result = DevicePairingRepository.awaitPairing(request, "client-uuid-1")

        assertTrue(result is DevicePairingRepository.PollResult.Paired)
        assertEquals(2, calls)
    }
}
