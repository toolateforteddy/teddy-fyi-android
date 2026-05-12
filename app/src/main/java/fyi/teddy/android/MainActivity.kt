package fyi.teddy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloTeddyApp()
        }
    }
}

@Composable
fun HelloTeddyApp() {
    // Surface provides the background color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Text(
            text = "Hello Teddy A second Time.",
            color = Color.White
        )
    }
}
