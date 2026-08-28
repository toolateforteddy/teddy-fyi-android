package fyi.teddy.android.todo.domain

import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.repository.swapPositions
import kotlinx.coroutines.flow.first

class MoveItemUpUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.siblingsOf(item)
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
        val siblings = allItems.siblingsOf(item)
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
        val siblings = allItems.siblingsOf(item)
        val minPos = siblings.minByOrNull { it.position }?.position ?: 0
        repository.updateItem(item.copy(position = minPos - 1))
    }
}

class MoveItemToBottomUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(item: TodoItem) {
        val allItems = repository.getAllItems(item.userId ?: "").first()
        val siblings = allItems.siblingsOf(item)
        val maxPos = siblings.maxByOrNull { it.position }?.position ?: 0
        repository.updateItem(item.copy(position = maxPos + 1))
    }
}

/**
 * Items that share the moved item's ordering group: same parent *and* same list.
 *
 * Lists ("spaces") are shown one at a time, so treating every same-parent item across all
 * lists as a sibling made a move swap positions with a row the user could not see.
 */
private fun List<TodoItem>.siblingsOf(item: TodoItem): List<TodoItem> =
    filter { it.parentId == item.parentId && it.listId == item.listId }
