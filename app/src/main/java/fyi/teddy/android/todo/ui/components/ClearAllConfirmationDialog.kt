package fyi.teddy.android.todo.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fyi.teddy.android.R
import fyi.teddy.android.todo.ui.theme.TodoTheme

@Composable
fun ClearAllConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_all) + "?") },
        text = { Text("This will permanently delete all tasks in the database for your account. This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm_clear), color = TodoTheme.colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.abort))
            }
        }
    )
}
