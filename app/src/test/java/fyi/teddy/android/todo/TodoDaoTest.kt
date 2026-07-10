package fyi.teddy.android.todo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao
    private val testUserId = "user123"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        todoDao = database.todoDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTodoItem() = runTest {
        val item = TodoItem(title = "Task 1", userId = testUserId)
        todoDao.insertItem(item)
        
        val allItems = todoDao.getAllItems(testUserId).first()
        assertEquals(1, allItems.size)
        assertEquals("Task 1", allItems[0].title)
        assertFalse(allItems[0].isCompleted)
        assertEquals(testUserId, allItems[0].userId)
    }

    @Test
    fun updateTodoItemCompletion() = runTest {
        val item = TodoItem(id = "1", title = "Task 1", isCompleted = false, userId = testUserId)
        todoDao.insertItem(item)
        
        todoDao.updateItem(item.copy(isCompleted = true))
        
        val allItems = todoDao.getAllItems(testUserId).first()
        assertTrue(allItems[0].isCompleted)
    }

    @Test
    fun deleteTodoItem() = runTest {
        val item = TodoItem(id = "1", title = "Task 1", userId = testUserId)
        todoDao.insertItem(item)
        todoDao.deleteItem(item)
        
        val allItems = todoDao.getAllItems(testUserId).first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    fun deleteAllTodosForUser() = runTest {
        todoDao.insertItem(TodoItem(title = "User 1 Task", userId = "user1"))
        todoDao.insertItem(TodoItem(title = "User 2 Task", userId = "user2"))
        
        todoDao.deleteAll("user1")
        
        assertTrue(todoDao.getAllItems("user1").first().isEmpty())
        assertEquals(1, todoDao.getAllItems("user2").first().size)
    }

    @Test
    fun allTodosOrdering_descendingCreatedAt() = runTest {
        val item1 = TodoItem(id = "1", title = "Older", createdAt = 1000, userId = testUserId)
        val item2 = TodoItem(id = "2", title = "Newer", createdAt = 2000, userId = testUserId)
        
        todoDao.insertItem(item1)
        todoDao.insertItem(item2)
        
        val allItems = todoDao.getAllItems(testUserId).first()
        assertEquals("2", allItems[0].id) // Newer first
        assertEquals("1", allItems[1].id) // Older second
    }

    @Test
    fun userIsolation() = runTest {
        todoDao.insertItem(TodoItem(title = "Task User A", userId = "A"))
        todoDao.insertItem(TodoItem(title = "Task User B", userId = "B"))

        val itemsA = todoDao.getAllItems("A").first()
        val itemsB = todoDao.getAllItems("B").first()

        assertEquals(1, itemsA.size)
        assertEquals("Task User A", itemsA[0].title)
        assertEquals(1, itemsB.size)
        assertEquals("Task User B", itemsB[0].title)
    }

    @Test
    fun claimUnownedItems() = runTest {
        todoDao.insertItem(TodoItem(title = "Unowned Task", userId = null))
        todoDao.insertItem(TodoItem(title = "Owned Task", userId = "other"))

        todoDao.claimUnownedItems(testUserId)

        val items = todoDao.getAllItems(testUserId).first()
        assertEquals(1, items.size)
        assertEquals("Unowned Task", items[0].title)
        assertEquals(testUserId, items[0].userId)
    }

    @Test
    fun softDeletedItemsAreExcluded() = runTest {
        val today = "2023-10-27"
        val tomorrow = "2023-10-28"
        
        // 1. All Items
        todoDao.insertItem(TodoItem(id = "active", title = "Active", userId = testUserId, isDeleted = false))
        todoDao.insertItem(TodoItem(id = "deleted", title = "Deleted", userId = testUserId, isDeleted = true))
        
        val allItems = todoDao.getAllItems(testUserId).first()
        assertEquals(1, allItems.size)
        assertEquals("active", allItems[0].id)
        
        // 2. Today Items
        todoDao.insertItem(TodoItem(id = "today_active", title = "Today Active", userId = testUserId, scheduledDate = today, isDeleted = false))
        todoDao.insertItem(TodoItem(id = "today_deleted", title = "Today Deleted", userId = testUserId, scheduledDate = today, isDeleted = true))
        
        val todayItems = todoDao.getTodayItems(testUserId, today).first()
        // Should find "active" (as it matches general criteria) and "today_active"
        assertTrue(todayItems.any { it.id == "active" })
        assertTrue(todayItems.any { it.id == "today_active" })
        assertFalse(todayItems.any { it.id == "deleted" })
        assertFalse(todayItems.any { it.id == "today_deleted" })
        
        // 3. Scheduled Items
        todoDao.insertItem(TodoItem(id = "future_active", title = "Future Active", userId = testUserId, scheduledDate = tomorrow, isDeleted = false))
        todoDao.insertItem(TodoItem(id = "future_deleted", title = "Future Deleted", userId = testUserId, scheduledDate = tomorrow, isDeleted = true))
        
        val scheduledItems = todoDao.getScheduledItems(testUserId, today).first()
        assertEquals(1, scheduledItems.size)
        assertEquals("future_active", scheduledItems[0].id)
    }
}
