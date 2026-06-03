package fyi.teddy.android.todo.repository

import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.util.TaskSchedulerUtils
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer for managing [TodoItem] data operations.
 * Acts as the clean API boundary between the domain view models and Room database DAOs,
 * facilitating future local-first synchronization engine integrations.
 */
class TodoRepository(private val todoDao: TodoDao) {

    /**
     * Retrieves all todo items for a specific user, sorted by position and creation date.
     */
    fun getAllItems(userId: String): Flow<List<TodoItem>> = todoDao.getAllItems(userId)

    /**
     * Retrieves all todo items active or scheduled for today, utilizing modernized date formats.
     */
    fun getTodayItems(userId: String): Flow<List<TodoItem>> {
        val today = TaskSchedulerUtils.getTodayDateString()
        return todoDao.getTodayItems(userId, today)
    }

    /**
     * Retrieves all future scheduled todo items starting after today's date.
     */
    fun getScheduledItems(userId: String): Flow<List<TodoItem>> {
        val today = TaskSchedulerUtils.getTodayDateString()
        return todoDao.getScheduledItems(userId, today)
    }

    /**
     * Inserts a new todo item at the next position sequence for the user.
     */
    suspend fun insertItem(item: TodoItem) = todoDao.insertWithNextPosition(item)

    /**
     * Updates an existing todo item's details or completion state.
     */
    suspend fun updateItem(item: TodoItem) = todoDao.updateItem(item)

    /**
     * Deletes a todo item from the local persistence layer.
     */
    suspend fun deleteItem(item: TodoItem) = todoDao.deleteItem(item)

    /**
     * Deletes all todo items belonging to a specific user.
     */
    suspend fun deleteAll(userId: String) = todoDao.deleteAll(userId)

    /**
     * Resets any non-daily planned items for the user.
     */
    suspend fun resetPlannedItems(userId: String) = todoDao.resetPlannedItems(userId)

    /**
     * Automatically claims unowned items for the currently logged-in user.
     */
    suspend fun claimUnownedItems(userId: String) = todoDao.claimUnownedItems(userId)

    /**
     * Resets state of recurring daily items back to uncompleted for today's date.
     */
    suspend fun resetDailyItems(userId: String) {
        val today = TaskSchedulerUtils.getTodayDateString()
        todoDao.resetDailyItems(userId, today)
    }

    /**
     * Swaps display positions of two todo items to facilitate drag/drop or ordering.
     */
    suspend fun swapPositions(item1: TodoItem, item2: TodoItem) = todoDao.swapPositions(item1, item2)
}
