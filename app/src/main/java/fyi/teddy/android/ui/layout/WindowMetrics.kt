package fyi.teddy.android.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Helpers for sizing UI against the space it is actually drawn in — the window, or a
 * pane measured by BoxWithConstraints — rather than against a fixed dp constant picked
 * on one reference phone. A hardcoded cap is simultaneously too small on a tall tablet
 * and too large on a short landscape window; a floored fraction is neither.
 */

/** Size of the window this composition is drawn in (the app window, not the physical display). */
@Composable
@ReadOnlyComposable
fun windowSizeDp(): DpSize {
    val container = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        DpSize(container.width.toDp(), container.height.toDp())
    }
}

/**
 * A height cap expressed as [fraction] of the window height, clamped between [min] and [max].
 *
 * The floor keeps the element usable on short windows (a landscape 8" tablet is
 * only ~533dp tall); the ceiling stops a single list from swallowing a very tall one.
 */
@Composable
@ReadOnlyComposable
fun fractionOfWindowHeight(
    fraction: Float,
    min: Dp,
    max: Dp = Dp.Infinity
): Dp = fractionOfHeight(windowSizeDp().height, fraction, min, max)

/**
 * Non-composable core of [fractionOfWindowHeight]: a floored fraction of any height,
 * so it works against a measured pane too, and so the clamping is unit-testable.
 */
fun fractionOfHeight(
    availableHeight: Dp,
    fraction: Float,
    min: Dp,
    max: Dp = Dp.Infinity
): Dp = (availableHeight * fraction).coerceIn(min, max.coerceAtLeast(min))

/**
 * Number of grid columns that fit [availableWidth] giving each column at least
 * [minColumnWidth], and at least one. Mirrors what `GridCells.Adaptive` resolves to,
 * for code that needs the count as a number rather than as a layout.
 */
fun columnsForWidth(availableWidth: Dp, minColumnWidth: Dp): Int {
    require(minColumnWidth > 0.dp) { "minColumnWidth must be positive" }
    return (availableWidth / minColumnWidth).toInt().coerceAtLeast(1)
}

/**
 * How many items a uniform grid can show inside [availableHeight] without scrolling.
 *
 * Rows are [rowHeight] tall with [rowSpacing] between them (no trailing gap), and the
 * result is rows x [columns] — at least one row and one column, so the grid is never empty.
 */
fun itemsThatFit(
    availableHeight: Dp,
    rowHeight: Dp,
    rowSpacing: Dp,
    columns: Int
): Int {
    require(rowHeight > 0.dp) { "rowHeight must be positive" }
    val rows = ((availableHeight + rowSpacing) / (rowHeight + rowSpacing)).toInt()
    return rows.coerceAtLeast(1) * columns.coerceAtLeast(1)
}
