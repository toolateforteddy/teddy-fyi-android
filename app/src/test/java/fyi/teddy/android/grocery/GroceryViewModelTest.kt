package fyi.teddy.android.grocery

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import fyi.teddy.android.data.SyncLogDao
import fyi.teddy.android.data.UserSyncMetadataDao
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.network.SyncWorker
import fyi.teddy.android.grocery.ui.GroceryPhase
import fyi.teddy.android.grocery.ui.GroceryUiEvent
import fyi.teddy.android.grocery.ui.GroceryViewModel
import fyi.teddy.android.grocery.ui.deleteItem
import fyi.teddy.android.grocery.ui.insertCategory
import fyi.teddy.android.grocery.ui.insertStore
import fyi.teddy.android.grocery.ui.markDoneForTrip
import fyi.teddy.android.grocery.ui.toggleBought
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryViewModelTest {

    private val repository = mockk<GroceryRepository>(relaxed = true)
    private lateinit var application: Application
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val syncLogDao = mockk<SyncLogDao>(relaxed = true)
    private val userSyncMetadataDao = mockk<UserSyncMetadataDao>(relaxed = true)
    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val userId = "test-user-id"

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(testDispatcher)

        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        every { syncLogDao.getLatestLog() } returns flowOf(null)
        
        // Mock shared prefs
        every { sharedPrefs.getString(any(), any()) } returns null
        every { sharedPrefs.edit() } returns mockk(relaxed = true)

        // Mock repository flows to avoid null errors during stateIn init
        coEvery { repository.getAllItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllStores(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllCategories(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllStoreInfo(userId) } returns flowOf(emptyList())
        coEvery { repository.getRecommendedItems(userId) } returns flowOf(emptyList())
        coEvery { repository.getUnsyncedCountFlow() } returns flowOf(0)
        coEvery { repository.getItemsWithoutList(userId) } returns flowOf(emptyList())
        coEvery { repository.getAllLists(userId) } returns flowOf(emptyList())
    }

    private fun createViewModel() = GroceryViewModel(
        repository = repository,
        userId = userId,
        application = application,
        workManager = workManager,
        syncLogDao = syncLogDao,
        userSyncMetadataDao = userSyncMetadataDao,
        prefs = sharedPrefs
    )

    @After
    fun tearDown() {
        // The sync hold is process-wide; leaving one set would leak into the next test.
        SyncWorker.releaseSyncHold(application)
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `test formatName trims and capitalizes word boundaries cleanly`() {
        val viewModel = createViewModel()
        val input = "   whole milk organic   "
        val expected = "Whole Milk Organic"
        assertEquals(expected, viewModel.formatName(input))
    }

    @Test
    fun `test formatName already capitalized is unchanged`() {
        val viewModel = createViewModel()
        val input = "Whole Milk Organic"
        assertEquals(input, viewModel.formatName(input))
    }

    @Test
    fun `test setPhase updates stateflow`() = runTest {
        val viewModel = createViewModel()
        viewModel.setPhase(GroceryPhase.SHOPPING)
        testScheduler.advanceUntilIdle()
        assertEquals(GroceryPhase.SHOPPING, viewModel.state.value.currentPhase)
    }

    @Test
    fun `test insertItem delegates formatted insert to repository`() = runTest {
        val viewModel = createViewModel()
        val nameInput = "organic bananas"
        val qtyInput = "3 bunches"
        val categoryId = "4"

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
        val viewModel = createViewModel()
        viewModel.insertItem("", "1", null)
        viewModel.insertItem("   ", "1", null)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertItem(any()) }
    }

    @Test
    fun `test toggleBought standard updates item immediately`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", isBought = false, userId = userId)

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
        val viewModel = createViewModel()
        val item = GroceryItem(id = "99", name = "Milk", isBought = false, userId = userId)

        viewModel.setPhase(GroceryPhase.SHOPPING)
        viewModel.toggleBought(item, isChecked = true)

        // Run the immediate coroutine that saves to DB and updates state, but don't advance past the 2s delay
        testScheduler.runCurrent()

        // Verify it was marked as bought in the DB immediately
        coVerify(exactly = 1) {
            repository.updateItem(withArg { assertTrue(it.isBought) })
        }

        // Verify it's present in recentlyCheckedIds (which prevents it from disappearing immediately)
        assertTrue(viewModel.state.value.recentlyCheckedIds.contains("99"))

        // Advance 2 seconds
        testScheduler.advanceTimeBy(2000)
        testScheduler.runCurrent()

        // Verify it was removed from recentlyCheckedIds, so it moves to In Cart
        assertFalse(viewModel.state.value.recentlyCheckedIds.contains("99"))
    }

    @Test
    fun `test markDoneForTrip delegates to repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.markDoneForTrip()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.markDoneForTrip(userId, any()) }
    }

    @Test
    fun `test deleteItem marks as inactive if bought before`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", timesBought = 5, userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateItem(withArg {
                assertFalse(it.isActive)
            })
        }
        coVerify(exactly = 0) { repository.deleteItem(any()) }
    }

    @Test
    fun `test deleteItem fully deletes if never bought`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", timesBought = 0, userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteItem(item) }
        coVerify(exactly = 0) { repository.updateItem(any()) }
    }

    @Test
    fun `test deleteItem offers an undo action`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", timesBought = 0, userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        val message = viewModel.state.value.snackbarMessage
        assertNotNull(message)
        assertEquals("Removed Bananas.", message!!.message)
        assertEquals("Undo", message.actionLabel)
        assertEquals(GroceryUiEvent.RestoreItem(item), message.action)
    }

    @Test
    fun `test RestoreItem puts the item back verbatim`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", timesBought = 3, userId = userId)

        viewModel.onEvent(GroceryUiEvent.RestoreItem(item))
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.restoreItem(item) }
    }

    @Test
    fun `test deleteItem holds sync until the undo window closes`() = runTest {
        val viewModel = createViewModel()
        val item = GroceryItem(id = "1", name = "Bananas", timesBought = 0, userId = userId)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()
        assertTrue(SyncWorker.isSyncHeld())

        val messageId = viewModel.state.value.snackbarMessage!!.id
        viewModel.onEvent(GroceryUiEvent.DismissSnackbar(messageId))
        testScheduler.advanceUntilIdle()
        assertFalse(SyncWorker.isSyncHeld())
    }

    @Test
    fun `test insertStore delegates capitalized name`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.SHOPPING))
        testScheduler.advanceUntilIdle()
        assertEquals(GroceryPhase.SHOPPING, viewModel.state.value.currentPhase)
    }

    @Test
    fun `test insertItem with units and notes and listId`() = runTest {
        val viewModel = createViewModel()
        val nameInput = "organic bananas"
        val qtyInput = "1.5"
        val categoryId = "4"
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
        val viewModel = createViewModel()
        val item1 = GroceryItem(id = "1", name = "A", position = 0, userId = userId)
        val item2 = GroceryItem(id = "2", name = "B", position = 1, userId = userId)
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
        val viewModel = createViewModel()
        
        viewModel.onEvent(GroceryUiEvent.InsertList("Costco Trip"))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insertList(any()) }

        val list = GroceryList(id = "list-id-1", name = "Costco Trip", ownerId = userId)
        viewModel.onEvent(GroceryUiEvent.DeleteList(list))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteList(list) }

        viewModel.onEvent(GroceryUiEvent.ShareList("list-id-1", "invited-user"))
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 2) { repository.insertListMember(any()) }
    }

    @Test
    fun `test state combines all individual flows correctly`() = runTest {
        val viewModel = createViewModel()
        
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

    @Test
    fun `test insertItemFromInput with various natural language inputs`() = runTest {
        val viewModel = createViewModel()
        
        val testCases = listOf(
            "Fairlife Milk" to Triple("Fairlife Milk", "1", null),
            "2 Bananas" to Triple("Bananas", "2", null),
            "Two Bananas" to Triple("Bananas", "2", null),
            "2 bunches of Bananas" to Triple("Bananas", "2", "bunches"),
            "10 lbs potatoes" to Triple("Potatoes", "10", "lbs"),
            "Milk" to Triple("Milk", "1", null),
            "one gallon milk" to Triple("Milk", "1", "gallon"),
            "2 fairlife milk" to Triple("Fairlife Milk", "2", null)
        )

        testCases.forEach { (input, expected) ->
            clearMocks(repository)
            // Need to mock getItemsWithoutList for each iteration since we aren't testing reactivation here
            coEvery { repository.getItemsWithoutList(userId) } returns flowOf(emptyList())
            
            viewModel.insertItemFromInput(input)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                repository.insertItem(withArg {
                    assertEquals(expected.first, it.name)
                    assertEquals(expected.second, it.quantity)
                    assertEquals(expected.third, it.unit)
                })
            }
        }
    }

    @Test
    fun `test reactivation of existing item keeps old quantity if not specified`() = runTest {
        val existingItem = GroceryItem(id = "10", name = "Fairlife Milk", quantity = "3", isActive = false, userId = userId)
        
        coEvery { repository.getItemsWithoutList(userId) } returns flowOf(listOf(existingItem))
        val viewModel = createViewModel()
        
        // Start collection to populate items.value
        val job = launch { viewModel.items.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.insertItemFromInput("Fairlife Milk")
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.updateItem(withArg {
                assertEquals("Fairlife Milk", it.name)
                assertEquals("3", it.quantity)
                assertTrue(it.isActive)
            })
        }
        job.cancel()
    }

    @Test
    fun `test reactivation of existing item overwrites quantity if specified`() = runTest {
        val existingItem = GroceryItem(id = "10", name = "Fairlife Milk", quantity = "3", isActive = false, userId = userId)
        
        coEvery { repository.getItemsWithoutList(userId) } returns flowOf(listOf(existingItem))
        val viewModel = createViewModel()
        
        val job = launch { viewModel.items.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.insertItemFromInput("1 Fairlife Milk")
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.updateItem(withArg {
                assertEquals("Fairlife Milk", it.name)
                assertEquals("1", it.quantity)
                assertTrue(it.isActive)
            })
        }
        job.cancel()
    }
}
