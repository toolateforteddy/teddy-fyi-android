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
        coEvery { repository.getScheduledItems(userId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test insertItem with lowercase title capitalizes each word correctly`() = runTest {
        // Given
        val viewModel = TodoViewModel(application, repository, userId)
        val titleInput = "buy some groceries"
        val parentId = 12
        val scheduledDate = "2026-06-03"

        // When
        viewModel.insertItem(titleInput, userId, parentId, scheduledDate)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            repository.insertItem(withArg {
                assert(it.title == "Buy Some Groceries")
                assert(it.userId == userId)
                assert(it.parentId == parentId)
                assert(it.scheduledDate == scheduledDate)
            })
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
        val item = TodoItem(id = 1, title = "Delete Me", userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.deleteItem(item)
        }
    }

    @Test
    fun `test updateItem delegates to repository`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = 1, title = "Update Me", userId = userId)

        viewModel.updateItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(item)
        }
    }

    @Test
    fun `test toggleComplete unchecked updates item immediately`() = runTest {
        val viewModel = TodoViewModel(application, repository, userId)
        val item = TodoItem(id = 1, title = "Task", isCompleted = true, userId = userId)

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
        val item = TodoItem(id = 1, title = "Task", isCompleted = false, userId = userId)

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
        val item = TodoItem(id = 1, title = "Task", isCompleted = false, recurrenceIntervalDays = 5, userId = userId)

        viewModel.toggleComplete(item, isChecked = true)

        testScheduler.advanceTimeBy(2001)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assert(!it.isCompleted)
                assert(it.recurrenceIntervalDays == 5)
                assert(it.scheduledDate == null)
                // Expected rescheduled scheduledAt is base time + 5 days
                assert(it.scheduledAt > System.currentTimeMillis())
            })
        }
    }
}
