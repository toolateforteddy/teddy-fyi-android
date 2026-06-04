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
        val yesterday = LocalDate.now().minusDays(1).toString()
        val task1 = TodoItem(id = 1, title = "Normal", scheduledDate = yesterday, isDaily = false, userId = userId)
        val task2 = TodoItem(id = 2, title = "Daily", scheduledDate = yesterday, isDaily = true, userId = userId)
        
        todoDao.insertItem(task1)
        todoDao.insertItem(task2)
        
        todoDao.resetPlannedItems(userId, today)
        
        val items = todoDao.getAllItems(userId).first()
        val normal = items.find { it.id == 1 }!!
        val daily = items.find { it.id == 2 }!!
        
        assertNull(normal.scheduledDate)
        assertEquals(yesterday, daily.scheduledDate)
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

    @Test
    fun test_resetPlannedItems_pastItemIsReset_futureItemIsPreserved() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        
        val taskPast = TodoItem(id = 1, title = "Past Task", scheduledDate = yesterday, isDaily = false, userId = userId)
        val taskToday = TodoItem(id = 2, title = "Today Task", scheduledDate = today, isDaily = false, userId = userId)
        val taskTomorrow = TodoItem(id = 3, title = "Tomorrow Task", scheduledDate = tomorrow, isDaily = false, userId = userId)
        
        todoDao.insertItem(taskPast)
        todoDao.insertItem(taskToday)
        todoDao.insertItem(taskTomorrow)
        
        todoDao.resetPlannedItems(userId, today)
        
        val items = todoDao.getAllItems(userId).first()
        val pastLoaded = items.find { it.id == 1 }!!
        val todayLoaded = items.find { it.id == 2 }!!
        val tomorrowLoaded = items.find { it.id == 3 }!!
        
        assertNull(pastLoaded.scheduledDate)
        assertEquals(today, todayLoaded.scheduledDate)
        assertEquals(tomorrow, tomorrowLoaded.scheduledDate)
    }

    @Test
    fun test_getTodayItems_reflectsNewDate_onNextDayRollOver() = runTest {
        val taskPlannedForToday = TodoItem(
            id = 1, 
            title = "Planned Today", 
            scheduledDate = today, 
            recurrenceIntervalDays = 1,
            scheduledAt = System.currentTimeMillis() + 100000000,
            userId = userId
        )
        todoDao.insertItem(taskPlannedForToday)
        
        // When queried with yesterday's date, it should not be in getTodayItems
        val yesterday = LocalDate.now().minusDays(1).toString()
        val yesterdayItems = todoDao.getTodayItems(userId, yesterday).first()
        assertFalse(yesterdayItems.any { it.id == 1 })
        
        // When queried with today's date, it should be in getTodayItems
        val todayItemsList = todoDao.getTodayItems(userId, today).first()
        assertTrue(todayItemsList.any { it.id == 1 })
    }

    @Test
    fun test_dailyTask_isIncludedInToday_afterResetDailyItemsForNewDay() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val dailyTask = TodoItem(id = 1, title = "Daily Task", isDaily = true, isCompleted = true, scheduledDate = yesterday, userId = userId)
        todoDao.insertItem(dailyTask)
        
        // Run reset daily items for today's date
        todoDao.resetDailyItems(userId, today)
        
        val todayItemsList = todoDao.getTodayItems(userId, today).first()
        val loadedDaily = todayItemsList.find { it.id == 1 }!!
        
        assertEquals(today, loadedDaily.scheduledDate)
        assertFalse(loadedDaily.isCompleted)
    }

    @Test
    fun test_getScheduledItems_excludesToday_whenDateRollsOverToToday() = runTest {
        val taskScheduledForToday = TodoItem(id = 1, title = "Scheduled", scheduledDate = today, userId = userId)
        todoDao.insertItem(taskScheduledForToday)
        
        // On yesterday, this item is a future scheduled item (scheduledDate > yesterday)
        val yesterday = LocalDate.now().minusDays(1).toString()
        val yesterdayScheduled = todoDao.getScheduledItems(userId, yesterday).first()
        assertTrue(yesterdayScheduled.any { it.id == 1 })
        
        // On today, it is no longer a future scheduled item (since scheduledDate is equal to today, not > today)
        val todayScheduled = todoDao.getScheduledItems(userId, today).first()
        assertFalse(todayScheduled.any { it.id == 1 })
    }

    @Test
    fun test_parentAndChildPlannedForToday_arePreservedAndQueriedCorrectly() = runTest {
        val parent = TodoItem(id = 1, title = "Parent", scheduledDate = today, userId = userId)
        val child = TodoItem(id = 2, title = "Child", parentId = 1, scheduledDate = today, userId = userId)
        todoDao.insertItem(parent)
        todoDao.insertItem(child)
        
        // Reset planned items shouldn't clear today's items
        todoDao.resetPlannedItems(userId, today)
        
        val todayItemsList = todoDao.getTodayItems(userId, today).first()
        val loadedParent = todayItemsList.find { it.id == 1 }!!
        val loadedChild = todayItemsList.find { it.id == 2 }!!
        
        assertEquals(today, loadedParent.scheduledDate)
        assertEquals(today, loadedChild.scheduledDate)
    }
}
