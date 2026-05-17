package fyi.teddy.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import fyi.teddy.android.auth.GoogleSignInResult
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.grocery.ui.CategoryManagementScreen
import fyi.teddy.android.grocery.ui.GroceryConfigScreen
import fyi.teddy.android.grocery.ui.GroceryScreen
import fyi.teddy.android.grocery.ui.StoreManagementScreen
import fyi.teddy.android.repository.TeddyRepository
import fyi.teddy.android.todo.ui.TodoScreen
import fyi.teddy.android.ui.theme.TeddyTheme
import fyi.teddy.android.utils.EmulatorUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeddyTheme {
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
                        LoginScreen(onLoginSuccess = { result ->
                            session.userName = result.displayName
                            session.idToken = result.idToken
                            session.profilePictureUri = result.profilePictureUri?.toString()
                            session.save(context)
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
                            onManageConfig = { navController.navigate("grocery_config") }
                        )
                    }
                    composable("grocery_config") {
                        GroceryConfigScreen(
                            onBack = { navController.popBackStack() },
                            onManageStores = { navController.navigate("stores") },
                            onManageCategories = { navController.navigate("categories") }
                        )
                    }
                    composable("stores") {
                        StoreManagementScreen(onBack = { navController.popBackStack() })
                    }
                    composable("categories") {
                        CategoryManagementScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (GoogleSignInResult) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    val isEmulator = remember { EmulatorUtils.isEmulator() }

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
                            val signInResult = handleSignIn(result)
                            if (signInResult != null) {
                                onLoginSuccess(signInResult)
                            } else {
                                errorMessage = "Google Sign-In succeeded but no ID Token was returned."
                            }
                        } catch (e: GetCredentialException) {
                            errorMessage = "Auth Error: ${e.message}"
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun handleSignIn(result: GetCredentialResponse): GoogleSignInResult? {
    val credential = result.credential
    
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleSignInResult(
                displayName = googleIdTokenCredential.displayName,
                idToken = googleIdTokenCredential.idToken,
                profilePictureUri = googleIdTokenCredential.profilePictureUri
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("MainActivity", "Received an invalid google id token response", e)
        }
    } else if (credential is GoogleIdTokenCredential) {
        return GoogleSignInResult(
            displayName = credential.displayName,
            idToken = credential.idToken,
            profilePictureUri = credential.profilePictureUri
        )
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
    var isClusterHappy by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isClusterHappy = TeddyRepository.checkClusterHealth()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        contentScale = ContentScale.Crop,
                        onError = {
                            Log.e("MainActivity", "Error loading profile picture: ${it.result.throwable.message}")
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
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
        
        // Health Check Icon in bottom right
        if (isClusterHappy != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isClusterHappy!!) Color.Green else Color.Red)
            ) {
                Icon(
                    imageVector = if (isClusterHappy!!) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = "Health Check",
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    var temperature by remember { mutableStateOf<String?>("Loading...") }

    LaunchedEffect(Unit) {
        temperature = try {
            TeddyRepository.fetchTemperature()
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
            TeddyRepository.callAuthedHello(idToken)
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
