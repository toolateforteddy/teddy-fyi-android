package fyi.teddy.android.grocery.repository

import fyi.teddy.android.grocery.data.*
import kotlinx.coroutines.flow.Flow

class GroceryRepository(private val groceryDao: GroceryDao) {
    fun getAllItems(): Flow<List<GroceryItem>> = groceryDao.getAllItems()
    fun getAllStores(): Flow<List<Store>> = groceryDao.getAllStores()
    fun getAllCategories(): Flow<List<Category>> = groceryDao.getAllCategories()
    fun getAllStoreInfo(): Flow<List<GroceryItemStoreInfo>> = groceryDao.getAllStoreInfo()
    fun getRecommendedItems(): Flow<List<GroceryItem>> = groceryDao.getRecommendedItems()
    
    suspend fun insertItem(item: GroceryItem) = groceryDao.insertItemWithNextPosition(item)
    suspend fun updateItem(item: GroceryItem) = groceryDao.updateItem(item)
    suspend fun deleteItem(item: GroceryItem) = groceryDao.deleteItem(item)
    suspend fun swapItemPositions(item1: GroceryItem, item2: GroceryItem) = groceryDao.swapItemPositions(item1, item2)
    
    suspend fun insertStore(store: Store) = groceryDao.insertStoreWithNextPosition(store)
    suspend fun updateStore(store: Store) = groceryDao.updateStore(store)
    suspend fun deleteStore(store: Store) = groceryDao.deleteStore(store)
    suspend fun swapStorePositions(store1: Store, store2: Store) = groceryDao.swapStorePositions(store1, store2)
    
    suspend fun insertCategory(category: Category) = groceryDao.insertCategoryWithNextPosition(category)
    suspend fun updateCategory(category: Category) = groceryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = groceryDao.deleteCategoryAndCleanup(category)
    suspend fun swapCategoryPositions(cat1: Category, cat2: Category) = groceryDao.swapCategoryPositions(cat1, cat2)
    
    suspend fun insertStoreInfo(info: GroceryItemStoreInfo) = groceryDao.insertStoreInfo(info)
}
