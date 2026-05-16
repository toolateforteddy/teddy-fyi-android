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
class GroceryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var groceryDao: GroceryDao

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
    fun insertAndGetGroceryItem() = runTest {
        val item = GroceryItem(name = "Apples", quantity = "5")
        groceryDao.insertItem(item)
        
        val allItems = groceryDao.getAllItems().first()
        assertEquals(1, allItems.size)
        assertEquals("Apples", allItems[0].name)
        assertEquals("5", allItems[0].quantity)
    }

    @Test
    fun insertItemWithCategory() = runTest {
        val category = Category(id = 10, name = "Produce")
        groceryDao.insertCategory(category)
        
        val item = GroceryItem(name = "Carrots", categoryId = 10)
        groceryDao.insertItem(item)
        
        val allItems = groceryDao.getAllItems().first()
        assertEquals(1, allItems.size)
        assertEquals(10, allItems[0].categoryId)
    }

    @Test
    fun updateGroceryItem() = runTest {
        val item = GroceryItem(id = 1, name = "Bananas", isBought = false)
        groceryDao.insertItem(item)
        
        val updatedItem = item.copy(isBought = true)
        groceryDao.updateItem(updatedItem)
        
        val allItems = groceryDao.getAllItems().first()
        assertTrue(allItems[0].isBought)
    }

    @Test
    fun deleteGroceryItem() = runTest {
        val item = GroceryItem(id = 1, name = "Bananas")
        groceryDao.insertItem(item)
        groceryDao.deleteItem(item)
        
        val allItems = groceryDao.getAllItems().first()
        assertEquals(0, allItems.size)
    }

    @Test
    fun deleteAllItems() = runTest {
        groceryDao.insertItem(GroceryItem(name = "Item 1"))
        groceryDao.insertItem(GroceryItem(name = "Item 2"))
        
        groceryDao.deleteAll()
        
        val allItems = groceryDao.getAllItems().first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    fun itemsOrdering_positionThenTimestamp() = runTest {
        val item1 = GroceryItem(id = 1, name = "Later but pos 0", position = 0, createdAt = 100)
        val item2 = GroceryItem(id = 2, name = "Earlier but pos 0", position = 0, createdAt = 50)
        val item3 = GroceryItem(id = 3, name = "Pos 1", position = 1, createdAt = 100)
        
        groceryDao.insertItem(item2)
        groceryDao.insertItem(item1)
        groceryDao.insertItem(item3)
        
        val allItems = groceryDao.getAllItems().first()
        assertEquals(1, allItems[0].id) // Pos 0, higher timestamp
        assertEquals(2, allItems[1].id) // Pos 0, lower timestamp
        assertEquals(3, allItems[2].id) // Pos 1
    }

    @Test
    fun insertAndGetAllStores() = runTest {
        val store1 = Store(id = 1, name = "Whole Foods")
        val store2 = Store(id = 2, name = "Trader Joe's")
        groceryDao.insertStore(store1)
        groceryDao.insertStore(store2)
        
        val allStores = groceryDao.getAllStores().first()
        assertEquals(2, allStores.size)
        // Ordered by position then name: Trader Joe's comes first if positions are 0
        assertEquals("Trader Joe's", allStores[0].name)
        assertEquals("Whole Foods", allStores[1].name)
    }

    @Test
    fun updateStore() = runTest {
        val store = Store(id = 1, name = "Original", isDefaultSupported = true)
        groceryDao.insertStore(store)
        
        val updatedStore = store.copy(name = "Updated", isDefaultSupported = false)
        groceryDao.updateStore(updatedStore)
        
        val allStores = groceryDao.getAllStores().first()
        assertEquals("Updated", allStores[0].name)
        assertFalse(allStores[0].isDefaultSupported)
    }

    @Test
    fun deleteStore() = runTest {
        val store = Store(id = 1, name = "To Delete")
        groceryDao.insertStore(store)
        groceryDao.deleteStore(store)
        
        val allStores = groceryDao.getAllStores().first()
        assertTrue(allStores.isEmpty())
    }

    @Test
    fun insertAndGetCategories() = runTest {
        val cat1 = Category(id = 1, name = "Dairy")
        val cat2 = Category(id = 2, name = "Bakery")
        groceryDao.insertCategory(cat1)
        groceryDao.insertCategory(cat2)
        
        val allCats = groceryDao.getAllCategories().first()
        assertEquals(2, allCats.size)
        // Ordered by position then name
        assertEquals("Bakery", allCats[0].name)
        assertEquals("Dairy", allCats[1].name)
    }

    @Test
    fun updateCategory() = runTest {
        val cat = Category(id = 1, name = "Old Name")
        groceryDao.insertCategory(cat)
        
        groceryDao.updateCategory(cat.copy(name = "New Name"))
        
        val allCats = groceryDao.getAllCategories().first()
        assertEquals("New Name", allCats[0].name)
    }

    @Test
    fun storeInfoPersistence() = runTest {
        val item = GroceryItem(id = 1, name = "Milk")
        val store = Store(id = 1, name = "Store")
        groceryDao.insertItem(item)
        groceryDao.insertStore(store)
        
        val info = GroceryItemStoreInfo(groceryItemId = 1, storeId = 1, price = 3.99, isAvailable = true)
        groceryDao.insertStoreInfo(info)
        
        val allInfo = groceryDao.getAllStoreInfo().first()
        assertEquals(1, allInfo.size)
        assertEquals(3.99, allInfo[0].price!!, 0.001)
    }

    @Test
    fun getStoreInfoForItem_filtersCorrectly() = runTest {
        val item1 = GroceryItem(id = 1, name = "Milk")
        val item2 = GroceryItem(id = 2, name = "Bread")
        val store = Store(id = 1, name = "Store")
        groceryDao.insertItem(item1)
        groceryDao.insertItem(item2)
        groceryDao.insertStore(store)
        
        groceryDao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = 1, storeId = 1, price = 1.0))
        groceryDao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = 2, storeId = 1, price = 2.0))
        
        val item1Info = groceryDao.getStoreInfoForItem(1).first()
        assertEquals(1, item1Info.size)
        assertEquals(1.0, item1Info[0].price!!, 0.001)
    }
}
