package fyi.teddy.android.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

class TodoViewModel(application: Application, userId: String) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TodoRepository(database.todoDao())

    val allItems: StateFlow<List<TodoItem>> = repository.getAllItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayItems: StateFlow<List<TodoItem>> = repository.getTodayItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledItems: StateFlow<List<TodoItem>> = repository.getScheduledItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.claimUnownedItems(userId)
        }
        
        viewModelScope.launch {
            val sharedPref = application.getSharedPreferences("todo_prefs", android.content.Context.MODE_PRIVATE)
            val lastReset = sharedPref.getLong("last_reset_time", 0)
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val midnight = calendar.timeInMillis
            if (now >= midnight && lastReset < midnight) {
                repository.resetPlannedItems(userId)
            }

            calendar.set(Calendar.HOUR_OF_DAY, 8)
            val eightAm = calendar.timeInMillis
            if (now >= eightAm && lastReset < eightAm) {
                repository.resetDailyItems(userId)
            }
            sharedPref.edit().putLong("last_reset_time", now).apply()
        }
    }

    fun insertItem(item: TodoItem) {
        viewModelScope.launch { repository.insertItem(item) }
    }

    fun updateItem(item: TodoItem) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun deleteItem(item: TodoItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun deleteAll(userId: String) {
        viewModelScope.launch { repository.deleteAll(userId) }
    }

    fun swapPositions(item1: TodoItem, item2: TodoItem) {
        viewModelScope.launch { repository.swapPositions(item1, item2) }
    }
}
