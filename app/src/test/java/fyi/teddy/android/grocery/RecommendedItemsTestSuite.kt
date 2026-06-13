package fyi.teddy.android.grocery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.grocery.ui.GroceryPhase
import fyi.teddy.android.grocery.ui.GroceryViewModel
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecommendedItemsTestSuite {

    private lateinit var database: AppDatabase
    private lateinit var groceryDao: GroceryDao
    private lateinit var repository: GroceryRepository
    private lateinit var viewModel: GroceryViewModel
    private val userId = "test_user"

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockkStatic(WorkManager::class)
        val workManager = mockk<WorkManager>(relaxed = true)
        every { WorkManager.getInstance(any()) } returns workManager

        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        groceryDao = database.groceryDao()
        repository = GroceryRepository(groceryDao, ApplicationProvider.getApplicationContext())
        viewModel = GroceryViewModel(repository, userId, ApplicationProvider.getApplicationContext())
    }

    @After
    fun teardown() {
        unmockkStatic(WorkManager::class)
        Dispatchers.resetMain()
        database.close()
    }

    private fun idleLooperAndAdvance() {
        testDispatcher.scheduler.advanceUntilIdle()
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun testOnlyInactiveAndBoughtItemsAreRecommended() = runTest {
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.recommendedItems.collect {} }

        // Given
        val item1 = GroceryItem(id = 1, name = "Eggs", timesBought = 3, isActive = false, userId = userId)
        val item2 = GroceryItem(id = 2, name = "Milk", timesBought = 0, isActive = false, userId = userId)
        val item3 = GroceryItem(id = 3, name = "Bread", timesBought = 5, isActive = true, userId = userId)

        groceryDao.insertItem(item1)
        groceryDao.insertItem(item2)
        groceryDao.insertItem(item3)

        idleLooperAndAdvance()

        // When/Then - Wait for exact emission size
        val recommended = viewModel.recommendedItems.filter { it.size == 1 }.first()
        assertEquals(1, recommended.size)
        assertEquals("Eggs", recommended[0].name)
    }

    @Test
    fun testRecommendedItemsAreSortedByTimesBoughtDescending() = runTest {
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.recommendedItems.collect {} }

        // Given
        val item1 = GroceryItem(id = 1, name = "Eggs", timesBought = 3, isActive = false, userId = userId)
        val item2 = GroceryItem(id = 2, name = "Milk", timesBought = 10, isActive = false, userId = userId)
        val item3 = GroceryItem(id = 3, name = "Bread", timesBought = 1, isActive = false, userId = userId)

        groceryDao.insertItem(item1)
        groceryDao.insertItem(item2)
        groceryDao.insertItem(item3)

        idleLooperAndAdvance()

        // When/Then - Wait for exact emission size
        val recommended = viewModel.recommendedItems.filter { it.size == 3 }.first()
        assertEquals(3, recommended.size)
        assertEquals("Milk", recommended[0].name) // 10 times
        assertEquals("Eggs", recommended[1].name) // 3 times
        assertEquals("Bread", recommended[2].name) // 1 time
    }

    @Test
    fun testCompletingATripConvertsCheckedItemsToRecommendations() = runTest {
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.recommendedItems.collect {} }

        // Given
        val activeBoughtItem = GroceryItem(id = 1, name = "Apples", isBought = true, isActive = true, timesBought = 1, userId = userId)
        val activeUnboughtItem = GroceryItem(id = 2, name = "Bananas", isBought = false, isActive = true, timesBought = 0, userId = userId)

        groceryDao.insertItem(activeBoughtItem)
        groceryDao.insertItem(activeUnboughtItem)

        idleLooperAndAdvance()

        // When completing trip
        viewModel.markDoneForTrip()
        idleLooperAndAdvance()

        // Then apples should become inactive, isBought reset, and timesBought incremented
        val allItems = viewModel.items.filter { list -> list.any { it.id == 1 && !it.isActive } }.first()
        val apple = allItems.find { it.id == 1 }!!
        assertFalse(apple.isActive)
        assertFalse(apple.isBought)
        assertEquals(2, apple.timesBought)

        // Bananas should remain active and unchanged
        val banana = allItems.find { it.id == 2 }!!
        assertTrue(banana.isActive)
        assertFalse(banana.isBought)
        assertEquals(0, banana.timesBought)

        // Apple should now be recommended
        val recommended = viewModel.recommendedItems.filter { it.size == 1 }.first()
        assertEquals(1, recommended.size)
        assertEquals("Apples", recommended[0].name)
    }

    @Test
    fun testAddRecommendedItemsResetsStateToActiveAndUnbought() = runTest {
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.recommendedItems.collect {} }

        // Given
        val recommendedItem = GroceryItem(id = 5, name = "Butter", timesBought = 2, isBought = true, isActive = false, userId = userId)
        groceryDao.insertItem(recommendedItem)

        idleLooperAndAdvance()

        // When adding recommendation back
        viewModel.addRecommendedItems(listOf(5))
        idleLooperAndAdvance()

        // Then butter should become active, unbought, and timesBought preserved
        val allItems = viewModel.items.filter { list -> list.any { it.id == 5 && it.isActive } }.first()
        val butter = allItems.find { it.id == 5 }!!
        assertTrue(butter.isActive)
        assertFalse(butter.isBought)
        assertEquals(2, butter.timesBought)
    }

    @Test
    fun testUserExactFlowCompletingTrip() = runTest {
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.recommendedItems.collect {} }

        // 1) Add a new item, "Coffee".
        viewModel.insertItem("Coffee", "1", null)
        idleLooperAndAdvance()

        // Get inserted item
        val itemsAfterAdd = viewModel.items.filter { it.isNotEmpty() }.first()
        val coffeeItem = itemsAfterAdd.find { it.name == "Coffee" }!!

        // 2) Go to the Shopping tab, and check off the item.
        viewModel.setPhase(GroceryPhase.SHOPPING)
        viewModel.toggleBought(coffeeItem, isChecked = true)
        idleLooperAndAdvance()

        // 3) Tap Complete Trip, and confirm in the modal.
        viewModel.markDoneForTrip()
        idleLooperAndAdvance()

        // 4) Go back to Planning
        viewModel.setPhase(GroceryPhase.PLANNING)
        idleLooperAndAdvance()

        // 5) Verify recommended items lists Coffee
        val recommended = viewModel.recommendedItems.filter { it.isNotEmpty() }.first()
        assertEquals(1, recommended.size)
        assertEquals("Coffee", recommended[0].name)
    }

    @Test
    fun testDialogFilteringExcludesActiveUnboughtButIncludesInactiveCompleted() {
        // Given
        val activeUnboughtItem = GroceryItem(id = 1, name = "Bananas", isBought = false, isActive = true, timesBought = 0, userId = userId)
        val inactiveCompletedItem = GroceryItem(id = 2, name = "Coffee", isBought = false, isActive = false, timesBought = 1, userId = userId)

        val activeItemsList = listOf(activeUnboughtItem, inactiveCompletedItem)
        val recommendedList = listOf(inactiveCompletedItem)

        // When - Dialog filtering
        val unboughtNames = activeItemsList.filter { it.isActive && !it.isBought }.map { it.name }.toSet()
        val availableRecommendations = recommendedList.filter { !unboughtNames.contains(it.name) }

        // Then
        // Bananas should be in unboughtNames because it's active and unbought
        assertTrue(unboughtNames.contains("Bananas"))
        // Coffee should NOT be in unboughtNames because it's inactive (completed)
        assertFalse(unboughtNames.contains("Coffee"))

        // availableRecommendations should contain Coffee
        assertEquals(1, availableRecommendations.size)
        assertEquals("Coffee", availableRecommendations[0].name)
    }
}
