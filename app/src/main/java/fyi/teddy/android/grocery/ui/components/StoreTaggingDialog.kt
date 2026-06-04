package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store

@Composable
fun StoreTaggingDialog(
    stores: List<Store>,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    onDismiss: () -> Unit,
    onToggleAvailability: (Int, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available at Stores") },
        text = {
            Column {
                stores.forEach { store ->
                    val info = itemStoreInfos.find { it.storeId == store.id }
                    val isAvailable = info?.isAvailable ?: true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAvailable,
                            onCheckedChange = { onToggleAvailability(store.id, it) }
                        )
                        Text(store.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
