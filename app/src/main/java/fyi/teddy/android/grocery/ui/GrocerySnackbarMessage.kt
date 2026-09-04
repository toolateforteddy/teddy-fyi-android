package fyi.teddy.android.grocery.ui

/**
 * A single line of feedback, optionally carrying a way back out of it.
 *
 * [action] exists so a destructive change can be announced and reversed in the same
 * breath: the snackbar shows [actionLabel], and tapping it dispatches [action].
 */
data class GrocerySnackbarMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false,
    val actionLabel: String? = null,
    val action: GroceryUiEvent? = null
)
