package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var userName by remember { mutableStateOf<String?>(null) }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(onLoginSuccess = { name ->
                        userName = name
                        navController.navigate("hello") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
                }
                composable("hello") {
                    HelloTeddyApp(
                        userName = userName,
                        onNavigateToWeather = { navController.navigate("weather") },
                        onLogout = {
                            userName = null
                            navController.navigate("login") {
                                popUpTo("hello") { inclusive = true }
                            }
                        }
                    )
                }
                composable("weather") {
                    WeatherScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Button(onClick = {
                scope.launch {
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
                        val name = handleSignIn(result)
                        onLoginSuccess(name)
                    } catch (e: GetCredentialException) {
                        errorMessage = e.message
                        Log.e("MainActivity", "Login Error", e)
                    }
                }
            }) {
                Text("Sign in with Google")
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Error: $errorMessage", color = Color.Red, fontSize = 14.sp)
            }
        }
    }
}

private fun handleSignIn(result: GetCredentialResponse): String? {
    val credential = result.credential
    return if (credential is GoogleIdTokenCredential) {
        Log.d("MainActivity", "Got ID token and user: ${credential.displayName}")
        credential.displayName
    } else {
        Log.e("MainActivity", "Unexpected credential type")
        null
    }
}

@Composable
fun HelloTeddyApp(userName: String?, onNavigateToWeather: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                text = "Hello ${userName ?: "Teddy"}",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onNavigateToWeather) {
                Text("Check Weather in Arlington, MA")
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
