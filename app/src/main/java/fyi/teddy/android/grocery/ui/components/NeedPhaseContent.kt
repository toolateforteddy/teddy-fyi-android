package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.GroceryUiEvent

/**
 * Need Phase: Focused on frictionless entry.
 */
@Composable
fun NeedPhaseContent(
    items: List<GroceryItem>,
    categories: List<Category>,
    onEvent: (GroceryUiEvent) -> Unit
) {
    var expandedItemId by remember { mutableStateOf<Int?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = items.groupBy { it.categoryId }
        categories.forEach { category ->
            val categoryItems = grouped[category.id] ?: emptyList()
            if (categoryItems.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = category.name.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(categoryItems, key = { it.id }) { item ->
                    NeedItemTile(
                        item = item,
                        showControls = expandedItemId == item.id,
                        onToggleControls = {
                            expandedItemId = if (expandedItemId == item.id) null else item.id
                        },
                        onDelete = { onEvent(GroceryUiEvent.DeleteItem(item)) },
                        onIncrement = {
                            val current = item.quantity.toIntOrNull() ?: 1
                            onEvent(GroceryUiEvent.UpdateItem(item.copy(quantity = (current + 1).toString())))
                        },
                        onDecrement = {
                            val current = item.quantity.toIntOrNull() ?: 1
                            if (current > 1) {
                                onEvent(GroceryUiEvent.UpdateItem(item.copy(quantity = (current - 1).toString())))
                            }
                        }
                    )
                }
            }
        }

        val uncategorized = grouped[null] ?: emptyList()
        if (uncategorized.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "UNCATEGORIZED",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(uncategorized, key = { it.id }) { item ->
                NeedItemTile(
                    item = item,
                    showControls = expandedItemId == item.id,
                    onToggleControls = {
                        expandedItemId = if (expandedItemId == item.id) null else item.id
                    },
                    onDelete = { onEvent(GroceryUiEvent.DeleteItem(item)) },
                    onIncrement = {
                        val current = item.quantity.toIntOrNull() ?: 1
                        onEvent(GroceryUiEvent.UpdateItem(item.copy(quantity = (current + 1).toString())))
                    },
                    onDecrement = {
                        val current = item.quantity.toIntOrNull() ?: 1
                        if (current > 1) {
                            onEvent(GroceryUiEvent.UpdateItem(item.copy(quantity = (current - 1).toString())))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedItemTile(
    item: GroceryItem,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onDelete: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.DismissedToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            val color = if (dismissState.dismissDirection == DismissDirection.EndToStart) Color.Red else Color.Transparent
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        dismissContent = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onToggleControls() },
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                AnimatedContent(
                    targetState = showControls,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "NeedItemControls"
                ) { isEditing ->
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = item.quantity,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (item.quantity.isNotBlank() && item.quantity != "1") {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "x${item.quantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
