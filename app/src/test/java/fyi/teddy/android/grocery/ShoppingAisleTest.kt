package fyi.teddy.android.grocery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.ui.graphics.Color
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.components.ShoppingAisle
import fyi.teddy.android.grocery.ui.components.aisleHeaderIndexes
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The arithmetic behind the aisle jump rail: a tap has to land on the right sign, and the
 * only thing standing between it and the wrong one is this index count.
 */
class ShoppingAisleTest {

    private fun items(count: Int): List<GroceryItem> =
        (1..count).map { GroceryItem(id = "item-$it", name = "Item $it") }

    private fun aisle(key: String, itemCount: Int, isExpanded: Boolean = true) = ShoppingAisle(
        key = key,
        name = key,
        icon = Icons.Default.ShoppingBasket,
        tint = Color.Red,
        items = items(itemCount),
        isExpanded = isExpanded,
    )

    @Test
    fun `first aisle sign is the first grid item`() {
        val indexes = aisleHeaderIndexes(listOf(aisle("produce", 3)))

        assertEquals(listOf(0), indexes)
    }

    @Test
    fun `each sign is offset by the signs and tiles above it`() {
        val indexes = aisleHeaderIndexes(
            listOf(
                aisle("produce", 3),
                aisle("dairy", 2),
                aisle("bakery", 1),
            )
        )

        // produce sign, 3 tiles, dairy sign, 2 tiles, bakery sign.
        assertEquals(listOf(0, 4, 7), indexes)
    }

    @Test
    fun `a collapsed aisle takes only its sign`() {
        val indexes = aisleHeaderIndexes(
            listOf(
                aisle("produce", 5, isExpanded = false),
                aisle("dairy", 2),
            )
        )

        assertEquals(listOf(0, 1), indexes)
    }

    @Test
    fun `an empty list of aisles has no stops`() {
        assertEquals(emptyList<Int>(), aisleHeaderIndexes(emptyList()))
    }

    @Test
    fun `an aisle with no items still occupies its sign`() {
        val indexes = aisleHeaderIndexes(listOf(aisle("produce", 0), aisle("dairy", 1)))

        assertEquals(listOf(0, 1), indexes)
    }
}
