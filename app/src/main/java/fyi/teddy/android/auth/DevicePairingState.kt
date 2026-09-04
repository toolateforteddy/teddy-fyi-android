package fyi.teddy.android.auth

/**
 * How far pairing sign-in has got, for the screen that is offering it.
 *
 * Pairing is the way in on a device with no Google account of its own -- a Fire tablet has no
 * Play Services, so Credential Manager has no provider to answer it. See
 * [fyi.teddy.android.network.DevicePairingRepository] for the flow itself.
 */
sealed interface DevicePairingState {
    /** Not pairing. Either it has not been asked for, or it finished. */
    data object Idle : DevicePairingState

    /** Asking the API for a code. There is nothing to show yet. */
    data object Starting : DevicePairingState

    /**
     * There is a code on screen and the tablet is waiting for it to be redeemed.
     *
     * @param userCode the characters as the API issued them, unpunctuated.
     * @param verificationUri the page to type them into, shown to be read off.
     */
    data class AwaitingRedemption(
        val userCode: String,
        val verificationUri: String,
    ) : DevicePairingState {
        /** The same code with its one hyphen in, which is the only place that hyphen exists. */
        val displayCode: String get() = DeviceUserCode.format(userCode)
    }

    /** The code ran out before anybody typed it. Asking for another is the way on. */
    data object Expired : DevicePairingState

    /** Pairing could not be run, with something worth showing. */
    data class Failure(val message: String) : DevicePairingState
}

/** How a pairing code is written down to be read out. */
object DeviceUserCode {

    /** How many characters the API issues. */
    private const val LENGTH = 8

    /**
     * Groups [code] into halves around a hyphen -- `H4KP-9TQR`.
     *
     * The hyphen is presentation and nothing else: the API issues and accepts the bare
     * characters, so it is added here and never sent back. Anything that is not the expected
     * length is shown exactly as it arrived rather than chopped somewhere arbitrary.
     */
    fun format(code: String): String {
        val bare = code.filter { it.isLetterOrDigit() }.uppercase()
        if (bare.length != LENGTH) return code
        return bare.substring(0, LENGTH / 2) + "-" + bare.substring(LENGTH / 2)
    }
}
