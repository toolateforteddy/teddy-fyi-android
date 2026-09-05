package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/**
 * Control buttons are smaller than a full [androidx.compose.material3.IconButton]: they share
 * a tile row with the item name. This is the comfortable-density size, and the default for
 * [inlineControlsFit], which is not composable; composables take the size from
 * [GroceryTheme.metrics] so the buttons follow the display-density preference.
 */
private val CompactButtonSize: Dp = 32.dp

/** Width reserved for the quantity between the − and + buttons. */
private val QuantityLabelWidth: Dp = 24.dp

/** Below this the item name is truncated past the point of being useful, so stack instead. */
private val MinInlineNameWidth: Dp = 96.dp

/** Horizontal padding a tile spends on its own chrome before any content is laid out. */
private val TileChromeWidth: Dp = 24.dp

/**
 * Whether a tile [tileWidth] wide can reveal [ItemQuantityControls] beside the item name and still
 * leave the name a readable slice. Tiles that cannot should stack the controls under the name —
 * either way the name stays on screen.
 */
fun inlineControlsFit(tileWidth: Dp, withDelete: Boolean, buttonSize: Dp = CompactButtonSize): Boolean {
    val buttons = if (withDelete) 4 else 3
    return tileWidth >= buttonSize * buttons + QuantityLabelWidth + MinInlineNameWidth + TileChromeWidth
}

/**
 * Quantity stepper plus the per-item actions, sized to sit alongside an item name rather than
 * replace it. [onDelete] is optional: the need tile offers delete here, the planning tile does not.
 */
@Composable
fun ItemQuantityControls(
    quantity: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onEditCategory: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    quantityColor: Color = GroceryTheme.colors.onSurface,
    buttonSize: Dp = GroceryTheme.metrics.controlSize,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        ControlButton(
            icon = Icons.Default.Remove,
            contentDescription = "Decrease quantity",
            onClick = onDecrement,
            tint = GroceryTheme.colors.onSurface,
            size = buttonSize
        )
        val shownQuantity = quantity.ifBlank { "1" }
        Text(
            text = shownQuantity,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = GroceryTheme.metrics.itemFontSize),
            fontWeight = FontWeight.Bold,
            color = quantityColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = QuantityLabelWidth)
                .semantics { contentDescription = "Quantity $shownQuantity" }
        )
        ControlButton(
            icon = Icons.Default.Add,
            contentDescription = "Increase quantity",
            onClick = onIncrement,
            tint = GroceryTheme.colors.onSurface,
            size = buttonSize
        )
        ControlButton(
            icon = Icons.Default.Category,
            contentDescription = "Change category",
            onClick = onEditCategory,
            tint = GroceryTheme.colors.onSurfaceMuted,
            size = buttonSize
        )
        onDelete?.let {
            ControlButton(
                icon = Icons.Default.Delete,
                contentDescription = "Delete item",
                onClick = it,
                tint = GroceryTheme.colors.danger,
                size = buttonSize
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    size: Dp,
) {
    val iconSize = GroceryTheme.metrics.glyphSize
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
