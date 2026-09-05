package fyi.teddy.android.grocery

import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.components.inlineControlsFit
import fyi.teddy.android.grocery.ui.theme.GroceryDensity
import fyi.teddy.android.grocery.ui.theme.metricsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The densities are a table, so the test is one too: every size moves the same direction, and
 * the controls that open inside a tile stay inside it.
 */
class GroceryDensityMetricsTest {

    private val compact = metricsFor(GroceryDensity.COMPACT)
    private val comfortable = metricsFor(GroceryDensity.COMFORTABLE)
    private val roomy = metricsFor(GroceryDensity.ACROSS_THE_KITCHEN)

    @Test
    fun `every density reports the one it came from`() {
        GroceryDensity.entries.forEach { assertEquals(it, metricsFor(it).density) }
    }

    @Test
    fun `tiles and their controls grow together`() {
        assertTrue(compact.tileHeight < comfortable.tileHeight)
        assertTrue(comfortable.tileHeight < roomy.tileHeight)
        assertTrue(compact.controlSize < comfortable.controlSize)
        assertTrue(comfortable.controlSize < roomy.controlSize)
        assertTrue(compact.itemFontSize.value < comfortable.itemFontSize.value)
        assertTrue(comfortable.itemFontSize.value < roomy.itemFontSize.value)
    }

    @Test
    fun `controls fit inside the tile row they open in`() {
        listOf(compact, comfortable, roomy).forEach {
            assertTrue("${it.density} controls overflow its tile", it.controlSize <= it.tileHeight)
        }
    }

    @Test
    fun `the roomiest density clears a 44dp touch target`() {
        // The point of "across the kitchen" is a target you can hit without looking at it.
        assertTrue(roomy.controlSize >= 44.dp)
    }

    @Test
    fun `wider controls need a wider tile to stay inline`() {
        // The stacking decision has to be made against the buttons that actually get laid out:
        // a tile that fits four comfortable buttons need not fit four across-the-kitchen ones.
        assertTrue(inlineControlsFit(280.dp, withDelete = true, buttonSize = comfortable.controlSize))
        assertFalse(inlineControlsFit(280.dp, withDelete = true, buttonSize = roomy.controlSize))
    }
}
