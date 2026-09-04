package fyi.teddy.android.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The hyphen in a pairing code exists only on this screen: the API issues and accepts the bare
 * characters. Getting that wrong sends a hyphen back to the API, or shows a code somebody
 * cannot read off a propped-up tablet.
 */
class DeviceUserCodeTest {

    @Test
    fun `splits an eight character code around one hyphen`() {
        assertEquals("H4KP-9TQR", DeviceUserCode.format("H4KP9TQR"))
    }

    @Test
    fun `leaves anything that is not code-shaped exactly as it arrived`() {
        // Chopping an unexpected length somewhere arbitrary would show a code that is not the
        // one the API issued, which is worse than showing an odd-looking one.
        assertEquals("H4KP9", DeviceUserCode.format("H4KP9"))
        assertEquals("", DeviceUserCode.format(""))
    }

    @Test
    fun `formats a code that already carries its hyphen without doubling it`() {
        assertEquals("H4KP-9TQR", DeviceUserCode.format("H4KP-9TQR"))
    }

    @Test
    fun `uppercases what it shows`() {
        assertEquals("H4KP-9TQR", DeviceUserCode.format("h4kp9tqr"))
    }

    @Test
    fun `awaiting state exposes the display form of its code`() {
        val state = DevicePairingState.AwaitingRedemption(
            userCode = "CDFHJKMN",
            verificationUri = "https://teddy.fyi/link",
        )
        assertEquals("CDFH-JKMN", state.displayCode)
        assertEquals("CDFHJKMN", state.userCode)
    }
}
