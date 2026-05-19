package fyi.teddy.android.grocery.repository

import fyi.teddy.android.grocery.data.*
import kotlinx.coroutines.flow.Flow

class GroceryRepository(private val groceryDao: GroceryDao) {
    fun getAllItems(): Flow<List<GroceryItem>> = groceryDao.getAllItems()
    fun getAllStores(): Flow<List<Store>> = groceryDao.getAllStores()
    fun getAllCategories(): Flow<List<Category>> = groceryDao.getAllCategories()
    fun getAllStoreInfo(): Flow<List<GroceryItemStoreInfo>> = groceryDao.getAllStoreInfo()
    fun getRecommendedItems(): Flow<List<GroceryItem>> = groceryDao.getRecommendedItems()
    
    suspend fun insertItem(item: GroceryItem) = groceryDao.insertItem(item)
    suspend fun updateItem(item: GroceryItem) = groceryDao.updateItem(item)
    suspend fun deleteItem(item: GroceryItem) = groceryDao.deleteItem(item)
    
    suspend fun insertStore(store: Store) = groceryDao.insertStore(store)
    suspend fun updateStore(store: Store) = groceryDao.updateStore(store)
    suspend fun deleteStore(store: Store) = groceryDao.deleteStore(store)
    
    suspend fun insertCategory(category: Category) = groceryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = groceryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = groceryDao.deleteCategoryAndCleanup(category)
    
    suspend fun insertStoreInfo(info: GroceryItemStoreInfo) = groceryDao.insertStoreInfo(info)
}
