package fyi.teddy.android.grocery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroceryDaoExtendedTest {

    private lateinit var database: AppDatabase
    private lateinit var groceryDao: GroceryDao
    private val userId = "test_user"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        groceryDao = database.groceryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getRecommendedItems_sortsByBoughtFrequency() = runTest {
        val item1 = GroceryItem(id = 1, name = "A", timesBought = 5, userId = userId)
        val item2 = GroceryItem(id = 2, name = "B", timesBought = 10, userId = userId)
        val item3 = GroceryItem(id = 3, name = "C", timesBought = 2, userId = userId)
        
        groceryDao.insertItem(item1)
        groceryDao.insertItem(item2)
        groceryDao.insertItem(item3)
        
        val recommended = groceryDao.getRecommendedItems(userId).first()
        assertEquals(3, recommended.size)
        assertEquals("B", recommended[0].name) // Most bought
        assertEquals("A", recommended[1].name)
        assertEquals("C", recommended[2].name) // Least bought
    }

    @Test
    fun getRecommendedItems_onlyIncludesBoughtItems() = runTest {
        val item1 = GroceryItem(id = 1, name = "Bought", timesBought = 1, userId = userId)
        val item2 = GroceryItem(id = 2, name = "Never Bought", timesBought = 0, userId = userId)
        
        groceryDao.insertItem(item1)
        groceryDao.insertItem(item2)
        
        val recommended = groceryDao.getRecommendedItems(userId).first()
        assertEquals(1, recommended.size)
        assertEquals("Bought", recommended[0].name)
    }

    @Test
    fun storeAvailability_filtering() = runTest {
        groceryDao.insertItem(GroceryItem(id = 1, name = "Bread", userId = userId))
        groceryDao.insertStore(Store(id = 1, name = "Store A", userId = userId))
        
        // Item 1 NOT available at Store 1
        groceryDao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = 1, storeId = 1, isAvailable = false, userId = userId))
        
        val info = groceryDao.getStoreInfoForItem(1, userId).first()
        assertFalse(info[0].isAvailable)
    }

    @Test
    fun multipleStores_priceComparison() = runTest {
        groceryDao.insertItem(GroceryItem(id = 1, name = "Milk", userId = userId))
        groceryDao.insertStore(Store(id = 1, name = "Cheap", userId = userId))
        groceryDao.insertStore(Store(id = 2, name = "Expensive", userId = userId))
        
        groceryDao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = 1, storeId = 1, price = 2.0, userId = userId))
        groceryDao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = 1, storeId = 2, price = 5.0, userId = userId))
        
        val infos = groceryDao.getStoreInfoForItem(1, userId).first()
        assertEquals(2, infos.size)
        val minPrice = infos.minBy { it.price!! }
        assertEquals(1, minPrice.storeId)
        assertEquals(2.0, minPrice.price!!, 0.001)
    }

    @Test
    fun storePosition_ordering() = runTest {
        groceryDao.insertStore(Store(id = 1, name = "Z", position = 1, userId = userId))
        groceryDao.insertStore(Store(id = 2, name = "A", position = 0, userId = userId))
        
        val stores = groceryDao.getAllStores(userId).first()
        assertEquals("A", stores[0].name)
        assertEquals("Z", stores[1].name)
    }

    @Test
    fun categoryPosition_ordering() = runTest {
        groceryDao.insertCategory(Category(id = 1, name = "Produce", position = 5, userId = userId))
        groceryDao.insertCategory(Category(id = 2, name = "Dairy", position = 2, userId = userId))
        
        val cats = groceryDao.getAllCategories(userId).first()
        assertEquals("Dairy", cats[0].name)
        assertEquals("Produce", cats[1].name)
    }

    @Test
    fun deleteCategory_retainsItems() = runTest {
        groceryDao.insertCategory(Category(id = 1, name = "Cat", userId = userId))
        groceryDao.insertItem(GroceryItem(id = 1, name = "Item", categoryId = 1, userId = userId))
        
        groceryDao.deleteCategoryAndCleanup(Category(id = 1, name = "Cat", userId = userId))
        
        val items = groceryDao.getAllItems(userId).first()
        assertEquals(1, items.size)
        assertNull(items[0].categoryId) // Still exists but unlinked
    }

    @Test
    fun itemTimesBought_incrementTest() = runTest {
        val item = GroceryItem(id = 1, name = "Eggs", timesBought = 0, userId = userId)
        groceryDao.insertItem(item)
        
        groceryDao.updateItem(item.copy(timesBought = 1, isBought = true))
        
        val loaded = groceryDao.getAllItems(userId).first()
        assertEquals(1, loaded[0].timesBought)
    }

    @Test
    fun defaultStoreAvailability_trueByDefault() = runTest {
        val store = Store(id = 1, name = "New Store", isDefaultSupported = true, userId = userId)
        groceryDao.insertStore(store)
        
        val allStores = groceryDao.getAllStores(userId).first()
        assertTrue(allStores[0].isDefaultSupported)
    }

    @Test
    fun searchSuggestions_prefixMatching() = runTest {
        groceryDao.insertItem(GroceryItem(name = "Peanut Butter", userId = userId))
        groceryDao.insertItem(GroceryItem(name = "Peas", userId = userId))
        groceryDao.insertItem(GroceryItem(name = "Apple", userId = userId))
        
        val all = groceryDao.getAllItems(userId).first()
        val suggestions = all.filter { it.name.startsWith("Pea", ignoreCase = true) }
        assertEquals(2, suggestions.size)
    }
}
