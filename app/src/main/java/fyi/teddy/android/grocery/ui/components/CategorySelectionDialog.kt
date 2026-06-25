package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem

@Composable
fun CategorySelectionDialog(
    item: GroceryItem,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Category") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfirm(null) }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = item.categoryId == null, onClick = null)
                    Text("No Category", modifier = Modifier.padding(start = 8.dp))
                }
                categories.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(category.id) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = item.categoryId == category.id, onClick = null)
                        Text(category.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
