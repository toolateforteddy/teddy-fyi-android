package fyi.teddy.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.R
import fyi.teddy.android.repository.TeddyRepository
import fyi.teddy.android.ui.components.TeddyButton
import fyi.teddy.android.ui.theme.TeddyTheme

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
        color = TeddyTheme.colors.screenBottom,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Arlington, MA",
                color = TeddyTheme.colors.onSurfaceMuted,
                fontSize = 18.sp
            )
            Text(
                text = temperature ?: "Unknown",
                color = TeddyTheme.colors.onSurface,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            TeddyButton(text = stringResource(R.string.back), onClick = onBack)
        }
    }
}
