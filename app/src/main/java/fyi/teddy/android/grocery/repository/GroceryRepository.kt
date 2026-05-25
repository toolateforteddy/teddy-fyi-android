package fyi.teddy.android.grocery.repository

import fyi.teddy.android.grocery.data.*
import kotlinx.coroutines.flow.Flow

class GroceryRepository(private val groceryDao: GroceryDao) {
    fun getAllItems(userId: String): Flow<List<GroceryItem>> = groceryDao.getAllItems(userId)
    fun getAllStores(userId: String): Flow<List<Store>> = groceryDao.getAllStores(userId)
    fun getAllCategories(userId: String): Flow<List<Category>> = groceryDao.getAllCategories(userId)
    fun getAllStoreInfo(userId: String): Flow<List<GroceryItemStoreInfo>> = groceryDao.getAllStoreInfo(userId)
    fun getRecommendedItems(userId: String): Flow<List<GroceryItem>> = groceryDao.getRecommendedItems(userId)
    
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
    
    suspend fun claimEverything(userId: String) = groceryDao.claimEverything(userId)
    
    suspend fun markDoneForTrip() = groceryDao.markDoneForTrip()
}
