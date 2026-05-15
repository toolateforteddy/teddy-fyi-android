package fyi.teddy.android.grocery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GroceryItem)

    @Update
    suspend fun updateItem(item: GroceryItem)

    @Delete
    suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll()
}
