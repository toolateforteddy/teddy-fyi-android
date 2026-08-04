@file:Suppress("unused", "TooManyFunctions")
package fyi.teddy.android.grocery.ui

import androidx.lifecycle.viewModelScope
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.network.GroceryNetworkRepository
import fyi.teddy.android.network.SyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds



fun GroceryViewModel.swapStorePositions(store1: Store, store2: Store) {
    viewModelScope.launch { repository.swapStorePositions(store1, store2) }
}

fun GroceryViewModel.swapCategoryPositions(cat1: Category, cat2: Category) {
    viewModelScope.launch { repository.swapCategoryPositions(cat1, cat2) }
}

fun GroceryViewModel.insertStore(name: String) {
    if (name.isNotBlank()) {
        val capitalized = formatName(name)
        viewModelScope.launch {
            repository.insertStore(
                Store(
                    name = capitalized, 
                    userId = userId,
                    listId = _selectedListId.value
                )
            )
        }
    }
}

fun GroceryViewModel.deleteStore(store: Store) {
    viewModelScope.launch { repository.deleteStore(store) }
}

fun GroceryViewModel.updateStore(store: Store) {
    viewModelScope.launch { repository.updateStore(store) }
}

fun GroceryViewModel.insertCategory(name: String) {
    if (name.isNotBlank()) {
        val capitalized = formatName(name)
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    name = capitalized, 
                    userId = userId,
                    listId = _selectedListId.value
                )
            )
        }
    }
}

fun GroceryViewModel.updateCategory(category: Category) {
    viewModelScope.launch { repository.updateCategory(category) }
}

fun GroceryViewModel.deleteCategory(category: Category) {
    viewModelScope.launch { repository.deleteCategory(category) }
}

fun GroceryViewModel.insertList(name: String) {
    if (name.isNotBlank()) {
        val capitalized = formatName(name)
        viewModelScope.launch {
            val newList = GroceryList(
                name = capitalized,
                ownerId = userId
            )
            repository.insertList(newList)
            repository.insertListMember(
                GroceryListMember(
                    listId = newList.id,
                    userId = userId,
                    role = "ADMIN"
                )
            )
            setSelectedListId(newList.id)
        }
    }
}

fun GroceryViewModel.deleteList(list: GroceryList) {
    viewModelScope.launch {
        repository.deleteList(list)
        if (_selectedListId.value == list.id) {
            setSelectedListId(null)
        }
    }
}

fun GroceryViewModel.updateList(list: GroceryList) {
    viewModelScope.launch {
        repository.updateList(list)
    }
}

fun GroceryViewModel.reorderLists(lists: List<GroceryList>) {
    viewModelScope.launch {
        repository.updateListPositions(lists)
    }
}

fun GroceryViewModel.updateItem(item: GroceryItem) {
    viewModelScope.launch { repository.updateItem(item) }
}

fun GroceryViewModel.deleteItem(item: GroceryItem) {
    viewModelScope.launch {
        if (item.timesBought > 0) {
            repository.updateItem(item.copy(isActive = false))
        } else {
            repository.deleteItem(item)
        }
    }
}

fun GroceryViewModel.updateStoreInfo(info: GroceryItemStoreInfo) {
    viewModelScope.launch { repository.insertStoreInfo(info.copy(userId = userId)) }
}

fun GroceryViewModel.deleteStoreInfo(info: GroceryItemStoreInfo) {
    viewModelScope.launch { repository.deleteStoreInfo(info) }
}

fun GroceryViewModel.toggleBought(item: GroceryItem, isChecked: Boolean) {
    val updatedItem = item.copy(isBought = isChecked)
    if (isChecked && _currentPhase.value == GroceryPhase.SHOPPING) {
        _recentlyCheckedIds.update { it + item.id }
        updateItem(updatedItem)
        viewModelScope.launch {
            delay(2.seconds)
            _recentlyCheckedIds.update { it - item.id }
        }
    } else {
        _recentlyCheckedIds.update { it - item.id }
        updateItem(updatedItem)
    }
}

fun GroceryViewModel.markDoneForTrip() {
    val listId = _selectedListId.value
    _dismissedRecommendationIds.value = emptySet()
    viewModelScope.launch {
        repository.markDoneForTrip(userId, listId)
        setShoppingStoreId(null)
    }
}

fun GroceryViewModel.addRecommendedItems(selectedItemIds: List<String>) {
    viewModelScope.launch {
        val currentRecommended = repository.getRecommendedItems(userId).first()
        currentRecommended.filter { selectedItemIds.contains(it.id) }.forEach { item ->
            repository.updateItem(item.copy(isBought = false, isActive = true))
        }
    }
}

fun GroceryViewModel.shareListWithUser(listId: String, memberUserId: String) {
    if (memberUserId.isNotBlank()) {
        viewModelScope.launch {
            repository.insertListMember(
                GroceryListMember(
                    listId = listId,
                    userId = memberUserId
                )
            )
        }
    }
}

fun GroceryViewModel.createInvite(listId: String) {
    viewModelScope.launch {
        val code = GroceryNetworkRepository.createInvite(listId)
        setActiveInviteCode(code)
    }
}

fun GroceryViewModel.joinList(code: String) {
    viewModelScope.launch {
        val listId = GroceryNetworkRepository.joinList(code)
        if (listId != null) {
            userSyncMetadataDao.clear(userId)
            SyncWorker.enqueue(application)
            setSelectedListId(listId)
            setSnackbarMessage(
                GrocerySnackbarMessage(
                    message = "Successfully joined the list!",
                    isError = false
                )
            )
        } else {
            setSnackbarMessage(
                GrocerySnackbarMessage(
                    message = "Failed to join list. Check the code and try again.",
                    isError = true
                )
            )
        }
    }
}

fun GroceryViewModel.removeListMember(member: GroceryListMember) {
    viewModelScope.launch {
        repository.deleteListMember(member)
    }
}

fun GroceryViewModel.getListMembers(listId: String): Flow<List<GroceryListMember>> = repository.getListMembers(listId)
