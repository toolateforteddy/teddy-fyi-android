package fyi.teddy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "hello") {
                composable("hello") {
                    HelloTeddyApp(onNavigateToWeather = { navController.navigate("weather") })
                }
                composable("weather") {
                    WeatherScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun HelloTeddyApp(onNavigateToWeather: () -> Unit) {
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
                text = "Hello Teddy",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onNavigateToWeather) {
                Text("Check Weather in Arlington, MA")
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
        // Open-Meteo API for Arlington, MA (42.4154, -71.1565)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=42.4154&longitude=-71.1565&current_weather=true&temperature_unit=fahrenheit"
        val response = URL(url).readText()
        val json = JSONObject(response)
        val currentWeather = json.getJSONObject("current_weather")
        val temp = currentWeather.getDouble("temperature")
        "$temp°F"
    }
}
