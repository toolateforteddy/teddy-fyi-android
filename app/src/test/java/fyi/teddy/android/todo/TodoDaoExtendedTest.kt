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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TodoDaoExtendedTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao
    private val userId = "test_user"
    private val today = LocalDate.now().toString()

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
    fun getAllItems_retrievesTasks() = runTest {
        val task = TodoItem(title = "Task", userId = userId)
        todoDao.insertItem(task)
        
        val items = todoDao.getAllItems(userId).first()
        assertEquals(1, items.size)
        assertEquals("Task", items[0].title)
    }

    @Test
    fun getTodayItems_includesScheduled() = runTest {
        val notPlanned = TodoItem(title = "Not Planned", scheduledDate = null, userId = userId)
        
        todoDao.insertItem(TodoItem(id = 1, title = "Planned", scheduledDate = today, userId = userId))
        todoDao.insertItem(notPlanned)
        todoDao.insertItem(TodoItem(id = 2, title = "Child", parentId = 1, scheduledDate = null, userId = userId))
        
        val items = todoDao.getTodayItems(userId, today).first()
        assertTrue(items.any { it.title == "Planned" })
        assertFalse(items.any { it.title == "Not Planned" })
        assertTrue(items.any { it.title == "Child" })
    }

    @Test
    fun getTodayItems_ordering_plannedFirst() = runTest {
        val dueSoon = TodoItem(id = 1, title = "Due Soon", userId = userId, scheduledDate = null)
        val planned = TodoItem(id = 2, title = "Planned", scheduledDate = today, userId = userId)
        
        todoDao.insertItem(dueSoon)
        todoDao.insertItem(planned)
        
        val items = todoDao.getTodayItems(userId, today).first()
        assertEquals("Planned", items[0].title)
    }

    @Test
    fun resetPlannedItems_ignoresDaily() = runTest {
        val task1 = TodoItem(id = 1, title = "Normal", scheduledDate = today, isDaily = false, userId = userId)
        val task2 = TodoItem(id = 2, title = "Daily", scheduledDate = today, isDaily = true, userId = userId)
        
        todoDao.insertItem(task1)
        todoDao.insertItem(task2)
        
        todoDao.resetPlannedItems(userId)
        
        val items = todoDao.getAllItems(userId).first()
        val normal = items.find { it.id == 1 }!!
        val daily = items.find { it.id == 2 }!!
        
        assertNull(normal.scheduledDate)
        assertEquals(today, daily.scheduledDate)
    }

    @Test
    fun resetDailyItems_setsScheduledDate() = runTest {
        val task = TodoItem(id = 1, title = "Daily", isCompleted = true, scheduledDate = null, isDaily = true, userId = userId)
        todoDao.insertItem(task)
        
        todoDao.resetDailyItems(userId, today)
        
        val items = todoDao.getAllItems(userId).first()
        assertEquals(today, items[0].scheduledDate)
        assertFalse(items[0].isCompleted)
    }

    @Test
    fun nestedTasks_retrieval() = runTest {
        val parent = TodoItem(id = 1, title = "Parent", userId = userId)
        val child = TodoItem(id = 2, title = "Child", parentId = 1, userId = userId)
        
        todoDao.insertItem(parent)
        todoDao.insertItem(child)
        
        val items = todoDao.getAllItems(userId).first()
        assertEquals(2, items.size)
        val loadedChild = items.find { it.id == 2 }!!
        assertEquals(1, loadedChild.parentId)
    }

    @Test
    fun deleteAll_isolation() = runTest {
        todoDao.insertItem(TodoItem(title = "User A", userId = "A"))
        todoDao.insertItem(TodoItem(title = "User B", userId = "B"))
        
        todoDao.deleteAll("A")
        
        assertTrue(todoDao.getAllItems("A").first().isEmpty())
        assertEquals(1, todoDao.getAllItems("B").first().size)
    }

    @Test
    fun update_position_swapping() = runTest {
        val item1 = TodoItem(id = 1, title = "Task 1", position = 0, userId = userId)
        val item2 = TodoItem(id = 2, title = "Task 2", position = 1, userId = userId)
        
        todoDao.insertItem(item1)
        todoDao.insertItem(item2)
        
        todoDao.updateItem(item1.copy(position = 1))
        todoDao.updateItem(item2.copy(position = 0))
        
        val items = todoDao.getAllItems(userId).first()
        assertEquals("Task 2", items[0].title)
        assertEquals("Task 1", items[1].title)
    }

    @Test
    fun claimUnownedItems_ignoresExistingOwnership() = runTest {
        todoDao.insertItem(TodoItem(id = 1, title = "Unowned", userId = null))
        todoDao.insertItem(TodoItem(id = 2, title = "Other", userId = "other_user"))
        
        todoDao.claimUnownedItems(userId)
        
        val myItems = todoDao.getAllItems(userId).first()
        val otherItems = todoDao.getAllItems("other_user").first()
        
        assertEquals(1, myItems.size)
        assertEquals("Unowned", myItems[0].title)
        assertEquals(1, otherItems.size)
        assertEquals("Other", otherItems[0].title)
    }
}
