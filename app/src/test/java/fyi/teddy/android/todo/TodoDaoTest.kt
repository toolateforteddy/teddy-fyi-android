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
        val item = TodoItem(title = "Task 1")
        todoDao.insertItem(item)
        
        val allItems = todoDao.getAllItems().first()
        assertEquals(1, allItems.size)
        assertEquals("Task 1", allItems[0].title)
        assertFalse(allItems[0].isCompleted)
    }

    @Test
    fun updateTodoItemCompletion() = runTest {
        val item = TodoItem(id = 1, title = "Task 1", isCompleted = false)
        todoDao.insertItem(item)
        
        todoDao.updateItem(item.copy(isCompleted = true))
        
        val allItems = todoDao.getAllItems().first()
        assertTrue(allItems[0].isCompleted)
    }

    @Test
    fun deleteTodoItem() = runTest {
        val item = TodoItem(id = 1, title = "Task 1")
        todoDao.insertItem(item)
        todoDao.deleteItem(item)
        
        val allItems = todoDao.getAllItems().first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    fun deleteAllTodos() = runTest {
        todoDao.insertItem(TodoItem(title = "Task 1"))
        todoDao.insertItem(TodoItem(title = "Task 2"))
        
        todoDao.deleteAll()
        
        val allItems = todoDao.getAllItems().first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    fun allTodosOrdering_descendingCreatedAt() = runTest {
        val item1 = TodoItem(id = 1, title = "Older", createdAt = 1000)
        val item2 = TodoItem(id = 2, title = "Newer", createdAt = 2000)
        
        todoDao.insertItem(item1)
        todoDao.insertItem(item2)
        
        val allItems = todoDao.getAllItems().first()
        assertEquals(2, allItems[0].id) // Newer first
        assertEquals(1, allItems[1].id) // Older second
    }
}
