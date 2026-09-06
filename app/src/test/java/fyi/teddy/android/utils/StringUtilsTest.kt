package fyi.teddy.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `formatTitle capitalizes single and multi-word titles`() {
        assertEquals("Apple", StringUtils.formatTitle("apple"))
        assertEquals("Buy Some Milk", StringUtils.formatTitle("buy some milk"))
    }

    @Test
    fun `formatTitle preserves uppercase acronyms`() {
        assertEquals("FIX API BUG", StringUtils.formatTitle("FIX API BUG"))
        assertEquals("Update README File", StringUtils.formatTitle("update README file"))
    }

    @Test
    fun `formatTitle trims leading trailing and extra internal whitespace`() {
        assertEquals("Clean The House", StringUtils.formatTitle("   clean   the   house  "))
    }

    @Test
    fun `formatTitle handles empty and whitespace-only strings`() {
        assertEquals("", StringUtils.formatTitle(""))
        assertEquals("", StringUtils.formatTitle("   "))
    }

    @Test
    fun `formatTitle handles strings with numbers and symbols`() {
        assertEquals("Buy 2 Apples", StringUtils.formatTitle("buy 2 apples"))
        assertEquals("Item #100", StringUtils.formatTitle("item #100"))
    }
}
