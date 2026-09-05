package fyi.teddy.android.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import fyi.teddy.android.BuildConfig
import fyi.teddy.android.R
import fyi.teddy.android.network.DevicePairingRepository
import fyi.teddy.android.network.NetworkClient
import fyi.teddy.android.ui.theme.TeddyTheme
import fyi.teddy.android.utils.EmulatorUtils
import fyi.teddy.android.utils.GmsUtils
import kotlinx.coroutines.launch
import android.net.Uri
import java.util.UUID

/**
 * The way in, by whichever of the two routes this device can actually run.
 *
 * With Play Services present that is Google sign-in through Credential Manager, unchanged.
 * Without it — a Fire tablet has none — Credential Manager has no identity provider to answer
 * it and throws rather than showing an account chooser, so the screen offers pairing instead:
 * a code shown here and redeemed at teddy.fyi/link on a device that does have a Google
 * account. Asked before the attempt rather than after the failure, so nobody is told there is
 * no account on a tablet that was never going to have one.
 *
 * @param onLoginSuccess a Google ID token to exchange at `/auth/login`.
 * @param onPaired a session the API already minted, so there is nothing left to exchange.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (GoogleSignInResult) -> Unit,
    onPaired: (DevicePairingRepository.PairedSession) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(value = false) }

    val isEmulator = remember { EmulatorUtils.isEmulator() }
    val isGmsAvailable = remember { GmsUtils.isGmsAvailable(context) }

    var pairingState by remember { mutableStateOf<DevicePairingState>(DevicePairingState.Idle) }
    // Pairing asked for on a device that could have used Google. Debug builds only, and off
    // until somebody taps for it: see [showPairing].
    var codeRequested by remember { mutableStateOf(value = false) }
    // Bumped to ask for a code; zeroed to stop. Keying the effect on it means leaving the
    // screen, or cancelling, cancels the poll loop with it.
    var pairingAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(pairingAttempt) {
        if (pairingAttempt == 0) return@LaunchedEffect
        pairingState = DevicePairingState.Starting

        val session = NetworkClient.session
        // The same client_uuid has to be on /start and /poll, and it is this install's
        // identity everywhere else too, so it is settled here rather than invented twice.
        val clientUuid = session.clientUuid ?: UUID.randomUUID().toString().also {
            session.clientUuid = it
        }

        val request = DevicePairingRepository.start(clientUuid, BuildConfig.PAIRING_APP)
        if (request == null) {
            pairingState = DevicePairingState.Failure(DevicePairingRepository.startFailureMessage())
            return@LaunchedEffect
        }

        pairingState = DevicePairingState.AwaitingRedemption(
            userCode = request.userCode,
            verificationUri = request.verificationUri,
        )

        when (val result = DevicePairingRepository.awaitPairing(request, clientUuid)) {
            is DevicePairingRepository.PollResult.Paired -> {
                pairingState = DevicePairingState.Idle
                onPaired(result.session)
            }
            is DevicePairingRepository.PollResult.Failure ->
                pairingState = DevicePairingState.Failure(result.message)
            // awaitPairing resolves to one of the three above or gives up; a code it is still
            // waiting on when the deadline passes is an expired code.
            else -> pairingState = DevicePairingState.Expired
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TeddyTheme.colors.screenBottom,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The label, not a literal: the grocery build is a different app on the home
            // screen and should not welcome anybody to a name it does not carry.
            Text(
                text = "Welcome to ${stringResource(R.string.app_name)}",
                color = TeddyTheme.colors.onSurface,
                fontSize = 24.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoggingIn) {
                CircularProgressIndicator(color = TeddyTheme.colors.accent)
            } else {
                // Pairing is the only way in with no Play Services. With Play Services it is
                // still the quicker way to test the pairing path -- no Fire tablet needed, and
                // no Android OAuth client for this build's package name -- so a debug build can
                // ask for it. A release build offers exactly one way in per device.
                val showPairing = !isGmsAvailable || codeRequested

                if (!showPairing) {
                    Button(onClick = {
                        scope.launch {
                            isLoggingIn = true
                            errorMessage = null
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(
                                    "34718544535-rem2k0n6tue6qmevqgp9c84gmh24a6mp.apps.googleusercontent.com"
                                )
                                .setNonce(java.util.UUID.randomUUID().toString())
                                .setAutoSelectEnabled(true)
                                .build()

                            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            
                            try {
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context,
                                )
                                val signInResult = handleSignIn(result)
                                if (signInResult != null) {
                                    onLoginSuccess(signInResult)
                                } else {
                                    errorMessage = "Google Sign-In succeeded but no ID Token was returned."
                                }
                            } catch (e: GetCredentialException) {
                                errorMessage = "Auth Error: ${e.message}"
                                Log.e("LoginScreen", "Sign-in error: ${e.message} (code: ${e.type})")
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    }) {
                        Text("Sign in with Google")
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = {
                            // Straight to a code: somebody who taps this has already decided.
                            codeRequested = true
                            pairingAttempt += 1
                        }) {
                            Text(
                                "Sign in with a code (debug)",
                                color = TeddyTheme.colors.onSurfaceMuted,
                            )
                        }
                    }
                } else {
                    DevicePairingSection(
                        state = pairingState,
                        onStart = { pairingAttempt += 1 },
                        onCancel = {
                            pairingAttempt = 0
                            pairingState = DevicePairingState.Idle
                            // On a device with Google sign-in, cancelling goes back to it.
                            codeRequested = false
                        },
                    )

                    // The browser flow that predates pairing, kept as a second way in — it
                    // needs a browser that can hand the redirect back to this app, which is
                    // exactly what a Fire tablet may not have, so it is no longer the offer.
                    // Out of the way entirely once there is a code on screen: a second way in
                    // is a distraction while somebody is typing the first one into a phone.
                    if (pairingState == DevicePairingState.Idle) {
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = {
                            val clientId = "34718544535-a8csa0c9ihbe5543dcl21h4ruvilpjav.apps.googleusercontent.com"
                            val redirectUri = "com.googleusercontent.apps.34718544535-a8csa0c9ihbe5543dcl21h4ruvilpjav:/oauth2redirect"
                            val nonce = java.util.UUID.randomUUID().toString()
                            val scopeParam = "openid profile email"
                        
                            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                                .buildUpon()
                                .appendQueryParameter("client_id", clientId)
                                .appendQueryParameter("redirect_uri", redirectUri)
                                .appendQueryParameter("response_type", "id_token")
                                .appendQueryParameter("scope", scopeParam)
                                .appendQueryParameter("nonce", nonce)
                                .build()

                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, authUrl)
                        }) {
                            Text(
                                "Sign in through a browser instead",
                                color = TeddyTheme.colors.onSurfaceMuted,
                            )
                        }
                    }
                }

                if (isEmulator) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        onLoginSuccess(GoogleSignInResult("Emulator Guest", "fake_emulator_token", null))
                    }) {
                        Text("Skip Auth (Emulator only)", color = TeddyTheme.colors.onSurfaceMuted)
                    }
                }
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = errorMessage!!,
                    color = TeddyTheme.colors.danger,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun handleSignIn(result: GetCredentialResponse): GoogleSignInResult? {
    val credential = result.credential
    
    if ((credential is CustomCredential) && (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            var pic = googleIdTokenCredential.profilePictureUri
            if (pic == null) {
                pic = AuthUtils.extractPictureFromToken(googleIdTokenCredential.idToken)
            }
            Log.d("LoginScreen", "Sign-in success: ${googleIdTokenCredential.displayName}, pic=$pic")
            return GoogleSignInResult(
                displayName = googleIdTokenCredential.displayName,
                idToken = googleIdTokenCredential.idToken,
                profilePictureUri = pic
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("LoginScreen", "Received an invalid google id token response", e)
        }
    } else if (credential is GoogleIdTokenCredential) {
        var pic = credential.profilePictureUri
        if (pic == null) {
            pic = AuthUtils.extractPictureFromToken(credential.idToken)
        }
        Log.d("LoginScreen", "Sign-in success (Native): ${credential.displayName}, pic=$pic")
        return GoogleSignInResult(
            displayName = credential.displayName,
            idToken = credential.idToken,
            profilePictureUri = pic
        )
    }
    Log.w("LoginScreen", "Sign-in returned unknown credential type: ${credential.type}")
    return null
}
