package fyi.teddy.android.ui.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reference windows: a phone (411 x 890), a Pixel Tablet-class 11" in landscape
 * (1280 x 800), and a short 8" tablet in landscape (800 x 533) where the old flat
 * 400dp caps overflowed.
 */
class WindowMetricsTest {

    @Test
    fun `dialog list height scales with the window`() {
        assertEquals(445.dp, fractionOfHeight(890.dp, fraction = 0.5f, min = 200.dp))
        assertEquals(400.dp, fractionOfHeight(800.dp, fraction = 0.5f, min = 200.dp))
        assertEquals(266.5.dp, fractionOfHeight(533.dp, fraction = 0.5f, min = 200.dp))
    }

    @Test
    fun `dialog list height is floored on very short windows`() {
        assertEquals(200.dp, fractionOfHeight(320.dp, fraction = 0.5f, min = 200.dp))
    }

    @Test
    fun `dialog list height honours its ceiling`() {
        assertEquals(280.dp, fractionOfHeight(2000.dp, fraction = 0.22f, min = 120.dp, max = 280.dp))
    }

    @Test
    fun `ceiling below the floor still yields the floor`() {
        assertEquals(200.dp, fractionOfHeight(890.dp, fraction = 0.5f, min = 200.dp, max = 100.dp))
    }

    @Test
    fun `column count follows the available width`() {
        // Mirrors GridCells.Adaptive(220dp) in the recommendation tray.
        assertEquals(1, columnsForWidth(411.dp, minColumnWidth = 220.dp))
        assertEquals(3, columnsForWidth(800.dp, minColumnWidth = 220.dp))
        assertEquals(5, columnsForWidth(1280.dp, minColumnWidth = 220.dp))
    }

    @Test
    fun `column count is at least one on a narrow window`() {
        assertEquals(1, columnsForWidth(100.dp, minColumnWidth = 220.dp))
    }

    @Test
    fun `grid capacity fills the tray without a trailing gap`() {
        // 4 rows of 40dp + 3 gaps of 8dp = 184dp, and a 5th row would need 232dp.
        assertEquals(8, itemsThatFit(222.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2))
        assertEquals(4, itemsThatFit(88.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2))
    }

    @Test
    fun `recommendation capacity per reference pane`() {
        // Mirrors PlanningPhaseContent's BoxWithConstraints arithmetic: the tray is the
        // full pane height beside the list and a floored quarter of it when stacked,
        // minus the tray heading, at the grid's own adaptive column count — never fewer
        // than the 8 recommendations a phone has always shown.
        fun capacity(paneWidth: Int, paneHeight: Int): Int {
            val twoPane = paneWidth >= 600
            val trayHeight = if (twoPane) {
                paneHeight.dp
            } else {
                fractionOfHeight(paneHeight.dp, fraction = 0.25f, min = 124.dp)
            }
            val trayWidth = if (twoPane) (paneWidth.dp - 16.dp) * 0.4f else paneWidth.dp
            return itemsThatFit(
                availableHeight = trayHeight - 28.dp,
                rowHeight = 40.dp,
                rowSpacing = 8.dp,
                columns = columnsForWidth(trayWidth, minColumnWidth = 220.dp)
            ).coerceAtLeast(8)
        }

        // Phone portrait: stacked, one column, floored at 8.
        assertEquals(8, capacity(paneWidth = 379, paneHeight = 700))
        // 11" tablet landscape: two-pane, a 461dp tray pane holding 2 x 12.
        assertEquals(24, capacity(paneWidth = 1170, paneHeight = 640))
        // 8" tablet landscape: two-pane but short and narrow, so the floor still applies.
        assertEquals(8, capacity(paneWidth = 700, paneHeight = 380))
    }

    @Test
    fun `grid capacity never collapses to nothing`() {
        assertEquals(2, itemsThatFit(0.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2))
        assertEquals(1, itemsThatFit(40.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 0))
    }
}
