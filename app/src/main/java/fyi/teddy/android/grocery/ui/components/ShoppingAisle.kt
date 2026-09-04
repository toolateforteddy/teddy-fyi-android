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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/** One aisle block of the shopping list: its sign, and the items filed under it. */
data class ShoppingAisle(
    /** The category the block belongs to; null for "Everything else". */
    val categoryId: String?,
    val name: String,
    val icon: ImageVector,
    val items: List<GroceryItem>,
)

/**
 * The jump rail: a thin column of aisle glyphs down the side of the shopping grid, one per
 * aisle, that scrolls straight to that aisle's sign.
 *
 * A sixty-item trip is several screens of scrolling on a tablet even at six columns, and
 * the pinned sign only tells you where you are, not how to get somewhere else. The rail is
 * glyphs and counts only -- no names -- because it has to stay narrow enough to cost the
 * grid nothing, and the sign it lands on says the name anyway.
 *
 * [spans] is the same [aisleSpans] the pinned sign uses, in the same order as [aisles], so
 * the two can never disagree about where an aisle starts.
 */
@Composable
fun AisleJumpRail(
    aisles: List<ShoppingAisle>,
    spans: List<AisleSpan>,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = GroceryTheme.metrics
    val stops = aisles.zip(spans)

    LazyColumn(
        modifier = modifier
            .width(metrics.railWidth)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.gutter),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(stops, key = { (aisle, _) -> aisle.categoryId ?: UncategorizedRailKey }) { (aisle, span) ->
            AisleRailStop(aisle = aisle, onClick = { onJumpTo(span.headerIndex) })
        }
    }
}

/** List key for the "Everything else" stop, which has no category id of its own. */
private const val UncategorizedRailKey = "__uncategorized__"

/** One glyph in the rail: the aisle's mark over the number of items still to grab. */
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
            tint = aisleTint(aisle.categoryId),
            modifier = Modifier.size(metrics.glyphSize),
        )
        Text(
            text = if (remaining > 0) remaining.toString() else "✓",
            style = MaterialTheme.typography.labelSmall,
            color = if (remaining > 0) colors.onSurfaceMuted else colors.success,
        )
    }
}
