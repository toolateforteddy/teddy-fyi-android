package fyi.teddy.android.grocery.ui

import fyi.teddy.android.grocery.ui.components.AisleSpan
import fyi.teddy.android.grocery.ui.components.aisleSpans
import fyi.teddy.android.grocery.ui.components.pinnedAisle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The arithmetic behind the pinned aisle sign in the shopping grid: which grid indices
 * belong to which aisle, and therefore which sign should stay on screen.
 */
class AisleScrollLayoutTest {

    private val produce = "produce"
    private val dairy = "dairy"

    private fun allExpanded(): (String?) -> Boolean = { true }

    @Test
    fun `each aisle starts after the sign and items of the one before it`() {
        val spans = aisleSpans(listOf(produce to 3, dairy to 2, null to 1), allExpanded())

        assertEquals(
            listOf(
                AisleSpan(produce, headerIndex = 0, visibleItemCount = 3),
                AisleSpan(dairy, headerIndex = 4, visibleItemCount = 2),
                AisleSpan(null, headerIndex = 7, visibleItemCount = 1),
            ),
            spans,
        )
    }

    @Test
    fun `a collapsed aisle takes only the row its sign is on`() {
        val spans = aisleSpans(listOf(produce to 3, dairy to 2)) { it != produce }

        assertEquals(
            listOf(
                AisleSpan(produce, headerIndex = 0, visibleItemCount = 0),
                AisleSpan(dairy, headerIndex = 1, visibleItemCount = 2),
            ),
            spans,
        )
    }

    @Test
    fun `nothing is pinned while the aisle's own sign is on screen`() {
        val spans = aisleSpans(listOf(produce to 3, dairy to 2), allExpanded())

        assertNull(pinnedAisle(spans, firstVisibleItemIndex = 0))
        assertNull(pinnedAisle(spans, firstVisibleItemIndex = 4))
    }

    @Test
    fun `the aisle being scrolled through is pinned`() {
        val spans = aisleSpans(listOf(produce to 3, dairy to 2), allExpanded())

        assertEquals(produce, pinnedAisle(spans, firstVisibleItemIndex = 1)?.categoryId)
        assertEquals(produce, pinnedAisle(spans, firstVisibleItemIndex = 3)?.categoryId)
        assertEquals(dairy, pinnedAisle(spans, firstVisibleItemIndex = 5)?.categoryId)
        assertEquals(dairy, pinnedAisle(spans, firstVisibleItemIndex = 6)?.categoryId)
    }

    @Test
    fun `the uncategorised aisle is pinned like any other`() {
        val spans = aisleSpans(listOf(produce to 1, null to 2), allExpanded())

        val pinned = pinnedAisle(spans, firstVisibleItemIndex = 3)

        assertEquals(2, pinned?.headerIndex)
        assertNull(pinned?.categoryId)
    }

    @Test
    fun `nothing is pinned once the list scrolls past the aisles into the cart`() {
        val spans = aisleSpans(listOf(produce to 3, dairy to 2), allExpanded())

        assertNull(pinnedAisle(spans, firstVisibleItemIndex = 7))
        assertNull(pinnedAisle(spans, firstVisibleItemIndex = 12))
    }

    @Test
    fun `an empty list pins nothing`() {
        assertNull(pinnedAisle(aisleSpans(emptyList(), allExpanded()), firstVisibleItemIndex = 0))
    }
}
