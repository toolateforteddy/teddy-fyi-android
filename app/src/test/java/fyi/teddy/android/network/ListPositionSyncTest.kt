package fyi.teddy.android.network

import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.todo.data.TodoList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests for list ordering surviving a sync round-trip.
 *
 * `TodoListDto` and `GroceryListDto` used to omit `position` entirely. Because the DAOs
 * upsert (full-row replace), any remote change to a list reset its local ordering to 0 —
 * silently undoing the user's reordering of spaces.
 */
class ListPositionSyncTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun todoListPositionIsUploaded() {
        val list = TodoList(id = "list-1", name = "Work", position = 4, userId = "user-1")
        assertEquals(4, list.toDto().position)
    }

    @Test
    fun todoListPositionSurvivesRoundTrip() {
        val list = TodoList(id = "list-1", name = "Work", position = 4, userId = "user-1")
        val encoded = json.encodeToString(TodoListDto.serializer(), list.toDto())
        val decoded = json.decodeFromString(TodoListDto.serializer(), encoded)
        assertEquals(4, decoded.toEntity().position)
    }

    @Test
    fun todoListWithoutServerPositionKeepsLocalOrdering() {
        // A server that does not persist ordering omits the field; the local value must win.
        val payload = """
            {"id":"list-1","name":"Work","color_hex":"#000000","user_id":"user-1","created_at":1}
        """.trimIndent()
        val dto = json.decodeFromString(TodoListDto.serializer(), payload)
        assertEquals(7, dto.toEntity(fallbackPosition = 7).position)
    }

    @Test
    fun todoListWithExplicitZeroPositionOverridesLocalOrdering() {
        val payload = """
            {"id":"list-1","name":"Work","color_hex":"#000000","user_id":"user-1",
             "created_at":1,"position":0}
        """.trimIndent()
        val dto = json.decodeFromString(TodoListDto.serializer(), payload)
        assertEquals(0, dto.toEntity(fallbackPosition = 7).position)
    }

    @Test
    fun groceryListPositionIsUploaded() {
        val list = GroceryList(id = "glist-1", name = "Home", position = 2, ownerId = "user-1")
        assertEquals(2, list.toDto().position)
    }

    @Test
    fun groceryListPositionSurvivesRoundTrip() {
        val list = GroceryList(id = "glist-1", name = "Home", position = 2, ownerId = "user-1")
        val encoded = json.encodeToString(GroceryListDto.serializer(), list.toDto())
        val decoded = json.decodeFromString(GroceryListDto.serializer(), encoded)
        assertEquals(2, decoded.toEntity().position)
    }

    @Test
    fun groceryListWithoutServerPositionKeepsLocalOrdering() {
        val payload = """
            {"id":"glist-1","name":"Home","owner_id":"user-1","created_at":1}
        """.trimIndent()
        val dto = json.decodeFromString(GroceryListDto.serializer(), payload)
        assertEquals(5, dto.toEntity(fallbackPosition = 5).position)
    }
}
