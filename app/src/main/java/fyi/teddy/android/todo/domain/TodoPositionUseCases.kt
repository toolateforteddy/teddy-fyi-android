package fyi.teddy.android.todo.domain

import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import kotlinx.coroutines.flow.first

class MoveItemUpUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.filter { it.parentId == item.parentId }
            .sortedBy { it.position }
        val index = siblings.indexOfFirst { it.id == item.id }
        if (index > 0) {
            repository.swapPositions(item, siblings[index - 1])
        }
    }
}

class MoveItemDownUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.filter { it.parentId == item.parentId }
            .sortedBy { it.position }
        val index = siblings.indexOfFirst { it.id == item.id }
        if (index != -1 && index < siblings.size - 1) {
            repository.swapPositions(item, siblings[index + 1])
        }
    }
}

class MoveItemToTopUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.filter { it.parentId == item.parentId }
        val minPos = siblings.minByOrNull { it.position }?.position ?: 0
        repository.updateItem(item.copy(position = minPos - 1))
    }
}

class MoveItemToBottomUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.filter { it.parentId == item.parentId }
        val maxPos = siblings.maxByOrNull { it.position }?.position ?: 0
        repository.updateItem(item.copy(position = maxPos + 1))
    }
}
