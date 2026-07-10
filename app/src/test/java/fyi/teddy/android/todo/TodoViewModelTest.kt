package fyi.teddy.android.todo

import android.app.Application
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.ui.TodoViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<TodoRepository>(relaxed = true)
    private val userId = "test-user-id"

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Flow streams returned by repository to avoid null pointer/errors in stateIn
        coEvery { repository.getAllItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getTodayItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getTodayItems(userId, any()) } returns flowOf(emptyList())
        coEvery { repository.getScheduledItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getScheduledItems(userId, any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test selectedPlanningDate default is today`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val today = java.time.LocalDate.now().toString()
        assert(viewModel.selectedPlanningDate.value == today)
    }

    @Test
    fun `test setSelectedPlanningDate updates selectedPlanningDate flow`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val targetDate = "2026-12-25"
        
        viewModel.setSelectedPlanningDate(targetDate)
        
        assert(viewModel.selectedPlanningDate.value == targetDate)
    }

    @Test
    fun `test insertItem with lowercase title capitalizes each word correctly`() = runTest {
        // Given
        val viewModel = TodoViewModel(application, repository, userId)
        val titleInput = "buy some groceries"
        val parentId = "12"
        val scheduledDate = "2026-06-03"

        // When
        viewModel.insertItem(titleInput, userId, parentId, scheduledDate)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            repository.insertItem(
                withArg {
                    assert(it.title == "Buy Some Groceries")
                    assert(it.userId == userId)
                    assert(it.parentId == parentId)
                    assert(it.scheduledDate == scheduledDate)
                },
            )
        }
    }

    @Test
    fun `test insertItem with already capitalized title does not break`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val titleInput = "Already Capitalized"

        viewModel.insertItem(titleInput, userId, null, null)
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.insertItem(withArg {
                assert(it.title == "Already Capitalized")
            })
        }
    }

    @Test
    fun `test insertItem empty title is ignored`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)

        viewModel.insertItem("", userId, null, null)
        viewModel.insertItem("   ", userId, null, null)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            repository.insertItem(any())
        }
    }

    @Test
    fun `test deleteItem delegates to repository`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = "1", title = "Delete Me", userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.deleteItem(item)
        }
    }

    @Test
    fun `test updateItem delegates to repository`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = "1", title = "Update Me", userId = userId)

        viewModel.updateItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(item)
        }
    }

    @Test
    fun `test toggleComplete unchecked updates item immediately`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = "1", title = "Task", isCompleted = true, userId = userId)

        viewModel.toggleComplete(item, isChecked = false)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assert(!it.isCompleted)
            })
        }
    }

    @Test
    fun `test toggleComplete checked non-recurring delays 2 seconds and updates`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = "1", title = "Task", isCompleted = false, userId = userId)

        viewModel.toggleComplete(item, isChecked = true)

        // Verify that initially it's not marked as completed yet (within 2s delay)
        coVerify(exactly = 0) { repository.updateItem(any()) }

        // Advance 2 seconds (2000ms)
        testScheduler.advanceTimeBy(2001)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assert(it.isCompleted)
            })
        }
    }

    @Test
    fun `test toggleComplete checked recurring task reschedules interval and resets state`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = "1", title = "Task", isCompleted = false, recurrenceRule = "FREQ=DAILY;INTERVAL=5", userId = userId)

        viewModel.toggleComplete(item, isChecked = true)

        testScheduler.advanceTimeBy(2001)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assert(!it.isCompleted)
                assert(it.recurrenceRule == "FREQ=DAILY;INTERVAL=5")
                assert(it.scheduledDate == null)
                // Expected rescheduled scheduledAt is base time + 5 days
                assert(it.scheduledAt > System.currentTimeMillis())
            })
        }
    }

    @Test
    fun `test init loads TODAY tab if there are today items`() = runTest {
        // Given today's items are not empty
        val todayItem = TodoItem(id = "1", title = "Today's Task", userId = userId)
        coEvery { repository.getTodayItems(userId, any()) } returns flowOf(listOf(todayItem))
        
        // When VM is initialized
        val viewModel = TodoViewModel(application, repository, userId)
        testScheduler.advanceUntilIdle()
        
        // Then currentMode should be TODAY
        assert(viewModel.currentMode.value == fyi.teddy.android.todo.ui.TodoMode.TODAY)
    }

    @Test
    fun `test init loads BACKLOG tab if there are no today items`() = runTest {
        // Given today's items are empty
        coEvery { repository.getTodayItems(userId, any()) } returns flowOf(emptyList())
        
        // When VM is initialized
        val viewModel = TodoViewModel(application, repository, userId)
        testScheduler.advanceUntilIdle()
        
        // Then currentMode should be BACKLOG
        assert(viewModel.currentMode.value == fyi.teddy.android.todo.ui.TodoMode.BACKLOG)
    }

    @Test
    fun `test displayedLists filters and counts correctly in TODAY mode`() = runTest {
        val today = java.time.LocalDate.now().toString()
        val list1 = fyi.teddy.android.todo.data.TodoList(id = "list-1", name = "List 1", userId = userId)
        val list2 = fyi.teddy.android.todo.data.TodoList(id = "list-2", name = "List 2", userId = userId)

        val items = listOf(
            // Parent and child both scheduled today -> both should be counted
            TodoItem(id = "p1", title = "P1", listId = "list-1", scheduledDate = today, isCompleted = false),
            TodoItem(id = "c1", title = "C1", listId = "list-1", parentId = "p1", scheduledDate = today, isCompleted = false),
            
            // Parent scheduled today, child NOT scheduled -> both should be counted (child inherits todayness)
            TodoItem(id = "p2", title = "P2", listId = "list-1", scheduledDate = today, isCompleted = false),
            TodoItem(id = "c2", title = "C2", listId = "list-1", parentId = "p2", scheduledDate = null, isCompleted = false),
            
            // Parent NOT scheduled, child scheduled today -> only child should be counted (parent is hidden or just a container)
            // Actually, in Today mode logic, if child is scheduled, parent is shown too.
            TodoItem(id = "p3", title = "P3", listId = "list-2", scheduledDate = null, isCompleted = false),
            TodoItem(id = "c3", title = "C3", listId = "list-2", parentId = "p3", scheduledDate = today, isCompleted = false),
            
            // Item scheduled for tomorrow -> should NOT be counted
            TodoItem(id = "future", title = "Future", listId = "list-2", scheduledDate = "2099-01-01", isCompleted = false)
        )

        coEvery { repository.getAllLists(userId) } returns flowOf(listOf(list1, list2))
        coEvery { repository.getTodayItems(userId, any()) } returns flowOf(items)

        val viewModel = TodoViewModel(application, repository, userId)
        viewModel.setMode(fyi.teddy.android.todo.ui.TodoMode.TODAY)
        testScheduler.advanceUntilIdle()

        val displayed = viewModel.displayedLists.value
        // list-1: P1, C1, P2, C2 = 4
        // list-2: P3, C3 = 2 (p3 is shown because c3 is scheduled)
        assert(displayed.size == 2)
        assert(displayed.find { it.list.id == "list-1" }?.incompleteCount == 4)
        assert(displayed.find { it.list.id == "list-2" }?.incompleteCount == 2)
    }

    @Test
    fun `test displayedLists shows all lists in edit mode`() = runTest {
        val list1 = fyi.teddy.android.todo.data.TodoList(id = "list-1", name = "List 1", userId = userId)
        val list2 = fyi.teddy.android.todo.data.TodoList(id = "list-2", name = "List 2", userId = userId)

        coEvery { repository.getAllLists(userId) } returns flowOf(listOf(list1, list2))
        coEvery { repository.getTodayItems(userId, any()) } returns flowOf(emptyList())

        val viewModel = TodoViewModel(application, repository, userId)
        viewModel.setMode(fyi.teddy.android.todo.ui.TodoMode.TODAY)
        viewModel.setEditMode(true)
        testScheduler.advanceUntilIdle()

        val displayed = viewModel.displayedLists.value
        assert(displayed.size == 2)
    }
}
