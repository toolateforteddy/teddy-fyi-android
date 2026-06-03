package fyi.teddy.android.grocery

import android.app.Application
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.grocery.ui.GroceryPhase
import fyi.teddy.android.grocery.ui.GroceryViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroceryViewModelTest {

    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<GroceryRepository>(relaxed = true)
    private val userId = "test-user-id"

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock repository flows to avoid null errors during stateIn init
        coEvery { repository.getAllItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllStores(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllCategories(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllStoreInfo(userId) } returns flowOf(emptyList())
        coEvery { repository.getRecommendedItems(userId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test formatName trims and capitalizes word boundaries cleanly`() {
        val viewModel = GroceryViewModel(application, repository, userId)
        val input = "   whole milk organic   "
        val expected = "Whole Milk Organic"
        assertEquals(expected, viewModel.formatName(input))
    }

    @Test
    fun `test formatName already capitalized is unchanged`() {
        val viewModel = GroceryViewModel(application, repository, userId)
        val input = "Whole Milk Organic"
        assertEquals(input, viewModel.formatName(input))
    }

    @Test
    fun `test setPhase updates stateflow`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        viewModel.setPhase(GroceryPhase.SHOPPING)
        assertEquals(GroceryPhase.SHOPPING, viewModel.currentPhase.value)
    }

    @Test
    fun `test insertItem delegates formatted insert to repository`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        val nameInput = "organic bananas"
        val qtyInput = "3 bunches"
        val categoryId = 4

        viewModel.insertItem(nameInput, qtyInput, categoryId)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertItem(withArg {
                assertEquals("Organic Bananas", it.name)
                assertEquals("3 bunches", it.quantity)
                assertEquals(categoryId, it.categoryId)
                assertEquals(userId, it.userId)
                assertTrue(it.isActive)
            })
        }
    }

    @Test
    fun `test insertItem empty name is ignored`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        viewModel.insertItem("", "1", null)
        viewModel.insertItem("   ", "1", null)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertItem(any()) }
    }

    @Test
    fun `test toggleBought standard updates item immediately`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        val item = GroceryItem(id = 1, name = "Bananas", isBought = false, userId = userId)

        viewModel.toggleBought(item, isChecked = true)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assertTrue(it.isBought)
            })
        }
    }

    @Test
    fun `test toggleBought shopping phase checking item starts 2-second in-cart delay`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        val item = GroceryItem(id = 99, name = "Milk", isBought = false, userId = userId)

        viewModel.setPhase(GroceryPhase.SHOPPING)
        viewModel.toggleBought(item, isChecked = true)

        // Run the immediate coroutine that saves to DB
        testScheduler.runCurrent()

        // Verify it was marked as bought in the DB immediately
        coVerify(exactly = 1) {
            repository.updateItem(withArg { assertTrue(it.isBought) })
        }

        // Verify it's present in recentlyCheckedIds (which prevents it from disappearing immediately)
        assertTrue(viewModel.recentlyCheckedIds.value.contains(99))

        // Advance 2 seconds
        testScheduler.advanceTimeBy(2000)
        testScheduler.runCurrent()

        // Verify it was removed from recentlyCheckedIds, so it moves to In Cart
        assertFalse(viewModel.recentlyCheckedIds.value.contains(99))
    }

    @Test
    fun `test markDoneForTrip delegates to repository`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        viewModel.markDoneForTrip()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.markDoneForTrip(userId) }
    }

    @Test
    fun `test insertStore delegates capitalized name`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        viewModel.insertStore("trader joe's")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertStore(withArg {
                assertEquals("Trader Joe's", it.name)
                assertEquals(userId, it.userId)
            })
        }
    }

    @Test
    fun `test insertCategory delegates capitalized name`() = runTest {
        val viewModel = GroceryViewModel(application, repository, userId)
        viewModel.insertCategory("produce section")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertCategory(withArg {
                assertEquals("Produce Section", it.name)
                assertEquals(userId, it.userId)
            })
        }
    }
}
