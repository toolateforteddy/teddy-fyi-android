package fyi.teddy.android.grocery

import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.ui.components.inlineControlsFit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quantity steppers sit beside the item name whenever the tile can spare the width, and drop
 * to their own line otherwise. Either way the name stays on screen, so no layout may hide it.
 */
class InlineControlsFitTest {

    @Test
    fun `tablet grid tile reveals the controls beside the name`() {
        // A two-column grid on a ~840dp tablet leaves each tile well past 220dp.
        assertTrue(inlineControlsFit(tileWidth = 400.dp, withDelete = true))
        assertTrue(inlineControlsFit(tileWidth = 280.dp, withDelete = true))
    }

    @Test
    fun `narrow phone grid tile stacks the controls instead`() {
        // Two columns on a 360dp phone give each need tile roughly 168dp.
        assertFalse(inlineControlsFit(tileWidth = 168.dp, withDelete = true))
    }

    @Test
    fun `planning row on a phone is wide enough for inline controls`() {
        // The planning list is full width and has no delete button in the control row.
        assertTrue(inlineControlsFit(tileWidth = 336.dp, withDelete = false))
    }

    @Test
    fun `dropping the delete button lowers the width the controls need`() {
        // A tile too narrow for the need tile's four buttons can still be wide enough for the
        // planning tile's three.
        assertFalse(inlineControlsFit(tileWidth = 260.dp, withDelete = true))
        assertTrue(inlineControlsFit(tileWidth = 260.dp, withDelete = false))

        // The shorter control row must never be the fussier of the two at any width.
        for (width in 0..480) {
            if (inlineControlsFit(width.dp, withDelete = true)) {
                assertTrue("${width}dp fits delete but not the shorter row", inlineControlsFit(width.dp, withDelete = false))
            }
        }
    }
}
