package fyi.teddy.android.grocery

import fyi.teddy.android.grocery.ui.components.DOCKED_ADD_PANE_MIN_WIDTH_DP
import fyi.teddy.android.grocery.ui.components.shouldDockAddItemPane
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The add-item entry is a modal sheet on a phone and a docked pane on a tablet. These pin the
 * width at which it flips, so nobody quietly regresses tablets back to the full-width slab.
 */
class AddItemPaneBreakpointTest {

    @Test
    fun `phone widths keep the modal sheet`() {
        assertFalse(shouldDockAddItemPane(screenWidthDp = 360))
        assertFalse(shouldDockAddItemPane(screenWidthDp = 411))
        assertFalse(shouldDockAddItemPane(screenWidthDp = 600))
    }

    @Test
    fun `tablet widths dock the entry field`() {
        assertTrue(shouldDockAddItemPane(screenWidthDp = 800))
        assertTrue(shouldDockAddItemPane(screenWidthDp = 960))
        assertTrue(shouldDockAddItemPane(screenWidthDp = 1280))
    }

    @Test
    fun `the breakpoint itself docks, one dp below it does not`() {
        assertTrue(shouldDockAddItemPane(DOCKED_ADD_PANE_MIN_WIDTH_DP))
        assertFalse(shouldDockAddItemPane(DOCKED_ADD_PANE_MIN_WIDTH_DP - 1))
    }
}
