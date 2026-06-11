package fyi.teddy.android.grocery

import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.grocery.ui.GroceryPhase
import fyi.teddy.android.grocery.ui.GroceryUiEvent
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
        val viewModel = GroceryViewModel(repository, userId)
        val input = "   whole milk organic   "
        val expected = "Whole Milk Organic"
        assertEquals(expected, viewModel.formatName(input))
    }

    @Test
    fun `test formatName already capitalized is unchanged`() {
        val viewModel = GroceryViewModel(repository, userId)
        val input = "Whole Milk Organic"
        assertEquals(input, viewModel.formatName(input))
    }

    @Test
    fun `test setPhase updates stateflow`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        viewModel.setPhase(GroceryPhase.SHOPPING)
        testScheduler.advanceUntilIdle()
        assertEquals(GroceryPhase.SHOPPING, viewModel.state.value.currentPhase)
    }

    @Test
    fun `test insertItem delegates formatted insert to repository`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        val nameInput = "organic bananas"
        val qtyInput = "3 bunches"
        val categoryId = 4

        viewModel.insertItem(nameInput, qtyInput, categoryId)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertItem(
                withArg {
                    assertEquals("Organic Bananas", it.name)
                    assertEquals("3 bunches", it.quantity)
                    assertEquals(categoryId, it.categoryId)
                    assertEquals(userId, it.userId)
                    assertTrue(it.isActive)
                },
            )
        }
    }

    @Test
    fun `test insertItem empty name is ignored`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        viewModel.insertItem("", "1", null)
        viewModel.insertItem("   ", "1", null)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertItem(any()) }
    }

    @Test
    fun `test toggleBought standard updates item immediately`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
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
        val viewModel = GroceryViewModel(repository, userId)
        val item = GroceryItem(id = 99, name = "Milk", isBought = false, userId = userId)

        viewModel.setPhase(GroceryPhase.SHOPPING)
        viewModel.toggleBought(item, isChecked = true)

        // Run the immediate coroutine that saves to DB and updates state, but don't advance past the 2s delay
        testScheduler.runCurrent()

        // Verify it was marked as bought in the DB immediately
        coVerify(exactly = 1) {
            repository.updateItem(withArg { assertTrue(it.isBought) })
        }

        // Verify it's present in recentlyCheckedIds (which prevents it from disappearing immediately)
        assertTrue(viewModel.state.value.recentlyCheckedIds.contains(99))

        // Advance 2 seconds
        testScheduler.advanceTimeBy(2000)
        testScheduler.runCurrent()

        // Verify it was removed from recentlyCheckedIds, so it moves to In Cart
        assertFalse(viewModel.state.value.recentlyCheckedIds.contains(99))
    }

    @Test
    fun `test markDoneForTrip delegates to repository`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        viewModel.markDoneForTrip()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.markDoneForTrip(userId) }
    }

    @Test
    fun `test insertStore delegates capitalized name`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
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
        val viewModel = GroceryViewModel(repository, userId)
        viewModel.insertCategory("produce section")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertCategory(withArg {
                assertEquals("Produce Section", it.name)
                assertEquals(userId, it.userId)
            })
        }
    }

    @Test
    fun `test onEvent SetPhase updates stateflow`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.SHOPPING))
        testScheduler.advanceUntilIdle()
        assertEquals(GroceryPhase.SHOPPING, viewModel.state.value.currentPhase)
    }

    @Test
    fun `test insertItem with units and notes and listId`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        val nameInput = "organic bananas"
        val qtyInput = "1.5"
        val categoryId = 4
        val unitInput = "lbs"

        // Set listId
        viewModel.onEvent(GroceryUiEvent.SetSelectedListId("test-list-uuid"))
        
        viewModel.insertItem(nameInput, qtyInput, categoryId, unitInput)
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.insertItem(withArg {
                assertEquals("Organic Bananas", it.name)
                assertEquals("1.5", it.quantity)
                assertEquals(categoryId, it.categoryId)
                assertEquals(userId, it.userId)
                assertEquals("lbs", it.unit)
                assertEquals("test-list-uuid", it.listId)
            })
        }
    }

    @Test
    fun `test MoveItemUp and MoveItemDown use cases`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        val item1 = GroceryItem(id = 1, name = "A", position = 0, userId = userId)
        val item2 = GroceryItem(id = 2, name = "B", position = 1, userId = userId)
        val siblings = listOf(item1, item2)

        viewModel.onEvent(GroceryUiEvent.MoveItemUp(item2, siblings))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.swapItemPositions(item2, item1) }

        viewModel.onEvent(GroceryUiEvent.MoveItemDown(item1, siblings))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.swapItemPositions(item1, item2) }
    }

    @Test
    fun `test list management events`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        
        viewModel.onEvent(GroceryUiEvent.InsertList("Costco Trip"))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insertList(any()) }

        val list = GroceryList(id = "list-id-1", name = "Costco Trip", ownerId = userId)
        viewModel.onEvent(GroceryUiEvent.DeleteList(list))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteList(list) }

        viewModel.onEvent(GroceryUiEvent.ShareList("list-id-1", "invited-user"))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insertListMember(any()) }
    }

    @Test
    fun `test state combines all individual flows correctly`() = runTest {
        val viewModel = GroceryViewModel(repository, userId)
        
        viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.PLANNING))
        viewModel.onEvent(GroceryUiEvent.SetEditMode(enabled = true))
        viewModel.onEvent(GroceryUiEvent.SetNewItemName("Apples"))
        viewModel.onEvent(GroceryUiEvent.SetNewItemQuantity("10"))
        viewModel.onEvent(GroceryUiEvent.SetNewItemUnit("pcs"))

        // Allow flows to combine
        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertEquals(GroceryPhase.PLANNING, currentState.currentPhase)
        assertTrue(currentState.isEditMode)
        assertEquals("Apples", currentState.newItemName)
        assertEquals("10", currentState.newItemQuantity)
        assertEquals("pcs", currentState.newItemUnit)
    }
}
