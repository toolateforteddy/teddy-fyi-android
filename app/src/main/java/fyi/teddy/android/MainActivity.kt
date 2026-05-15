package fyi.teddy.android

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import fyi.teddy.android.grocery.ui.GroceryScreen
import fyi.teddy.android.grocery.ui.StoreManagementScreen
import fyi.teddy.android.todo.ui.TodoScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UserSession {
    var userName by mutableStateOf<String?>(null)
    var idToken by mutableStateOf<String?>(null)
    var profilePictureUri by mutableStateOf<String?>(null)

    fun save(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", userName)
            putString("id_token", idToken)
            putString("profile_pic", profilePictureUri)
            apply()
        }
    }

    fun load(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userName = sharedPref.getString("user_name", null)
        idToken = sharedPref.getString("id_token", null)
        profilePictureUri = sharedPref.getString("profile_pic", null)
    }

    fun clear(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        userName = null
        idToken = null
        profilePictureUri = null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val DarkColorScheme = darkColorScheme(
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFFCCC2DC),
                tertiary = Color(0xFFEFB8C8),
                background = Color.Black,
                surface = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White,
            )

            MaterialTheme(colorScheme = DarkColorScheme) {
                val navController = rememberNavController()
                val session = remember { UserSession() }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    session.load(context)
                    if (session.idToken != null) {
                        navController.navigate("hello") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(onLoginSuccess = { name, token, pic ->
                            session.userName = name
                            session.idToken = token
                            session.profilePictureUri = pic?.toString()
                            session.save(context)
                            Log.d("MainActivity", "Session updated: name=$name")
                            navController.navigate("hello") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("hello") {
                        HelloTeddyApp(
                            userName = session.userName,
                            profilePic = session.profilePictureUri,
                            onNavigateToWeather = { navController.navigate("weather") },
                            onNavigateToAuthed = { navController.navigate("authed") },
                            onNavigateToTodo = { navController.navigate("todo") },
                            onNavigateToGrocery = { navController.navigate("grocery") },
                            onLogout = {
                                session.clear(context)
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
                    composable("todo") {
                        TodoScreen(onBack = { navController.popBackStack() })
                    }
                    composable("grocery") {
                        GroceryScreen(
                            onBack = { navController.popBackStack() },
                            onManageStores = { navController.navigate("stores") }
                        )
                    }
                    composable("stores") {
                        StoreManagementScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String?, String, android.net.Uri?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    val isEmulator = remember {
        Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.contains("sdk_gphone")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
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
                            val cred = handleSignIn(result)
                            if (cred?.second != null) {
                                onLoginSuccess(cred.first, cred.second!!, cred.third)
                            } else {
                                errorMessage = "Google Sign-In succeeded but no ID Token was returned."
                                Log.e("MainActivity", "Login succeeded but token was null")
                            }
                        } catch (e: GetCredentialException) {
                            errorMessage = "Auth Error: ${e.message}"
                            Log.e("MainActivity", "Login Error", e)
                        } finally {
                            isLoggingIn = false
                        }
                    }
                }) {
                    Text("Sign in with Google")
                }

                if (isEmulator) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        onLoginSuccess("Emulator Guest", "fake_emulator_token", null)
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun handleSignIn(result: GetCredentialResponse): Triple<String?, String?, android.net.Uri?>? {
    val credential = result.credential
    
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return Triple(googleIdTokenCredential.displayName, googleIdTokenCredential.idToken, googleIdTokenCredential.profilePictureUri)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("MainActivity", "Received an invalid google id token response", e)
        }
    } else if (credential is GoogleIdTokenCredential) {
        return Triple(credential.displayName, credential.idToken, credential.profilePictureUri)
    }

    return null
}

@Composable
fun HelloTeddyApp(
    userName: String?,
    profilePic: String?,
    onNavigateToWeather: () -> Unit,
    onNavigateToAuthed: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onNavigateToGrocery: () -> Unit,
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
            if (profilePic != null) {
                AsyncImage(
                    model = profilePic,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = "Hello ${userName ?: "Teddy"}",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToWeather, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Check Weather in Arlington, MA")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToAuthed, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Call Authed Endpoint")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToTodo, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Manage Todo List")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToGrocery, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Manage Grocery List")
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
            result = "Error: No ID Token found."
            return@LaunchedEffect
        }
        result = try {
            callAuthedHello(idToken)
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
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
