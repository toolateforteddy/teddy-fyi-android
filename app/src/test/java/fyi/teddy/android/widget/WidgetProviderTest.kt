package fyi.teddy.android.widget

import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.todo.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WidgetProviderTest {

    @Test
    fun testGlanceWidgetReceiversInstantiation() {
        val todoReceiver = TodoTacticalWidgetReceiver()
        val groceryReceiver = GroceryWidgetReceiver()

        assertNotNull(todoReceiver.glanceAppWidget)
        assertNotNull(groceryReceiver.glanceAppWidget)
    }

    @Test
    fun testTacticalHexCanvasRenderer_emptyItems() {
        val bitmap = TacticalHexCanvasRenderer.renderHexGrid(
            todoItems = emptyList(),
            widthPx = 300,
            heightPx = 300,
            density = 2f
        )
        assertNotNull(bitmap)
        assertEquals(300, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun testTacticalHexCanvasRenderer_withItems() {
        val items = listOf(
            TodoItem(id = "1", title = "Task 1", position = 0),
            TodoItem(id = "2", title = "Task 2", position = 1),
            TodoItem(id = "3", title = "Task 3", position = 2)
        )
        val bitmap = TacticalHexCanvasRenderer.renderHexGrid(
            todoItems = items,
            widthPx = 400,
            heightPx = 400,
            density = 2f
        )
        assertNotNull(bitmap)
        assertEquals(400, bitmap.width)
        assertEquals(400, bitmap.height)
    }

    @Test
    fun testGroceryWidgetRenderer_compactAndExpanded() {
        val items = listOf(
            GroceryItem(id = "1", name = "Apples", isActive = true, isBought = false),
            GroceryItem(id = "2", name = "Milk", isActive = true, isBought = true),
            GroceryItem(id = "3", name = "Bread", isActive = true, isBought = false)
        )

        // Compact height test
        val compactBitmap = GroceryWidgetRenderer.renderGroceryCard(
            groceryItems = items,
            widthPx = 250,
            heightPx = 100,
            density = 2f
        )
        assertNotNull(compactBitmap)
        assertEquals(250, compactBitmap.width)
        assertEquals(100, compactBitmap.height)

        // Expanded height test
        val expandedBitmap = GroceryWidgetRenderer.renderGroceryCard(
            groceryItems = items,
            widthPx = 350,
            heightPx = 300,
            density = 2f
        )
        assertNotNull(expandedBitmap)
        assertEquals(350, expandedBitmap.width)
        assertEquals(300, expandedBitmap.height)
    }
}
