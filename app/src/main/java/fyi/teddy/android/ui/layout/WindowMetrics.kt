package fyi.teddy.android.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Helpers for sizing UI against the window it is actually drawn in, rather than
 * against a fixed dp constant picked on one reference phone. A hardcoded cap is
 * simultaneously too small on a tall tablet and too large on a short landscape
 * window; a floored fraction of the window is neither.
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
 * A height cap expressed as [fraction] of the window height, clamped to [[min], [max]].
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

/** Non-composable core of [fractionOfWindowHeight], so the clamping is unit-testable. */
fun fractionOfHeight(
    windowHeight: Dp,
    fraction: Float,
    min: Dp,
    max: Dp = Dp.Infinity
): Dp = (windowHeight * fraction).coerceIn(min, max.coerceAtLeast(min))

/**
 * Number of grid columns that fit the window width, giving each column at least
 * [minColumnWidth], clamped to [[min], [max]].
 */
@Composable
@ReadOnlyComposable
fun columnsForWindowWidth(
    minColumnWidth: Dp,
    min: Int = 1,
    max: Int = Int.MAX_VALUE
): Int = columnsForWidth(windowSizeDp().width, minColumnWidth, min, max)

/** Non-composable core of [columnsForWindowWidth], so the arithmetic is unit-testable. */
fun columnsForWidth(
    windowWidth: Dp,
    minColumnWidth: Dp,
    min: Int = 1,
    max: Int = Int.MAX_VALUE
): Int {
    require(minColumnWidth > 0.dp) { "minColumnWidth must be positive" }
    val fits = (windowWidth / minColumnWidth).toInt()
    return fits.coerceIn(min, max.coerceAtLeast(min))
}

/**
 * How many items a uniform grid can show inside [availableHeight] without scrolling.
 *
 * Rows are [rowHeight] tall with [rowSpacing] between them (no trailing gap), and the
 * result is rows x [columns], with at least [minRows] rows so the grid never renders empty.
 */
fun itemsThatFit(
    availableHeight: Dp,
    rowHeight: Dp,
    rowSpacing: Dp,
    columns: Int,
    minRows: Int = 1
): Int {
    require(rowHeight > 0.dp) { "rowHeight must be positive" }
    val rows = ((availableHeight + rowSpacing) / (rowHeight + rowSpacing)).toInt()
    return rows.coerceAtLeast(minRows) * columns.coerceAtLeast(1)
}
