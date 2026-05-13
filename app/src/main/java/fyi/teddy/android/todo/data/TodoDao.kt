package fyi.teddy.android.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TodoItem)

    @Update
    suspend fun updateItem(item: TodoItem)

    @Delete
    suspend fun deleteItem(item: TodoItem)

    @Query("DELETE FROM todo_items")
    suspend fun deleteAll()
}
