package fyi.teddy.android.grocery.ui

data class GroceryUiState(
    val currentPhase: GroceryPhase = GroceryPhase.NEED,
    val selectedStoreIds: Set<Int> = emptySet(),
    val shoppingStoreId: Int? = null,
    val isEditMode: Boolean = false,
    val showRecommendedDialog: Boolean = false,
    val newItemName: String = "",
    val newItemQuantity: String = "1",
    val newItemUnit: String? = null,
    val newItemInput: String = "",
    val selectedCategoryId: Int? = null,
    val recentlyCheckedIds: Set<Int> = emptySet(),
    val selectedListId: String? = null
)
