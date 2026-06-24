package fyi.teddy.android.grocery.ui

data class GrocerySnackbarMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)
