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
    fun `column count follows the window width within bounds`() {
        assertEquals(2, columnsForWidth(411.dp, minColumnWidth = 200.dp, min = 2, max = 4))
        assertEquals(4, columnsForWidth(800.dp, minColumnWidth = 200.dp, min = 2, max = 4))
        assertEquals(4, columnsForWidth(1280.dp, minColumnWidth = 200.dp, min = 2, max = 4))
    }

    @Test
    fun `grid capacity fills the tray without a trailing gap`() {
        // 4 rows of 40dp + 3 gaps of 8dp = 184dp, and a 5th row would need 232dp.
        assertEquals(8, itemsThatFit(222.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2, minRows = 2))
        assertEquals(4, itemsThatFit(88.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2, minRows = 2))
    }

    @Test
    fun `recommendation capacity per reference window`() {
        fun capacity(width: Int, height: Int): Int {
            val tray = fractionOfHeight(height.dp, fraction = 0.25f, min = 88.dp)
            val columns = columnsForWidth(width.dp, minColumnWidth = 200.dp, min = 2, max = 4)
            return itemsThatFit(tray, rowHeight = 40.dp, rowSpacing = 8.dp, columns = columns, minRows = 2)
        }

        assertEquals(8, capacity(width = 411, height = 890))   // phone: unchanged from the old hardcoded 8
        assertEquals(16, capacity(width = 1280, height = 800)) // 11" tablet landscape
        assertEquals(8, capacity(width = 800, height = 533))   // 8" tablet landscape, short window
    }

    @Test
    fun `grid capacity never collapses to nothing`() {
        assertEquals(4, itemsThatFit(0.dp, rowHeight = 40.dp, rowSpacing = 8.dp, columns = 2, minRows = 2))
    }
}
