package fyi.teddy.android.grocery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY position ASC, createdAt DESC")
    abstract fun getAllItems(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItem(item: GroceryItem): Long

    @Update
    abstract suspend fun updateItem(item: GroceryItem)

    @Delete
    abstract suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM grocery_items WHERE timesBought > 0 ORDER BY timesBought DESC")
    abstract fun getRecommendedItems(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM stores ORDER BY position ASC, name ASC")
    abstract fun getAllStores(): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStore(store: Store)

    @Update
    abstract suspend fun updateStore(store: Store)

    @Delete
    abstract suspend fun deleteStore(store: Store)

    @Query("SELECT * FROM grocery_item_store_info")
    abstract fun getAllStoreInfo(): Flow<List<GroceryItemStoreInfo>>

    @Query("SELECT * FROM grocery_item_store_info WHERE groceryItemId = :itemId")
    abstract fun getStoreInfoForItem(itemId: Int): Flow<List<GroceryItemStoreInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStoreInfo(info: GroceryItemStoreInfo)

    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    abstract fun getAllCategories(): Flow<List<Category>>

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
}
