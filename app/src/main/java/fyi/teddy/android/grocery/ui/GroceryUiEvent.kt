package fyi.teddy.android.grocery.ui

import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store

sealed interface GroceryUiEvent {
    data class SetPhase(val phase: GroceryPhase) : GroceryUiEvent
    data class ToggleStoreSelection(val storeId: String) : GroceryUiEvent
    data class SetPlanningStoreContext(val storeId: String?) : GroceryUiEvent
    data class SetShoppingStoreId(val storeId: String?) : GroceryUiEvent
    data class SetEditMode(val enabled: Boolean) : GroceryUiEvent
    data class SetShowRecommendedDialog(val show: Boolean) : GroceryUiEvent
    data class SetNewItemName(val name: String) : GroceryUiEvent
    data class SetNewItemQuantity(val qty: String) : GroceryUiEvent
    data class SetNewItemUnit(val unit: String?) : GroceryUiEvent
    data class SetNewItemInput(val input: String) : GroceryUiEvent
    data class SetSelectedCategoryId(val categoryId: String?) : GroceryUiEvent
    data class SetSelectedListId(val listId: String?) : GroceryUiEvent
    data class InsertItemFromInput(val input: String) : GroceryUiEvent
    
    // Mutators
    data class InsertItem(val name: String, val quantity: String? = null, val categoryId: String?, val unit: String? = null) : GroceryUiEvent
    data class UpdateItem(val item: GroceryItem) : GroceryUiEvent
    data class DeleteItem(val item: GroceryItem) : GroceryUiEvent
    /** Puts a just-deleted item back exactly as it was, id and position included. */
    data class RestoreItem(val item: GroceryItem) : GroceryUiEvent
    data class MoveItemUp(val item: GroceryItem, val siblings: List<GroceryItem>) : GroceryUiEvent
    data class MoveItemDown(val item: GroceryItem, val siblings: List<GroceryItem>) : GroceryUiEvent
    data class UpdateStoreInfo(val info: GroceryItemStoreInfo) : GroceryUiEvent
    data class DeleteStoreInfo(val info: GroceryItemStoreInfo) : GroceryUiEvent
    data class ToggleBought(val item: GroceryItem, val isChecked: Boolean) : GroceryUiEvent
    object MarkDoneForTrip : GroceryUiEvent
    
    // Store
    data class InsertStore(val name: String) : GroceryUiEvent
    data class DeleteStore(val store: Store) : GroceryUiEvent
    data class UpdateStore(val store: Store) : GroceryUiEvent
    data class SwapStorePositions(val store1: Store, val store2: Store) : GroceryUiEvent
    
    // Category
    data class InsertCategory(val name: String) : GroceryUiEvent
    data class UpdateCategory(val category: Category) : GroceryUiEvent
    data class DeleteCategory(val category: Category) : GroceryUiEvent
    data class SwapCategoryPositions(val cat1: Category, val cat2: Category) : GroceryUiEvent
    
    // List & Collaboration
    data class InsertList(val name: String) : GroceryUiEvent
    data class DeleteList(val list: GroceryList) : GroceryUiEvent
    data class UpdateList(val list: GroceryList) : GroceryUiEvent
    data class ReorderLists(val lists: List<GroceryList>) : GroceryUiEvent
    data class ShareList(val listId: String, val userId: String) : GroceryUiEvent
    data class CreateInvite(val listId: String) : GroceryUiEvent
    data class JoinList(val code: String) : GroceryUiEvent
    data class DismissSnackbar(val messageId: Long) : GroceryUiEvent
    data class RemoveListMember(val member: GroceryListMember) : GroceryUiEvent
    data class AddRecommendedItems(val selectedIds: List<String>) : GroceryUiEvent
    data class DismissRecommendation(val itemId: String) : GroceryUiEvent
}
