package fyi.teddy.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.repository.TeddyRepository

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
            Box(modifier = Modifier.weight(1f)) {
                val lines = result.split("\n")
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lines) { line ->
                        // Further chunk long lines to avoid Constraint errors
                        val chunks = if (line.length > 2000) line.chunked(2000) else listOf(line)
                        chunks.forEach { chunk ->
                            Text(
                                text = chunk,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
