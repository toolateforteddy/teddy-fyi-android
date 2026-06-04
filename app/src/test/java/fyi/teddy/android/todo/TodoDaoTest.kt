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
}
