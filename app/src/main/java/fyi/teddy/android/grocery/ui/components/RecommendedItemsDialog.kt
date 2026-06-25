package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.grocery.data.GroceryItem

@Composable
fun RecommendedItemsDialog(
    recommendedItems: List<GroceryItem>,
    activeItems: List<GroceryItem>,
    onDismiss: () -> Unit,
    onAddItems: (List<String>) -> Unit
) {
    val unboughtNames = activeItems.filter { it.isActive && !it.isBought }.map { it.name }.toSet()
    val availableRecommendations = recommendedItems.filter { !unboughtNames.contains(it.name) }
    val selectedItemIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recommended Items") },
        text = {
            if (availableRecommendations.isEmpty()) {
                Text("No recommendations yet. Buy items to see them here!")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableRecommendations) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedItemIds.contains(item.id)) {
                                        selectedItemIds.remove(item.id)
                                    } else {
                                        selectedItemIds.add(item.id)
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = selectedItemIds.contains(item.id),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedItemIds.add(item.id)
                                    else selectedItemIds.remove(item.id)
                                }
                            )
                            Text(item.name, modifier = Modifier.weight(1f))
                            Text(
                                text = "(${item.timesBought})",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableRecommendations.isNotEmpty()) {
                TextButton(onClick = {
                    onAddItems(selectedItemIds.toList())
                    onDismiss()
                }) { Text(stringResource(R.string.add)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
