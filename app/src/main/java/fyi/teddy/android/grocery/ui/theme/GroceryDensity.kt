package fyi.teddy.android.grocery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * How big the Grocery UI draws itself.
 *
 * "Docked on the kitchen counter" and "in my hands in aisle six" are the same device at
 * two different distances, and no window size class can tell them apart -- only the person
 * holding it knows. So this is a preference, not a measurement.
 */
enum class GroceryDensity(
    /** Stable key written to preferences; renaming an enum constant must not move anybody's setting. */
    val storageKey: String,
    /** Name shown in settings. */
    val label: String,
    /** One line under the name, saying when you would pick it. */
    val blurb: String,
) {
    COMPACT("compact", "Compact", "Most items per screen. Phone in your hand."),
    COMFORTABLE("comfortable", "Comfortable", "The everyday size."),
    ACROSS_THE_KITCHEN("across_the_kitchen", "Across the kitchen", "Readable from the doorway."),
    ;

    companion object {
        /** What a device gets before anybody has chosen. */
        val Default = COMFORTABLE

        /** Reads back [storageKey], falling back to [Default] for anything unrecognised. */
        fun fromStorageKey(key: String?): GroceryDensity =
            entries.firstOrNull { it.storageKey == key } ?: Default
    }
}

/**
 * The sizes one [GroceryDensity] resolves to.
 *
 * Screens read these instead of hard-coding dp, so a density change moves every surface
 * together rather than leaving one list dense and the next one roomy.
 */
@Immutable
data class GroceryMetrics(
    /** The preference these sizes came from. */
    val density: GroceryDensity,
    /** Height of an item tile in the shopping grid. */
    val tileHeight: Dp,
    /**
     * Narrowest a shopping-grid column may be. The grid is adaptive, so this is what decides
     * how many columns a screen gets. [GroceryDensity.COMFORTABLE] keeps the 220dp the grid
     * has always used -- the width an item name actually needs -- so the default layout is
     * unchanged; compact trades that for more columns, and across-the-kitchen for fewer.
     */
    val minTileWidth: Dp,
    /** Gap between tiles, and between the grid and the aisle rail. */
    val gutter: Dp,
    /** Height of an aisle sign. */
    val signHeight: Dp,
    /** Size of the mark at the head of an item tile. */
    val glyphSize: Dp,
    /**
     * Touch target of the +/-/category buttons that open inside a need or planning tile.
     * They sit in a [tileHeight]-tall row, so they have to move with it.
     */
    val controlSize: Dp,
    /** Item name size on a shopping tile. */
    val itemFontSize: TextUnit,
    /** Width of the aisle jump rail. */
    val railWidth: Dp,
)

private val CompactMetrics = GroceryMetrics(
    density = GroceryDensity.COMPACT,
    tileHeight = 40.dp,
    minTileWidth = 150.dp,
    gutter = 6.dp,
    signHeight = 30.dp,
    glyphSize = 16.dp,
    controlSize = 28.dp,
    itemFontSize = 13.sp,
    railWidth = 36.dp,
)

private val ComfortableMetrics = GroceryMetrics(
    density = GroceryDensity.COMFORTABLE,
    tileHeight = 48.dp,
    minTileWidth = 220.dp,
    gutter = 8.dp,
    signHeight = 36.dp,
    glyphSize = 18.dp,
    controlSize = 32.dp,
    itemFontSize = 15.sp,
    railWidth = 44.dp,
)

private val AcrossTheKitchenMetrics = GroceryMetrics(
    density = GroceryDensity.ACROSS_THE_KITCHEN,
    tileHeight = 64.dp,
    minTileWidth = 300.dp,
    gutter = 12.dp,
    signHeight = 48.dp,
    glyphSize = 24.dp,
    controlSize = 44.dp,
    itemFontSize = 20.sp,
    railWidth = 56.dp,
)

/** The sizes for a density. */
fun metricsFor(density: GroceryDensity): GroceryMetrics = when (density) {
    GroceryDensity.COMPACT -> CompactMetrics
    GroceryDensity.COMFORTABLE -> ComfortableMetrics
    GroceryDensity.ACROSS_THE_KITCHEN -> AcrossTheKitchenMetrics
}
