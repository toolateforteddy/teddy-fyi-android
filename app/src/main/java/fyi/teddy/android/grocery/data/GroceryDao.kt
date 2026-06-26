package fyi.teddy.android.grocery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GroceryDao {
    @Query("SELECT * FROM grocery_items WHERE userId = :userId ORDER BY position ASC, createdAt DESC")
    abstract fun getAllItems(userId: String): Flow<List<GroceryItem>>

    @Upsert
    abstract suspend fun upsertItem(item: GroceryItem)

    @Upsert
    abstract suspend fun upsertList(list: GroceryList)

    @Upsert
    abstract suspend fun upsertStore(store: Store)

    @Upsert
    abstract suspend fun upsertCategory(category: Category)

    @Upsert
    abstract suspend fun upsertListMember(member: GroceryListMember)

    @Upsert
    abstract suspend fun upsertStoreInfo(info: GroceryItemStoreInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItem(item: GroceryItem): Long

    @Update
    abstract suspend fun updateItem(item: GroceryItem)

    @Delete
    abstract suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: String)

    @Query("SELECT * FROM grocery_items WHERE timesBought >= 2 AND userId = :userId AND isActive = 0 ORDER BY timesBought DESC")
    abstract fun getRecommendedItems(userId: String): Flow<List<GroceryItem>>

    @Query("""
        SELECT DISTINCT s.* FROM stores s
        LEFT JOIN grocery_list_members m ON s.listId = m.listId
        LEFT JOIN grocery_lists l ON s.listId = l.id
        WHERE s.userId = :userId OR m.userId = :userId OR l.ownerId = :userId
        ORDER BY s.position ASC, s.name ASC
    """)
    abstract fun getAllStores(userId: String): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStore(store: Store)

    @Update
    abstract suspend fun updateStore(store: Store)

    @Delete
    abstract suspend fun deleteStore(store: Store)

    @Query("""
        SELECT DISTINCT i.* FROM grocery_item_store_info i
        JOIN grocery_items g ON i.groceryItemId = g.id
        LEFT JOIN grocery_list_members m ON g.listId = m.listId
        LEFT JOIN grocery_lists l ON g.listId = l.id
        WHERE g.userId = :userId OR m.userId = :userId OR l.ownerId = :userId
    """)
    abstract fun getAllStoreInfo(userId: String): Flow<List<GroceryItemStoreInfo>>

    @Query("""
        SELECT DISTINCT i.* FROM grocery_item_store_info i
        JOIN grocery_items g ON i.groceryItemId = g.id
        LEFT JOIN grocery_list_members m ON g.listId = m.listId
        LEFT JOIN grocery_lists l ON g.listId = l.id
        WHERE i.groceryItemId = :itemId AND (g.userId = :userId OR m.userId = :userId OR l.ownerId = :userId)
    """)
    abstract fun getStoreInfoForItem(itemId: String, userId: String): Flow<List<GroceryItemStoreInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStoreInfo(info: GroceryItemStoreInfo)

    @Delete
    abstract suspend fun deleteStoreInfo(info: GroceryItemStoreInfo)

    @Query("""
        SELECT DISTINCT c.* FROM categories c
        LEFT JOIN grocery_list_members m ON c.listId = m.listId
        LEFT JOIN grocery_lists l ON c.listId = l.id
        WHERE c.userId = :userId OR m.userId = :userId OR l.ownerId = :userId
        ORDER BY c.position ASC, c.name ASC
    """)
    abstract fun getAllCategories(userId: String): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategory(category: Category)

    @Update
    abstract suspend fun updateCategory(category: Category)

    @Transaction
    open suspend fun deleteCategoryAndCleanup(category: Category) {
        clearItemCategories(category.id)
        deleteCategory(category)
    }

    @Delete
    protected abstract suspend fun deleteCategory(category: Category)

    @Query("UPDATE grocery_items SET categoryId = NULL WHERE categoryId = :categoryId")
    protected abstract suspend fun clearItemCategories(categoryId: String)

    @Transaction
    open suspend fun insertItemWithNextPosition(item: GroceryItem): Long {
        val maxPos = getMaxItemPosition(item.userId ?: "", item.listId) ?: -1
        return insertItem(item.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM grocery_items WHERE (userId = :userId AND listId IS NULL) OR (listId = :listId AND listId IS NOT NULL)")
    protected abstract suspend fun getMaxItemPosition(userId: String, listId: String?): Int?

    @Transaction
    open suspend fun insertStoreWithNextPosition(store: Store) {
        val maxPos = getMaxStorePosition(store.userId ?: "", store.listId) ?: -1
        insertStore(store.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM stores WHERE (userId = :userId AND listId IS NULL) OR (listId = :listId AND listId IS NOT NULL)")
    protected abstract suspend fun getMaxStorePosition(userId: String, listId: String?): Int?

    @Transaction
    open suspend fun insertCategoryWithNextPosition(category: Category) {
        val maxPos = getMaxCategoryPosition(category.userId ?: "", category.listId) ?: -1
        insertCategory(category.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM categories WHERE (userId = :userId AND listId IS NULL) OR (listId = :listId AND listId IS NOT NULL)")
    protected abstract suspend fun getMaxCategoryPosition(userId: String, listId: String?): Int?

    @Transaction
    open suspend fun swapItemPositions(item1: GroceryItem, item2: GroceryItem) {
        val pos1 = item1.position
        val pos2 = item2.position
        updateItem(item1.copy(position = pos2))
        updateItem(item2.copy(position = pos1))
    }

    @Transaction
    open suspend fun swapStorePositions(store1: Store, store2: Store) {
        val pos1 = store1.position
        val pos2 = store2.position
        updateStore(store1.copy(position = pos2))
        updateStore(store2.copy(position = pos1))
    }

    @Transaction
    open suspend fun swapCategoryPositions(cat1: Category, cat2: Category) {
        val pos1 = cat1.position
        val pos2 = cat2.position
        updateCategory(cat1.copy(position = pos2))
        updateCategory(cat2.copy(position = pos1))
    }

    @Query("UPDATE grocery_lists SET ownerId = :userId WHERE ownerId IS NULL OR ownerId = 'unauthed'")
    abstract suspend fun claimUnownedLists(userId: String)

    @Query("UPDATE grocery_items SET userId = :userId WHERE userId IS NULL OR userId = 'unauthed'")
    abstract suspend fun claimUnownedItems(userId: String)

    @Query("UPDATE stores SET userId = :userId WHERE userId IS NULL OR userId = 'unauthed'")
    abstract suspend fun claimUnownedStores(userId: String)

    @Query("UPDATE categories SET userId = :userId WHERE userId IS NULL OR userId = 'unauthed'")
    abstract suspend fun claimUnownedCategories(userId: String)

    @Query("UPDATE grocery_list_members SET userId = :userId WHERE userId = 'unauthed'")
    abstract suspend fun claimUnownedMembers(userId: String)

    @Transaction
    open suspend fun claimEverything(userId: String) {
        claimUnownedLists(userId)
        claimUnownedItems(userId)
        claimUnownedStores(userId)
        claimUnownedCategories(userId)
        claimUnownedStoreInfo(userId)
        claimUnownedMembers(userId)
    }

    @Query("""
        UPDATE grocery_item_store_info 
        SET userId = :userId 
        WHERE (userId IS NULL OR userId = 'unauthed')
        AND groceryItemId IN (SELECT id FROM grocery_items WHERE userId = :userId)
    """)
    abstract suspend fun claimUnownedStoreInfo(userId: String)

    @Query("""
        UPDATE grocery_items 
        SET isBought = 0, 
            isActive = 0, 
            timesBought = timesBought + 1,
            sync_state = CASE WHEN sync_state = 'SYNCED' THEN 'PENDING_UPDATE' ELSE sync_state END
        WHERE isBought = 1
        AND isActive = 1
        AND (
            (listId = :listId AND :listId IS NOT NULL) 
            OR (listId IS NULL AND :listId IS NULL AND userId = :userId)
        )
    """)
    abstract suspend fun markDoneForTrip(userId: String, listId: String?)

    @Query("""
        SELECT DISTINCT l.* FROM grocery_lists l
        LEFT JOIN grocery_list_members m ON l.id = m.listId
        WHERE l.ownerId = :userId OR m.userId = :userId
        ORDER BY l.createdAt ASC
    """)
    abstract fun getAllLists(userId: String): Flow<List<GroceryList>>

    @Query("SELECT * FROM grocery_lists WHERE id = :id")
    abstract suspend fun getListByIdOneShot(id: String): GroceryList?

    @Query("SELECT * FROM grocery_items WHERE id = :id")
    abstract suspend fun getItemByIdOneShot(id: String): GroceryItem?

    @Query("SELECT * FROM stores WHERE id = :id")
    abstract suspend fun getStoreByIdOneShot(id: String): Store?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertList(list: GroceryList)

    @Update
    abstract suspend fun updateList(list: GroceryList)

    @Delete
    abstract suspend fun deleteList(list: GroceryList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListMember(member: GroceryListMember)

    @Delete
    abstract suspend fun deleteListMember(member: GroceryListMember)

    @Query("SELECT * FROM grocery_list_members WHERE listId = :listId")
    abstract fun getListMembers(listId: String): Flow<List<GroceryListMember>>

    @Query("SELECT * FROM grocery_items WHERE listId = :listId ORDER BY position ASC, createdAt DESC")
    abstract fun getItemsForList(listId: String): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE listId IS NULL AND userId = :userId ORDER BY position ASC, createdAt DESC")
    abstract fun getItemsWithoutList(userId: String): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedItems(): List<GroceryItem>

    @Query("SELECT * FROM grocery_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedLists(): List<GroceryList>

    @Query("SELECT * FROM stores WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedStores(): List<Store>

    @Query("SELECT * FROM categories WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedCategories(): List<Category>

    @Query("DELETE FROM grocery_items WHERE id = :id")
    abstract suspend fun hardDeleteItem(id: String)

    @Query("DELETE FROM grocery_lists WHERE id = :id")
    abstract suspend fun hardDeleteList(id: String)

    @Query("DELETE FROM stores WHERE id = :id")
    abstract suspend fun hardDeleteStore(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun hardDeleteCategory(id: String)

    @Query("DELETE FROM grocery_list_members WHERE id = :id")
    abstract suspend fun hardDeleteListMember(id: String)

    @Query("DELETE FROM grocery_item_store_info WHERE groceryItemId = :groceryItemId AND storeId = :storeId")
    abstract suspend fun hardDeleteStoreInfo(groceryItemId: String, storeId: String)

    @Query("SELECT * FROM grocery_items")
    abstract suspend fun getAllItemsOneShot(): List<GroceryItem>

    @Query("SELECT * FROM grocery_lists")
    abstract suspend fun getAllListsOneShot(): List<GroceryList>

    @Query("SELECT * FROM stores")
    abstract suspend fun getAllStoresOneShot(): List<Store>

    @Query("SELECT * FROM categories")
    abstract suspend fun getAllCategoriesOneShot(): List<Category>

    @Query("SELECT * FROM grocery_list_members")
    abstract suspend fun getAllListMembersOneShot(): List<GroceryListMember>

    @Query("SELECT * FROM grocery_item_store_info")
    abstract suspend fun getAllStoreInfosOneShot(): List<GroceryItemStoreInfo>

    @Query("SELECT * FROM grocery_list_members WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedListMembers(): List<GroceryListMember>

    @Query("SELECT * FROM grocery_item_store_info WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedStoreInfos(): List<GroceryItemStoreInfo>

    @Query("SELECT COUNT(*) FROM grocery_lists WHERE (ownerId = :userId OR ownerId IS NULL) AND is_deleted = 0")
    abstract suspend fun getGroceryListsCountOneShot(userId: String): Int

    @Query("SELECT * FROM grocery_lists WHERE (ownerId = :userId OR ownerId IS NULL) AND name = :name AND is_deleted = 0 LIMIT 1")
    abstract suspend fun getListByNameOneShot(userId: String, name: String): GroceryList?

    @Query("SELECT EXISTS(SELECT 1 FROM grocery_items WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0)")
    abstract suspend fun hasOrphanedItems(userId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM stores WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0)")
    abstract suspend fun hasOrphanedStores(userId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0)")
    abstract suspend fun hasOrphanedCategories(userId: String): Boolean

    @Query("UPDATE grocery_items SET listId = :listId, sync_state = 'PENDING_UPDATE' WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0")
    abstract suspend fun moveOrphanedItemsToList(userId: String, listId: String)

    @Query("UPDATE stores SET listId = :listId, sync_state = 'PENDING_UPDATE' WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0")
    abstract suspend fun moveOrphanedStoresToList(userId: String, listId: String)

    @Query("UPDATE categories SET listId = :listId, sync_state = 'PENDING_UPDATE' WHERE (userId = :userId OR userId IS NULL) AND listId IS NULL AND is_deleted = 0")
    abstract suspend fun moveOrphanedCategoriesToList(userId: String, listId: String)

    @Transaction
    open suspend fun ensureDefaultListAndClaimOrphanedItems(userId: String) {
        val listCount = getGroceryListsCountOneShot(userId)
        val orphanedItems = hasOrphanedItems(userId)
        val orphanedStores = hasOrphanedStores(userId)
        val orphanedCategories = hasOrphanedCategories(userId)

        if (listCount == 0 || orphanedItems || orphanedStores || orphanedCategories) {
            var defaultList = getListByNameOneShot(userId, "My List")
            val defaultListId = if (defaultList == null) {
                val newList = GroceryList(name = "My List", ownerId = userId)
                insertList(newList)
                newList.id
            } else {
                defaultList.id
            }

            if (orphanedItems) moveOrphanedItemsToList(userId, defaultListId)
            if (orphanedStores) moveOrphanedStoresToList(userId, defaultListId)
            if (orphanedCategories) moveOrphanedCategoriesToList(userId, defaultListId)
            
            // Re-claim everything to be sure IDs match
            claimEverything(userId)
        }
    }

    @Query("SELECT COUNT(*) FROM grocery_items")

    abstract fun getGroceryItemsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_lists")
    abstract fun getGroceryListsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stores")
    abstract fun getStoresCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM categories")
    abstract fun getCategoriesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_item_store_info")
    abstract fun getStoreInfosCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_items WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedItemsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedListsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stores WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedStoresCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM categories WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedCategoriesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_list_members WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedMembersCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_item_store_info WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedStoreInfosCountFlow(): Flow<Int>

    @Query("""
        SELECT (SELECT COUNT(*) FROM grocery_items WHERE sync_state != 'SYNCED' OR is_deleted = 1) +
               (SELECT COUNT(*) FROM grocery_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1) +
               (SELECT COUNT(*) FROM stores WHERE sync_state != 'SYNCED' OR is_deleted = 1) +
               (SELECT COUNT(*) FROM categories WHERE sync_state != 'SYNCED' OR is_deleted = 1) +
               (SELECT COUNT(*) FROM grocery_list_members WHERE sync_state != 'SYNCED' OR is_deleted = 1) +
               (SELECT COUNT(*) FROM grocery_item_store_info WHERE sync_state != 'SYNCED' OR is_deleted = 1)
    """)
    abstract fun getUnsyncedCountFlow(): Flow<Int>
}
