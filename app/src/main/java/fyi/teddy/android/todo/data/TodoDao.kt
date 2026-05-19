package fyi.teddy.android.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TodoDao {
    @Query("SELECT * FROM todo_items WHERE (isDaily = 1 OR recurrenceIntervalDays IS NULL OR scheduledAt <= (strftime('%s','now') * 1000 + 60000) OR isCompleted = 1) AND userId = :userId ORDER BY position ASC, createdAt DESC")
    abstract fun getAllItems(userId: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE (isPlannedForToday = 1 OR (dueDate IS NOT NULL AND dueDate <= (strftime('%s','now') * 1000 + 172800000))) AND (isDaily = 1 OR recurrenceIntervalDays IS NULL OR scheduledAt <= (strftime('%s','now') * 1000 + 60000) OR isCompleted = 1) AND userId = :userId ORDER BY (CASE WHEN isPlannedForToday = 1 THEN 0 ELSE 1 END) ASC, position ASC, createdAt DESC")
    abstract fun getTodayItems(userId: String): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItem(item: TodoItem)

    @Update
    abstract suspend fun updateItem(item: TodoItem)

    @Delete
    abstract suspend fun deleteItem(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: String)

    @Query("UPDATE todo_items SET isPlannedForToday = 0 WHERE isCompleted = 0 AND userId = :userId AND isDaily = 0")
    abstract suspend fun resetPlannedItems(userId: String)

    @Query("UPDATE todo_items SET userId = :userId WHERE userId IS NULL")
    abstract suspend fun claimUnownedItems(userId: String)

    @Query("UPDATE todo_items SET isCompleted = 0, isPlannedForToday = 1 WHERE isDaily = 1 AND userId = :userId")
    abstract suspend fun resetDailyItems(userId: String)

    @Transaction
    open suspend fun insertWithNextPosition(item: TodoItem) {
        val maxPos = if (item.parentId == null) {
            getMaxPositionNullParent(item.userId ?: "")
        } else {
            getMaxPosition(item.userId ?: "", item.parentId)
        } ?: -1
        insertItem(item.copy(position = maxPos + 1))
    }

    @Query("SELECT MAX(position) FROM todo_items WHERE userId = :userId AND parentId = :parentId")
    protected abstract suspend fun getMaxPosition(userId: String, parentId: Int): Int?

    @Query("SELECT MAX(position) FROM todo_items WHERE userId = :userId AND parentId IS NULL")
    protected abstract suspend fun getMaxPositionNullParent(userId: String): Int?

    @Transaction
    open suspend fun swapPositions(item1: TodoItem, item2: TodoItem) {
        val pos1 = item1.position
        val pos2 = item2.position
        updateItem(item1.copy(position = pos2))
        updateItem(item2.copy(position = pos1))
    }
}
