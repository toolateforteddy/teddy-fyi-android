package fyi.teddy.android.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE (isDaily = 1 OR recurrenceIntervalDays IS NULL OR scheduledAt <= :now OR isCompleted = 1) AND userId = :userId ORDER BY position ASC, createdAt DESC")
    fun getAllItems(userId: String, now: Long = System.currentTimeMillis()): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE isPlannedForToday = 1 AND (isDaily = 1 OR recurrenceIntervalDays IS NULL OR scheduledAt <= :now OR isCompleted = 1) AND userId = :userId ORDER BY position ASC, createdAt DESC")
    fun getTodayItems(userId: String, now: Long = System.currentTimeMillis()): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TodoItem)

    @Update
    suspend fun updateItem(item: TodoItem)

    @Delete
    suspend fun deleteItem(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("UPDATE todo_items SET isPlannedForToday = 0 WHERE isCompleted = 0 AND userId = :userId AND isDaily = 0")
    suspend fun resetPlannedItems(userId: String)

    @Query("UPDATE todo_items SET userId = :userId WHERE userId IS NULL")
    suspend fun claimUnownedItems(userId: String)

    @Query("UPDATE todo_items SET isCompleted = 0, isPlannedForToday = 1 WHERE isDaily = 1 AND userId = :userId")
    suspend fun resetDailyItems(userId: String)
}
