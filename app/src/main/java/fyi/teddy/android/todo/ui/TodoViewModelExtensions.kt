@file:Suppress("unused")
package fyi.teddy.android.todo.ui

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.launch

fun TodoViewModel.moveItemUp(item: TodoItem) {
    viewModelScope.launch { moveItemUpUseCase(item) }
}

fun TodoViewModel.moveItemDown(item: TodoItem) {
    viewModelScope.launch { moveItemDownUseCase(item) }
}

fun TodoViewModel.moveItemToTop(item: TodoItem) {
    viewModelScope.launch { moveItemToTopUseCase(item) }
}

fun TodoViewModel.moveItemToBottom(item: TodoItem) {
    viewModelScope.launch { moveItemToBottomUseCase(item) }
}

fun TodoViewModel.assignIcon(item: TodoItem) {
    viewModelScope.launch {
        val session = fyi.teddy.android.auth.UserSession()
        session.load(getApplication())
        val token = session.idToken
        if (token != null) {
            val iconName = repository.assignIcon(item, token)
            if (iconName != null) {
                Toast.makeText(getApplication(), "Assigned icon: $iconName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Failed to assign icon", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
