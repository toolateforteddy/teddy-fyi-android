package fyi.teddy.android.grocery.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tile metrics are a table, so the test is a table too: it pins the breakpoint and the
 * two sets of sizes rather than the code that looks them up.
 */
class GroceryMetricsTest {

    @Test
    fun `phone widths stay compact`() {
        assertFalse(isExpandedWidth(360.dp))
        assertFalse(isExpandedWidth(599.dp))
        assertEquals(CompactTileDimens, tileDimensFor(411.dp))
    }

    @Test
    fun `the breakpoint is inclusive at 600dp`() {
        assertTrue(isExpandedWidth(ExpandedWidth))
        assertEquals(ExpandedTileDimens, tileDimensFor(600.dp))
    }

    @Test
    fun `tablet widths get the expanded metrics`() {
        // A Fire HD 8 is 800dp wide in landscape, a Fire HD 10 is 1280dp.
        assertTrue(isExpandedWidth(800.dp))
        assertEquals(ExpandedTileDimens, tileDimensFor(1280.dp))
    }

    @Test
    fun `expanded rows sit in the 56-64dp band`() {
        assertTrue(ExpandedTileDimens.itemTileHeight >= 56.dp)
        assertTrue(ExpandedTileDimens.itemTileHeight <= 64.dp)
    }

    @Test
    fun `expanded controls clear the 44dp touch target`() {
        assertTrue(ExpandedTileDimens.itemTileControlSize >= 44.dp)
        assertTrue(ExpandedTileDimens.itemTileControlIconSize > CompactTileDimens.itemTileControlIconSize)
    }

    @Test
    fun `expanded metrics are strictly larger than compact ones`() {
        assertTrue(ExpandedTileDimens.itemTileHeight > CompactTileDimens.itemTileHeight)
        assertTrue(ExpandedTileDimens.itemTileControlSize > CompactTileDimens.itemTileControlSize)
    }
}
