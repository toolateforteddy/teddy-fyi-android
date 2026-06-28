package fyi.teddy.android.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TodoDao {
    @Query(
        "SELECT * FROM todo_items WHERE (isDaily = 1 OR recurrenceRule IS NULL OR " +
        "scheduledAt <= (strftime('%s','now') * 1000 + 60000) OR isCompleted = 1) " +
        "AND userId = :userId ORDER BY position ASC, createdAt DESC"
    )
    abstract fun getAllItems(userId: String): Flow<List<TodoItem>>

    @Query("""
        SELECT * FROM todo_items WHERE userId = :userId AND (
            scheduledDate = :today 
            OR (dueDate IS NOT NULL AND dueDate <= (strftime('%s','now') * 1000 + 172800000))
            OR (isDaily = 1 OR recurrenceRule IS NULL OR scheduledAt <= (strftime('%s','now') * 1000 + 60000) OR isCompleted = 1)
            OR id IN (SELECT parentId FROM todo_items WHERE scheduledDate = :today AND parentId IS NOT NULL AND userId = :userId)
            OR id IN (SELECT parentId FROM todo_items WHERE id IN (SELECT parentId FROM todo_items WHERE scheduledDate = :today AND parentId IS NOT NULL AND userId = :userId))
            OR parentId IN (SELECT id FROM todo_items WHERE scheduledDate = :today AND userId = :userId)
        )
        ORDER BY (CASE WHEN scheduledDate = :today THEN 0 ELSE 1 END) ASC, position ASC, createdAt DESC
    """)
    abstract fun getTodayItems(userId: String, today: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE userId = :userId AND scheduledDate > :today AND isCompleted = 0 ORDER BY scheduledDate ASC")
    abstract fun getScheduledItems(userId: String, today: String): Flow<List<TodoItem>>

    @Upsert
    abstract suspend fun upsertItem(item: TodoItem)

    @Upsert
    abstract suspend fun upsertList(list: TodoList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItem(item: TodoItem)

    @Update
    abstract suspend fun updateItem(item: TodoItem)

    @Delete
    abstract suspend fun deleteItem(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: String)

    @Query("UPDATE todo_items SET lastScheduledDate = scheduledDate, scheduledDate = NULL WHERE isCompleted = 0 AND userId = :userId AND isDaily = 0 AND (scheduledDate IS NOT NULL AND scheduledDate < :today)")
    abstract suspend fun resetPlannedItems(userId: String, today: String)

    @Query("UPDATE todo_items SET userId = :userId WHERE userId IS NULL")
    abstract suspend fun claimUnownedItems(userId: String)

    @Query("UPDATE todo_items SET isCompleted = 0, scheduledDate = :today WHERE isDaily = 1 AND userId = :userId")
    abstract suspend fun resetDailyItems(userId: String, today: String)

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
    protected abstract suspend fun getMaxPosition(userId: String, parentId: String): Int?

    @Query("SELECT MAX(position) FROM todo_items WHERE userId = :userId AND parentId IS NULL")
    protected abstract suspend fun getMaxPositionNullParent(userId: String): Int?

    @Transaction
    open suspend fun swapPositions(item1: TodoItem, item2: TodoItem) {
        val pos1 = item1.position
        val pos2 = item2.position
        updateItem(item1.copy(position = pos2))
        updateItem(item2.copy(position = pos1))
    }

    @Query("SELECT * FROM todo_lists WHERE userId = :userId AND is_deleted = 0 ORDER BY createdAt ASC")
    abstract fun getAllLists(userId: String): Flow<List<TodoList>>

    @Query("SELECT * FROM todo_lists WHERE id = :id")
    abstract suspend fun getListByIdOneShot(id: String): TodoList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertList(list: TodoList)

    @Update
    abstract suspend fun updateList(list: TodoList)

    @Delete
    abstract suspend fun deleteList(list: TodoList)

    @Transaction
    open suspend fun deleteListAndNullifyItems(list: TodoList) {
        nullifyListIdForItems(list.id)
        deleteList(list)
    }

    @Query("UPDATE todo_items SET listId = NULL WHERE listId = :listId")
    abstract suspend fun nullifyListIdForItems(listId: String)

    @Query("SELECT * FROM todo_items WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedItems(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedItemsFlow(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract suspend fun getUnsyncedLists(): List<TodoList>

    @Query("SELECT * FROM todo_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedListsFlow(): Flow<List<TodoList>>

    @Query("SELECT COUNT(*) FROM todo_items WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedItemsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1")
    abstract fun getUnsyncedListsCountFlow(): Flow<Int>

    @Query("SELECT (SELECT COUNT(*) FROM todo_items WHERE sync_state != 'SYNCED' OR is_deleted = 1) + (SELECT COUNT(*) FROM todo_lists WHERE sync_state != 'SYNCED' OR is_deleted = 1)")
    abstract fun getUnsyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items")
    abstract fun getTodoItemsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_lists")
    abstract fun getTodoListsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<TodoItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLists(lists: List<TodoList>)

    @Query("DELETE FROM todo_items WHERE id = :id")
    abstract suspend fun hardDeleteItem(id: String)

    @Query("DELETE FROM todo_lists WHERE id = :id")
    abstract suspend fun hardDeleteList(id: String)

    @Query("UPDATE todo_items SET scheduledDate = :scheduledDate, sync_state = 'PENDING_UPDATE' WHERE parentId = :parentId AND userId = :userId AND isCompleted = 0 AND (scheduledDate IS NULL OR scheduledDate = '')")
    abstract suspend fun scheduleIncompleteUnscheduledChildren(parentId: String, userId: String, scheduledDate: String)

    @Query("SELECT * FROM todo_items")
    abstract suspend fun getAllItemsOneShot(): List<TodoItem>

    @Query("SELECT * FROM todo_lists")
    abstract suspend fun getAllListsOneShot(): List<TodoList>
}
