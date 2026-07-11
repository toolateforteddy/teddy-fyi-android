package fyi.teddy.android.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import fyi.teddy.android.utils.EmulatorUtils
import fyi.teddy.android.utils.GmsUtils
import kotlinx.coroutines.launch
import android.net.Uri

@Composable
fun LoginScreen(onLoginSuccess: (GoogleSignInResult) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(value = false) }

    val isEmulator = remember { EmulatorUtils.isEmulator() }
    val isGmsAvailable = remember { GmsUtils.isGmsAvailable(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome to Teddy FYI", color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoggingIn) {
                CircularProgressIndicator(color = Color.White)
            } else {
                if (isGmsAvailable) {
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
                } else {
                    Button(onClick = {
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
                        Text("Sign in with Google (Web Fallback)")
                    }
                }

                if (isEmulator) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        onLoginSuccess(GoogleSignInResult("Emulator Guest", "fake_emulator_token", null))
                    }) {
                        Text("Skip Auth (Emulator only)", color = Color.Gray)
                    }
                }
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
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
