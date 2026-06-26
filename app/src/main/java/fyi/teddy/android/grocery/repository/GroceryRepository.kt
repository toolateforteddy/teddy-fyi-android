package fyi.teddy.android.grocery.repository

import android.content.Context
import fyi.teddy.android.grocery.data.*
import fyi.teddy.android.network.SyncWorker
import kotlinx.coroutines.flow.Flow

class GroceryRepository(
    private val groceryDao: GroceryDao,
    private val context: Context? = null
) {
    private fun scheduleSync() {
        context?.let { SyncWorker.enqueueDebounced(it) }
    }

    fun getAllItems(userId: String): Flow<List<GroceryItem>> = groceryDao.getAllItems(userId)
    fun getAllStores(userId: String): Flow<List<Store>> = groceryDao.getAllStores(userId)
    fun getAllCategories(userId: String): Flow<List<Category>> = groceryDao.getAllCategories(userId)
    fun getAllStoreInfo(userId: String): Flow<List<GroceryItemStoreInfo>> = groceryDao.getAllStoreInfo(userId)
    fun getRecommendedItems(userId: String): Flow<List<GroceryItem>> = groceryDao.getRecommendedItems(userId)
    
    suspend fun insertItem(item: GroceryItem): Long {
        val id = groceryDao.insertItemWithNextPosition(item)
        scheduleSync()
        return id
    }

    suspend fun updateItem(item: GroceryItem) {
        val nextSyncState = if (item.syncState == "SYNCED") "PENDING_UPDATE" else item.syncState
        groceryDao.updateItem(item.copy(syncState = nextSyncState))
        scheduleSync()
    }

    suspend fun deleteItem(item: GroceryItem) {
        if (item.syncState == "PENDING_INSERT") {
            groceryDao.deleteItem(item)
        } else {
            groceryDao.updateItem(item.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }

    suspend fun swapItemPositions(item1: GroceryItem, item2: GroceryItem) {
        groceryDao.swapItemPositions(item1, item2)
        scheduleSync()
    }
    
    suspend fun insertStore(store: Store) {
        groceryDao.insertStoreWithNextPosition(store)
        scheduleSync()
    }

    suspend fun updateStore(store: Store) {
        val nextSyncState = if (store.syncState == "SYNCED") "PENDING_UPDATE" else store.syncState
        groceryDao.updateStore(store.copy(syncState = nextSyncState))
        scheduleSync()
    }

    suspend fun deleteStore(store: Store) {
        if (store.syncState == "PENDING_INSERT") {
            groceryDao.deleteStore(store)
        } else {
            groceryDao.updateStore(store.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }

    suspend fun swapStorePositions(store1: Store, store2: Store) {
        groceryDao.swapStorePositions(store1, store2)
        scheduleSync()
    }
    
    suspend fun insertCategory(category: Category) {
        groceryDao.insertCategoryWithNextPosition(category)
        scheduleSync()
    }

    suspend fun updateCategory(category: Category) {
        val nextSyncState = if (category.syncState == "SYNCED") "PENDING_UPDATE" else category.syncState
        groceryDao.updateCategory(category.copy(syncState = nextSyncState))
        scheduleSync()
    }

    suspend fun deleteCategory(category: Category) {
        if (category.syncState == "PENDING_INSERT") {
            groceryDao.deleteCategoryAndCleanup(category)
        } else {
            groceryDao.updateCategory(category.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }

    suspend fun swapCategoryPositions(cat1: Category, cat2: Category) {
        groceryDao.swapCategoryPositions(cat1, cat2)
        scheduleSync()
    }
    
    suspend fun insertStoreInfo(info: GroceryItemStoreInfo) {
        groceryDao.insertStoreInfo(info)
        scheduleSync()
    }

    suspend fun deleteStoreInfo(info: GroceryItemStoreInfo) {
        groceryDao.deleteStoreInfo(info)
        scheduleSync()
    }
    
    suspend fun claimEverything(userId: String) {
        groceryDao.claimEverything(userId)
        scheduleSync()
    }

    suspend fun ensureDefaultListAndClaimOrphanedItems(userId: String) {
        groceryDao.ensureDefaultListAndClaimOrphanedItems(userId)
        scheduleSync()
    }

    suspend fun markDoneForTrip(userId: String) {
        groceryDao.markDoneForTrip(userId)
        scheduleSync()
    }

    // List & Collaboration operations
    fun getAllLists(userId: String): Flow<List<GroceryList>> = groceryDao.getAllLists(userId)

    suspend fun insertList(list: GroceryList) {
        groceryDao.insertList(list)
        scheduleSync()
    }

    suspend fun updateList(list: GroceryList) {
        val nextSyncState = if (list.syncState == "SYNCED") "PENDING_UPDATE" else list.syncState
        groceryDao.updateList(list.copy(syncState = nextSyncState))
        scheduleSync()
    }

    suspend fun deleteList(list: GroceryList) {
        if (list.syncState == "PENDING_INSERT") {
            groceryDao.deleteList(list)
        } else {
            groceryDao.updateList(list.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }

    suspend fun insertListMember(member: GroceryListMember) {
        groceryDao.insertListMember(member)
        scheduleSync()
    }

    suspend fun deleteListMember(member: GroceryListMember) {
        groceryDao.deleteListMember(member)
        scheduleSync()
    }

    fun getListMembers(listId: String): Flow<List<GroceryListMember>> = groceryDao.getListMembers(listId)
    fun getItemsForList(listId: String): Flow<List<GroceryItem>> = groceryDao.getItemsForList(listId)
    fun getItemsWithoutList(userId: String): Flow<List<GroceryItem>> = groceryDao.getItemsWithoutList(userId)
    fun getUnsyncedCountFlow(): Flow<Int> = groceryDao.getUnsyncedCountFlow()
}
