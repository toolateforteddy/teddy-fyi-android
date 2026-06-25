package fyi.teddy.android.grocery.ui

data class GroceryUiState(
    val currentPhase: GroceryPhase = GroceryPhase.NEED,
    val selectedStoreIds: Set<String> = emptySet(),
    val planningStoreContextId: String? = null,
    val shoppingStoreId: String? = null,
    val isEditMode: Boolean = false,
    val showRecommendedDialog: Boolean = false,
    val newItemName: String = "",
    val newItemQuantity: String = "1",
    val newItemUnit: String? = null,
    val newItemInput: String = "",
    val selectedCategoryId: String? = null,
    val recentlyCheckedIds: Set<String> = emptySet(),
    val selectedListId: String? = null,
    val activeInviteCode: String? = null,
    val snackbarMessage: GrocerySnackbarMessage? = null,
    val isAiReady: Boolean = false,
    val isCategorizing: Boolean = false,
    val hasItemsInDefaultList: Boolean = false
)
