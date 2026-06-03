package fyi.teddy.android.todo.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.repository.TodoRepository

class TodoViewModelFactory(
    private val application: Application,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = AppDatabase.getDatabase(application)
            val repository = TodoRepository(database.todoDao())
            return TodoViewModel(application, repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
