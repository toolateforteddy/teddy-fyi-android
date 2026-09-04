package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/**
 * One block of the shopping grid: a sign and the items filed under it.
 *
 * The grid is built from a list of these rather than from categories directly, so the
 * order on screen and the order in the jump rail cannot drift apart.
 */
data class ShoppingAisle(
    /** Identity for expand/collapse state. A category id, or one of the sentinels below. */
    val key: String,
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val items: List<GroceryItem>,
    /** Whether this aisle's items are currently shown. */
    val isExpanded: Boolean = true,
    /** The sign can be tapped to fold the aisle away. False for the cart, which is a tally. */
    val isCollapsible: Boolean = true,
    /** The cart at the foot of the list: tapping an item there puts it back on the shelf. */
    val isCart: Boolean = false,
) {
    companion object {
        /** Aisle for items with no category, or a category that has since been deleted. */
        const val UNCATEGORIZED_KEY = "__uncategorized__"

        /** The "in the cart" block that closes the list. */
        const val CART_KEY = "__cart__"
    }
}

/**
 * Where each aisle's sign lands in the grid, counted in lazy-grid items.
 *
 * A full-width sign is one item, and each tile below it is another, so a collapsed aisle
 * takes exactly one. This is what the jump rail scrolls to, and it is arithmetic rather
 * than measurement so it stays right at any column count.
 */
fun aisleHeaderIndexes(aisles: List<ShoppingAisle>): List<Int> {
    var next = 0
    return aisles.map { aisle ->
        val headerIndex = next
        next += 1 + if (aisle.isExpanded) aisle.items.size else 0
        headerIndex
    }
}

/**
 * The jump rail: a thin column of aisle glyphs down the side of the shopping grid, one per
 * aisle, that scrolls straight to that aisle's sign.
 *
 * A sixty-item trip is several screens of scrolling on a tablet even at six columns. The
 * rail is glyphs and counts only -- no names -- because it has to stay narrow enough that
 * it costs the grid nothing, and the sign it lands on says the name anyway.
 */
@Composable
fun AisleJumpRail(
    aisles: List<ShoppingAisle>,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = GroceryTheme.metrics
    val stops = aisles.zip(aisleHeaderIndexes(aisles))

    LazyColumn(
        modifier = modifier
            .width(metrics.railWidth)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.gutter),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(stops, key = { (aisle, _) -> aisle.key }) { (aisle, headerIndex) ->
            AisleRailStop(aisle = aisle, onClick = { onJumpTo(headerIndex) })
        }
    }
}

/** One glyph in the rail: the aisle's mark over the number of items still in it. */
@Composable
private fun AisleRailStop(
    aisle: ShoppingAisle,
    onClick: () -> Unit,
) {
    val colors = GroceryTheme.colors
    val metrics = GroceryTheme.metrics
    val remaining = aisle.items.count { !it.isBought }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Jump to ${aisle.name}" }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = aisle.icon,
            contentDescription = null,
            tint = aisle.tint,
            modifier = Modifier.size(metrics.glyphSize),
        )
        Text(
            text = if (remaining > 0) remaining.toString() else "✓",
            style = MaterialTheme.typography.labelSmall,
            color = if (remaining > 0) colors.onSurfaceMuted else colors.success,
        )
    }
}
