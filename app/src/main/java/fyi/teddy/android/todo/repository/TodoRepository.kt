package fyi.teddy.android.todo.repository

import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    fun getAllItems(userId: String): Flow<List<TodoItem>> = todoDao.getAllItems(userId)
    fun getTodayItems(userId: String): Flow<List<TodoItem>> = todoDao.getTodayItems(userId)
    
    suspend fun insertItem(item: TodoItem) = todoDao.insertWithNextPosition(item)
    suspend fun updateItem(item: TodoItem) = todoDao.updateItem(item)
    suspend fun deleteItem(item: TodoItem) = todoDao.deleteItem(item)
    suspend fun deleteAll(userId: String) = todoDao.deleteAll(userId)
    suspend fun resetPlannedItems(userId: String) = todoDao.resetPlannedItems(userId)
    suspend fun claimUnownedItems(userId: String) = todoDao.claimUnownedItems(userId)
    suspend fun resetDailyItems(userId: String) = todoDao.resetDailyItems(userId)
    suspend fun swapPositions(item1: TodoItem, item2: TodoItem) = todoDao.swapPositions(item1, item2)
}
