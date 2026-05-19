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
class TodoDaoExtendedTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao
    private val userId = "test_user"

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
    fun getAllItems_filtersFutureTasks() = runTest {
        val now = 10000L
        val pastTask = TodoItem(title = "Past", scheduledAt = 5000, userId = userId)
        val futureTask = TodoItem(title = "Future", scheduledAt = 15000, userId = userId)
        
        todoDao.insertItem(pastTask)
        todoDao.insertItem(futureTask)
        
        val items = todoDao.getAllItems(userId, now).first()
        assertEquals(1, items.size)
        assertEquals("Past", items[0].title)
    }

    @Test
    fun getTodayItems_includesDueSoon() = runTest {
        val now = 10000L
        val twoDaysLater = now + (2 * 24 * 60 * 60 * 1000L)
        
        val planned = TodoItem(title = "Planned", isPlannedForToday = true, userId = userId, scheduledAt = now)
        val dueSoon = TodoItem(title = "Due Soon", dueDate = twoDaysLater, userId = userId, scheduledAt = now)
        val dueLate = TodoItem(title = "Due Late", dueDate = twoDaysLater + 1000, userId = userId, scheduledAt = now)
        
        todoDao.insertItem(planned)
        todoDao.insertItem(dueSoon)
        todoDao.insertItem(dueLate)
        
        val items = todoDao.getTodayItems(userId, now, twoDaysLater).first()
        assertEquals(2, items.size)
        assertTrue(items.any { it.title == "Planned" })
        assertTrue(items.any { it.title == "Due Soon" })
    }

    @Test
    fun getTodayItems_ordering_plannedFirst() = runTest {
        val now = 10000L
        val twoDaysLater = now + (2 * 24 * 60 * 60 * 1000L)
        
        val dueSoon = TodoItem(id = 1, title = "Due Soon", dueDate = twoDaysLater, userId = userId, isPlannedForToday = false, scheduledAt = now)
        val planned = TodoItem(id = 2, title = "Planned", isPlannedForToday = true, userId = userId, scheduledAt = now)
        
        todoDao.insertItem(dueSoon)
        todoDao.insertItem(planned)
        
        val items = todoDao.getTodayItems(userId, now, twoDaysLater).first()
        assertEquals(2, items.size)
        assertEquals("Planned", items[0].title)
        assertEquals("Due Soon", items[1].title)
    }

    @Test
    fun resetPlannedItems_ignoresDaily() = runTest {
        val task1 = TodoItem(id = 1, title = "Normal", isPlannedForToday = true, isDaily = false, userId = userId)
        val task2 = TodoItem(id = 2, title = "Daily", isPlannedForToday = true, isDaily = true, userId = userId)
        
        todoDao.insertItem(task1)
        todoDao.insertItem(task2)
        
        todoDao.resetPlannedItems(userId)
        
        val items = todoDao.getAllItems(userId).first()
        val normal = items.find { it.id == 1 }!!
        val daily = items.find { it.id == 2 }!!
        
        assertFalse(normal.isPlannedForToday)
        assertTrue(daily.isPlannedForToday)
    }

    @Test
    fun resetDailyItems_setsPlannedForToday() = runTest {
        val task = TodoItem(id = 1, title = "Daily", isCompleted = true, isPlannedForToday = false, isDaily = true, userId = userId)
        todoDao.insertItem(task)
        
        todoDao.resetDailyItems(userId)
        
        val items = todoDao.getAllItems(userId).first()
        assertTrue(items[0].isPlannedForToday)
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
    fun recurrence_rescheduling_behavior() = runTest {
        val interval = 5
        val task = TodoItem(id = 1, title = "Mow Lawn", recurrenceIntervalDays = interval, userId = userId)
        todoDao.insertItem(task)
        
        val now = 5000L
        val nextTime = now + (interval * 24 * 60 * 60 * 1000L)
        todoDao.updateItem(task.copy(isCompleted = false, scheduledAt = nextTime, isPlannedForToday = false))
        
        val itemsAtNow = todoDao.getAllItems(userId, now).first()
        assertTrue(itemsAtNow.isEmpty())
        
        val itemsAtFuture = todoDao.getAllItems(userId, nextTime).first()
        assertEquals(1, itemsAtFuture.size)
        assertEquals("Mow Lawn", itemsAtFuture[0].title)
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
