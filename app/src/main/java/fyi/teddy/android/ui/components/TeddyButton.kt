package fyi.teddy.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TeddyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.8f
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(widthFraction)
    ) {
        Text(text)
    }
}
