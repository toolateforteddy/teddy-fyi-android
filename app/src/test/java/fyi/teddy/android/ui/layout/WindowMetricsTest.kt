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
    fun `column count follows the window width`() {
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
    fun `recommendation capacity per reference window`() {
        // Mirrors PlanningPhaseContent: a quarter of the window height, the grid's own
        // adaptive column count, and never fewer than the 8 a phone has always shown.
        fun capacity(width: Int, height: Int): Int {
            val tray = fractionOfHeight(height.dp, fraction = 0.25f, min = 88.dp)
            val columns = columnsForWidth(width.dp, minColumnWidth = 220.dp)
            return itemsThatFit(tray, rowHeight = 40.dp, rowSpacing = 8.dp, columns = columns)
                .coerceAtLeast(8)
        }

        assertEquals(8, capacity(width = 411, height = 890))   // phone: one column, floored at 8
        assertEquals(20, capacity(width = 1280, height = 800)) // 11" tablet landscape: 5 x 4
        assertEquals(8, capacity(width = 800, height = 533))   // 8" landscape: 3 x 2, floored at 8
    }

    @Test
    fun `grid capacity never collapses to nothing`() {
        assertEquals(2, itemsThatFit(0.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2))
        assertEquals(1, itemsThatFit(40.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 0))
    }
}
