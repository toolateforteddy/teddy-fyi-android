package fyi.teddy.android.grocery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizes and text styles that depend on how wide the window is.
 *
 * Grocery runs on a phone held at arm's length and on a Fire tablet propped on a kitchen
 * counter, which is roughly twice as far from the reader and on a much coarser screen. The
 * tablet therefore gets taller rows and larger names, rather than the same phone-sized row
 * stretched across more width.
 *
 * Screens read these through [GroceryTheme.metrics]; they never hardcode a row height or
 * pick a typography slot for an item name by hand.
 */
@Immutable
data class GroceryMetrics(
    /**
     * True once the window is at least [ExpandedWidth]. Layouts that restructure rather
     * than resize — the phase switcher becoming a NavigationRail — branch on this, so the
     * app has one breakpoint rather than one per screen.
     */
    val isExpandedWidth: Boolean,
    /** Height of an item tile in the need, planning and shopping lists. */
    val itemTileHeight: Dp,
    /** Touch target of the inline +/-/category controls inside an item tile. */
    val itemTileControlSize: Dp,
    /** Icon drawn inside an [itemTileControlSize] control. */
    val itemTileControlIconSize: Dp,
    /** The item's name — the one thing that has to be readable across a kitchen. */
    val itemName: TextStyle,
    /** Quantity and unit hints sitting beside an [itemName]. */
    val itemMeta: TextStyle,
)

/** Window width at or above which the tablet metrics take over. */
val ExpandedWidth: Dp = 600.dp

/** The part of [GroceryMetrics] that does not depend on the Material type scale. */
@Immutable
internal data class GroceryTileDimens(
    val itemTileHeight: Dp,
    val itemTileControlSize: Dp,
    val itemTileControlIconSize: Dp,
)

/** Phone-sized windows: rows sized for a device held at ~35cm. */
internal val CompactTileDimens = GroceryTileDimens(
    itemTileHeight = 48.dp,
    itemTileControlSize = 32.dp,
    itemTileControlIconSize = 18.dp,
)

/** Tablet-sized windows: rows sized for a device propped up at ~70cm. */
internal val ExpandedTileDimens = GroceryTileDimens(
    itemTileHeight = 60.dp,
    itemTileControlSize = 44.dp,
    itemTileControlIconSize = 24.dp,
)

internal fun isExpandedWidth(width: Dp): Boolean = width >= ExpandedWidth

internal fun tileDimensFor(width: Dp): GroceryTileDimens =
    if (isExpandedWidth(width)) ExpandedTileDimens else CompactTileDimens

/** Builds the metrics for a window [width] out of the current Material type scale. */
@Composable
@ReadOnlyComposable
internal fun groceryMetricsFor(width: Dp): GroceryMetrics {
    val expanded = isExpandedWidth(width)
    val dimens = if (expanded) ExpandedTileDimens else CompactTileDimens
    val typography = MaterialTheme.typography
    return GroceryMetrics(
        isExpandedWidth = expanded,
        itemTileHeight = dimens.itemTileHeight,
        itemTileControlSize = dimens.itemTileControlSize,
        itemTileControlIconSize = dimens.itemTileControlIconSize,
        itemName = if (expanded) typography.titleMedium else typography.bodyLarge,
        itemMeta = if (expanded) typography.bodySmall else typography.labelSmall,
    )
}

internal val LocalGroceryMetrics = staticCompositionLocalOf<GroceryMetrics> {
    error("No GroceryMetrics provided; wrap the UI in GroceryTheme.")
}
