package fyi.teddy.android.grocery.ui.components

/**
 * Where each aisle block sits in the shopping grid, so the sign for the aisle you are
 * standing in can be pinned above the list once its own sign has scrolled away.
 *
 * A [LazyVerticalGrid][androidx.compose.foundation.lazy.grid.LazyVerticalGrid] numbers
 * every emitted item, full-span signs included, so the position of a block is worked out
 * the same way it is emitted: one index for the sign, then one per visible tile.
 *
 * This is deliberately free of Compose types: the arithmetic is the part that can be
 * wrong, and it is cheaper to test it directly than through a scrolling grid.
 */
data class AisleSpan(
    /** The category id the block belongs to; null for "Everything else". */
    val categoryId: String?,
    /** Index of the aisle's sign in the grid. */
    val headerIndex: Int,
    /** How many tiles the grid draws under the sign — zero while the aisle is collapsed. */
    val visibleItemCount: Int,
) {
    /** Grid indices covered by this block, sign included. */
    val lastIndex: Int get() = headerIndex + visibleItemCount
}

/**
 * Lays the aisle blocks out in emission order.
 *
 * [aisles] is each aisle's category id paired with the number of items it holds, in the
 * order they are drawn. [isExpanded] answers whether an aisle's tiles are currently drawn.
 */
fun aisleSpans(
    aisles: List<Pair<String?, Int>>,
    isExpanded: (String?) -> Boolean,
): List<AisleSpan> {
    var index = 0
    return aisles.map { (categoryId, itemCount) ->
        val visible = if (isExpanded(categoryId)) itemCount else 0
        AisleSpan(categoryId, index, visible).also { index += visible + 1 }
    }
}

/**
 * The aisle whose sign should be pinned at the top of the list, or null when none should be.
 *
 * Null while the aisle's own sign is on screen (nothing is missing, so nothing is pinned)
 * and null once the list has scrolled past the last aisle into the cart, which is not an
 * aisle you can walk to.
 */
fun pinnedAisle(spans: List<AisleSpan>, firstVisibleItemIndex: Int): AisleSpan? =
    spans.lastOrNull { firstVisibleItemIndex in (it.headerIndex + 1)..it.lastIndex }
