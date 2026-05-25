package fyi.teddy.android.grocery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GroceryDao {
    @Query("SELECT * FROM grocery_items WHERE userId = :userId ORDER BY position ASC, createdAt DESC")
    abstract fun getAllItems(userId: String): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItem(item: GroceryItem): Long

    @Update
    abstract suspend fun updateItem(item: GroceryItem)

    @Delete
    abstract suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: String)

    @Query("SELECT * FROM grocery_items WHERE timesBought > 0 AND userId = :userId AND isActive = 1 ORDER BY timesBought DESC")
    abstract fun getRecommendedItems(userId: String): Flow<List<GroceryItem>>

    @Query("SELECT * FROM stores WHERE userId = :userId ORDER BY position ASC, name ASC")
    abstract fun getAllStores(userId: String): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStore(store: Store)

    @Update
    abstract suspend fun updateStore(store: Store)

    @Delete
    abstract suspend fun deleteStore(store: Store)

    @Query("""
        SELECT i.* FROM grocery_item_store_info i
        JOIN grocery_items g ON i.groceryItemId = g.id
        WHERE g.userId = :userId
    """)
    abstract fun getAllStoreInfo(userId: String): Flow<List<GroceryItemStoreInfo>>

    @Query("""
        SELECT i.* FROM grocery_item_store_info i
        JOIN grocery_items g ON i.groceryItemId = g.id
        WHERE i.groceryItemId = :itemId AND g.userId = :userId
    """)
    abstract fun getStoreInfoForItem(itemId: Int, userId: String): Flow<List<GroceryItemStoreInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStoreInfo(info: GroceryItemStoreInfo)

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY position ASC, name ASC")
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
    protected abstract suspend fun clearItemCategories(categoryId: Int)

    @Transaction
    open suspend fun insertItemWithNextPosition(item: GroceryItem): Long {
        val maxPos = getMaxItemPosition(item.userId ?: "") ?: -1
        return insertItem(item.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM grocery_items WHERE userId = :userId")
    protected abstract suspend fun getMaxItemPosition(userId: String): Int?

    @Transaction
    open suspend fun insertStoreWithNextPosition(store: Store) {
        val maxPos = getMaxStorePosition(store.userId ?: "") ?: -1
        insertStore(store.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM stores WHERE userId = :userId")
    protected abstract suspend fun getMaxStorePosition(userId: String): Int?

    @Transaction
    open suspend fun insertCategoryWithNextPosition(category: Category) {
        val maxPos = getMaxCategoryPosition(category.userId ?: "") ?: -1
        insertCategory(category.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM categories WHERE userId = :userId")
    protected abstract suspend fun getMaxCategoryPosition(userId: String): Int?

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

    @Query("UPDATE grocery_items SET userId = :userId WHERE userId IS NULL")
    abstract suspend fun claimUnownedItems(userId: String)

    @Query("UPDATE stores SET userId = :userId WHERE userId IS NULL")
    abstract suspend fun claimUnownedStores(userId: String)

    @Query("UPDATE categories SET userId = :userId WHERE userId IS NULL")
    abstract suspend fun claimUnownedCategories(userId: String)

    @Transaction
    open suspend fun claimEverything(userId: String) {
        claimUnownedItems(userId)
        claimUnownedStores(userId)
        claimUnownedCategories(userId)
        claimUnownedStoreInfo(userId)
    }

    @Query("""
        UPDATE grocery_item_store_info 
        SET userId = :userId 
        WHERE userId IS NULL 
        AND groceryItemId IN (SELECT id FROM grocery_items WHERE userId = :userId)
    """)
    abstract suspend fun claimUnownedStoreInfo(userId: String)

    @Query("""
        UPDATE grocery_items 
        SET isBought = 0, isActive = 0, timesBought = timesBought + 1
        WHERE isBought = 1
        AND isActive = 1
        AND userId = :userId
    """)
    abstract suspend fun markDoneForTrip(userId: String)
}
