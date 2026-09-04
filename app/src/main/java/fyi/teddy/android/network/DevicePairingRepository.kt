package fyi.teddy.android.network

import android.util.Log
import fyi.teddy.android.auth.AuthUtils
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The body of `/auth/device/start`. */
@Serializable
data class DeviceStartRequest(
    @SerialName("client_uuid") val clientUuid: String,
    /**
     * Which build asked -- `BuildConfig.PAIRING_APP`. The API keys the redemption page off
     * it, so it is a name that service knows, not a free-form label.
     */
    @SerialName("app") val app: String,
)

/** What `/auth/device/start` hands back: the code to show, and how to wait for it. */
@Serializable
data class DeviceStartResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String = ApiRoutes.DEVICE_LINK_PAGE,
    @SerialName("expires_in") val expiresIn: Long = DevicePairingRepository.DEFAULT_EXPIRES_IN_SECONDS,
    @SerialName("interval") val interval: Long = DevicePairingRepository.DEFAULT_INTERVAL_SECONDS,
)

/** The body of `/auth/device/poll`. The `client_uuid` must be the one `/start` was given. */
@Serializable
data class DevicePollRequest(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("client_uuid") val clientUuid: String,
)

/**
 * Pairing sign-in: signing in on a device that has no Google account of its own.
 *
 * A Fire tablet ships without Google Play Services, so [fyi.teddy.android.auth.LoginScreen]'s
 * Credential Manager call has no identity provider to answer it and never gets an ID token --
 * there is nothing to hand `/auth/login`. Instead the tablet asks the API for a short code,
 * shows it, and polls; somebody signs in with Google at `teddy.fyi/link` on a phone or a laptop
 * and types the code there. What comes back is the same access/refresh pair `/auth/login` mints,
 * so everything downstream of sign-in -- refresh, sync, the widgets -- is unchanged.
 *
 * The Google half of sign-in never happens on the tablet, which is the point: the tablet may be
 * a shared one sitting on a kitchen counter.
 *
 * Codes are secrets. Neither the `user_code` nor the `device_code` is ever logged.
 */
@Suppress("TooGenericExceptionCaught")
object DevicePairingRepository {

    private const val TAG = "DevicePairing"

    /** What the API sends today, used only if a response leaves them out. */
    const val DEFAULT_EXPIRES_IN_SECONDS = 600L
    const val DEFAULT_INTERVAL_SECONDS = 5L

    /**
     * A server that asked for no interval at all would be polled in a tight loop, and one that
     * asked for an hour would be asked once, after the code had already expired.
     */
    private const val MIN_INTERVAL_SECONDS = 1L
    private const val MAX_INTERVAL_SECONDS = 60L

    private const val MILLIS_PER_SECOND = 1000L

    /** Not in Ktor's named statuses as a constant we use elsewhere; spelled out for clarity. */
    private const val HTTP_ACCEPTED = 202
    private const val HTTP_GONE = 410
    private const val HTTP_TOO_MANY_REQUESTS = 429

    private const val START_FAILED = "Could not get a code. Check the connection and try again."
    private const val POLL_FAILED = "Sign-in could not be completed. Ask for a new code."

    /** Where a code has got to. */
    sealed interface PollResult {
        /** Nobody has redeemed it yet. Ask again after the interval. */
        data object Pending : PollResult

        data class Paired(val session: PairedSession) : PollResult

        /** The code ran out, or was already spent. A new one is the only way on. */
        data object Expired : PollResult

        data class Failure(val message: String) : PollResult

        /** Polled faster than the server allows: back off rather than stop. */
        data object TooFast : PollResult
    }

    /** A session minted for a code somebody redeemed. The tail of a Google sign-in. */
    data class PairedSession(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
    )

    /**
     * Asks for a code to put on screen. Returns null when the API could not be reached, which
     * the caller shows as [START_FAILED] -- there is nothing more specific worth saying, and a
     * response body is not a thing to put in front of somebody holding a tablet.
     */
    suspend fun start(clientUuid: String, app: String): DeviceStartResponse? {
        return try {
            // The login client deliberately carries no Auth plugin: there is no session yet,
            // and priming the main client's bearer provider with a null token is the bug
            // AuthRepository.login already avoids.
            val response = NetworkClient.loginClient.post(ApiRoutes.DEVICE_START) {
                contentType(ContentType.Application.Json)
                setBody(DeviceStartRequest(clientUuid = clientUuid, app = app))
            }
            if (response.status.value in 200..299) {
                response.body<DeviceStartResponse>()
            } else {
                // The status is all a log line here may say.
                Log.e(TAG, "Pairing start failed with status: ${response.status.value}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pairing start exception", e)
            null
        }
    }

    /** Asks once whether a code has been redeemed. [awaitPairing] is what callers want. */
    suspend fun poll(deviceCode: String, clientUuid: String): PollResult {
        return try {
            val response = NetworkClient.loginClient.post(ApiRoutes.DEVICE_POLL) {
                contentType(ContentType.Application.Json)
                setBody(DevicePollRequest(deviceCode = deviceCode, clientUuid = clientUuid))
            }
            classify(response)
        } catch (e: Exception) {
            // A request that never landed says nothing about the code, so it is retried rather
            // than reported -- which is exactly what Pending already means here.
            Log.w(TAG, "Pairing poll could not be made; will retry", e)
            PollResult.Pending
        }
    }

    private suspend fun classify(response: HttpResponse): PollResult {
        return when (val status = response.status.value) {
            in 200..201 -> {
                val tokens = response.body<TokenResponse>()
                val userId = AuthUtils.extractUserIdFromToken(tokens.accessToken)
                if (userId.isNullOrBlank()) {
                    // A session we cannot name is one that would sync somebody else's rows
                    // into this device's database. Refuse it rather than guess.
                    Log.e(TAG, "Paired session carried no user id")
                    PollResult.Failure(POLL_FAILED)
                } else {
                    PollResult.Paired(
                        PairedSession(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken,
                            userId = userId,
                        )
                    )
                }
            }

            HTTP_ACCEPTED -> PollResult.Pending
            // Ran out, or was already spent: a code is single-use.
            HTTP_GONE -> PollResult.Expired
            HTTP_TOO_MANY_REQUESTS -> PollResult.TooFast
            else -> {
                Log.e(TAG, "Pairing poll failed with status: $status")
                PollResult.Failure(POLL_FAILED)
            }
        }
    }

    /**
     * Polls [request] until somebody redeems it or it runs out, on the interval the server asked
     * for. Cancelling the coroutine -- which is what leaving the screen does -- stops it.
     *
     * A poll that could not be made is retried rather than reported: walking to another room to
     * fetch a phone takes a tablet through more than one dead spot, and a dropped packet is not
     * a reason to make somebody start again.
     */
    suspend fun awaitPairing(request: DeviceStartResponse, clientUuid: String): PollResult {
        val deadline = System.currentTimeMillis() + request.expiresIn * MILLIS_PER_SECOND
        var wait = request.interval.coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS)
        while (System.currentTimeMillis() < deadline) {
            delay(wait * MILLIS_PER_SECOND)
            when (val result = poll(request.deviceCode, clientUuid)) {
                PollResult.Pending -> Unit
                PollResult.TooFast -> wait = (wait * 2).coerceAtMost(MAX_INTERVAL_SECONDS)
                else -> return result
            }
        }
        return PollResult.Expired
    }

    /** The failure message for a start that did not produce a code. */
    fun startFailureMessage(): String = START_FAILED
}
