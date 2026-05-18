package fyi.teddy.android.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
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
import coil.compose.AsyncImage
import fyi.teddy.android.repository.TeddyRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    userName: String?,
    profilePic: String?,
    onNavigateToWeather: () -> Unit,
    onNavigateToAuthed: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onLogout: () -> Unit
) {
    Log.d("HomeScreen", "Rendering HomeScreen. userName=$userName, profilePic=$profilePic")
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
                            Log.e("HomeScreen", "Error loading profile picture: ${it.result.throwable.message}")
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userName ?: "T").take(1).uppercase(),
                            color = Color.White,
                            fontSize = 32.sp
                        )
                    }
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
                Button(onClick = { onNavigateToTodo() }, modifier = Modifier.fillMaxWidth(0.8f)) {
                    Text("Manage Todo List")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onNavigateToGrocery() }, modifier = Modifier.fillMaxWidth(0.8f)) {
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
