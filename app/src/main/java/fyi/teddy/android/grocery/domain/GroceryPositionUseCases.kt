package fyi.teddy.android.grocery.domain

import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.repository.GroceryRepository

class MoveGroceryItemUpUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(item: GroceryItem, siblingItems: List<GroceryItem>) {
        val index = siblingItems.indexOfFirst { it.id == item.id }
        if (index > 0) {
            repository.swapItemPositions(item, siblingItems[index - 1])
        }
    }
}

class MoveGroceryItemDownUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(item: GroceryItem, siblingItems: List<GroceryItem>) {
        val index = siblingItems.indexOfFirst { it.id == item.id }
        if (index != -1 && index < siblingItems.size - 1) {
            repository.swapItemPositions(item, siblingItems[index + 1])
        }
    }
}
