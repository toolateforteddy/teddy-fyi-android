package fyi.teddy.android.grocery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GroceryItem): Long

    @Update
    suspend fun updateItem(item: GroceryItem)

    @Delete
    suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store)

    @Delete
    suspend fun deleteStore(store: Store)

    @Query("SELECT * FROM grocery_item_store_info")
    fun getAllStoreInfo(): Flow<List<GroceryItemStoreInfo>>

    @Query("SELECT * FROM grocery_item_store_info WHERE groceryItemId = :itemId")
    fun getStoreInfoForItem(itemId: Int): Flow<List<GroceryItemStoreInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreInfo(info: GroceryItemStoreInfo)
}
