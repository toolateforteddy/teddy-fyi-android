package fyi.teddy.android.grocery

import fyi.teddy.android.grocery.ui.components.glyphForItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroceryGlyphsTest {

    @Test
    fun `glyphForItem matches exact item names`() {
        assertEquals("🍌", glyphForItem("bananas"))
        assertEquals("🍎", glyphForItem("apple"))
        assertEquals("🥛", glyphForItem("milk"))
        assertEquals("🥚", glyphForItem("eggs"))
        assertEquals("🍞", glyphForItem("bread"))
    }

    @Test
    fun `glyphForItem is case insensitive and handles extra words`() {
        assertEquals("🍌", glyphForItem("Organic Bananas"))
        assertEquals("🍗", glyphForItem("FRESH CHICKEN BREAST"))
        assertEquals("🧀", glyphForItem("250g Cheddar Cheese"))
    }

    @Test
    fun `glyphForItem ignores punctuation`() {
        assertEquals("🍅", glyphForItem("Tomatoes, organic!"))
        assertEquals("☕", glyphForItem("Coffee (espresso)"))
    }

    @Test
    fun `glyphForItem returns null for unmapped items or empty inputs`() {
        assertNull(glyphForItem("UnmappedProduct123"))
        assertNull(glyphForItem(""))
        assertNull(glyphForItem("   "))
    }
}
