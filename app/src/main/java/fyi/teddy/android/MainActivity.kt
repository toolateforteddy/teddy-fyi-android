package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UserSession {
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val session = remember { UserSession() }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(onLoginSuccess = { name, token ->
                        session.userName = name
                        session.idToken = token
                        Log.d("MainActivity", "Session updated: name=$name, tokenPresent=${token != null}")
                        navController.navigate("hello") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
                }
                composable("hello") {
                    HelloTeddyApp(
                        userName = session.userName,
                        onNavigateToWeather = { navController.navigate("weather") },
                        onNavigateToAuthed = { navController.navigate("authed") },
                        onLogout = {
                            session.userName = null
                            session.idToken = null
                            navController.navigate("login") {
                                popUpTo("hello") { inclusive = true }
                            }
                        }
                    )
                }
                composable("weather") {
                    WeatherScreen(onBack = { navController.popBackStack() })
                }
                composable("authed") {
                    AuthedHelloScreen(idToken = session.idToken, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String?, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
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
                Button(onClick = {
                    scope.launch {
                        isLoggingIn = true
                        errorMessage = null
                        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId("34718544535-rem2k0n6tue6qmevqgp9c84gmh24a6mp.apps.googleusercontent.com")
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
                            val (name, token) = handleSignIn(result)
                            if (token != null) {
                                onLoginSuccess(name, token)
                            } else {
                                errorMessage = "Google Sign-In succeeded but no ID Token was returned. This might happen if the Client ID is misconfigured."
                                Log.e("MainActivity", "Login succeeded but token was null")
                            }
                        } catch (e: GetCredentialException) {
                            errorMessage = "Auth Error: ${e.message} (${e.javaClass.simpleName})"
                            Log.e("MainActivity", "Login Error", e)
                        } finally {
                            isLoggingIn = false
                        }
                    }
                }) {
                    Text("Sign in with Google")
                }
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun handleSignIn(result: GetCredentialResponse): Pair<String?, String?> {
    val credential = result.credential
    Log.d("MainActivity", "Received credential type: ${credential.type}")

    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Log.d("MainActivity", "Got ID token and user: ${googleIdTokenCredential.displayName}")
            return Pair(googleIdTokenCredential.displayName, googleIdTokenCredential.idToken)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("MainActivity", "Received an invalid google id token response", e)
        }
    } else if (credential is GoogleIdTokenCredential) {
        // Some versions of the library might handle the unwrapping automatically
        Log.d("MainActivity", "Got ID token and user: ${credential.displayName}")
        return Pair(credential.displayName, credential.idToken)
    }

    Log.e("MainActivity", "Unexpected credential type: ${credential.type}")
    return Pair(null, null)
}

@Composable
fun HelloTeddyApp(
    userName: String?,
    onNavigateToWeather: () -> Unit,
    onNavigateToAuthed: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello ${userName ?: "Teddy"}",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onNavigateToWeather) {
                Text("Check Weather in Arlington, MA")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onNavigateToAuthed) {
                Text("Call Authed Endpoint")
            }
            Spacer(modifier = Modifier.height(40.dp))
            TextButton(onClick = {
                scope.launch {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    onLogout()
                }
            }) {
                Text("Logout", color = Color.Gray)
            }
        }
    }
}

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    var temperature by remember { mutableStateOf<String?>("Loading...") }

    LaunchedEffect(Unit) {
        temperature = try {
            fetchTemperature()
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Arlington, MA",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Text(
                text = temperature ?: "Unknown",
                color = Color.White,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
fun AuthedHelloScreen(idToken: String?, onBack: () -> Unit) {
    var result by remember { mutableStateOf("Calling API...") }

    LaunchedEffect(idToken) {
        if (idToken == null) {
            result = "Error: No ID Token found.\n" +
                    "Debug Info:\n" +
                    "- session.idToken was null when LaunchedEffect started.\n" +
                    "- Current time: ${System.currentTimeMillis()}"
            return@LaunchedEffect
        }
        result = try {
            callAuthedHello(idToken)
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}\n\nToken was present (length: ${idToken.length})"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Authed API Response",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = result,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

private suspend fun fetchTemperature(): String {
    return withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=42.4154&longitude=-71.1565&current_weather=true&temperature_unit=fahrenheit"
        val response = URL(url).readText()
        val json = JSONObject(response)
        val currentWeather = json.getJSONObject("current_weather")
        val temp = currentWeather.getDouble("temperature")
        "$temp°F"
    }
}

private suspend fun callAuthedHello(idToken: String): String {
    return withContext(Dispatchers.IO) {
        val url = URL("https://api-rust.teddy.fyi/authed/hello")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $idToken")
        
        val statusCode = connection.responseCode
        val headers = connection.headerFields.entries.joinToString("\n") { (key, values) ->
            "${key ?: "Status"}: ${values.joinToString(", ")}"
        }
        val body = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: e.localizedMessage
        }
        
        "Status: $statusCode\n\nHeaders:\n$headers\n\nBody:\n$body"
    }
}
