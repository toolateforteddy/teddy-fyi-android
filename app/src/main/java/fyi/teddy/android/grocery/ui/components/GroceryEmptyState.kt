package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.ui.components.StylizedShoppingCart

/**
 * What a Grocery screen shows when it has nothing to show.
 *
 * Reuses the hand-drawn cart from the home-screen bronze token, faded back so it reads
 * as a watermark: the one piece of illustration the app already owns, finally visible
 * inside the app rather than only on the tile that launches it.
 */
@Composable
fun GroceryEmptyState(
    headline: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val colors = GroceryTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StylizedShoppingCart(
            modifier = Modifier
                .size(120.dp)
                .alpha(0.22f),
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = colors.accentBright,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
