package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryUiEvent

/**
 * Need Phase: Focused on frictionless entry.
 */
@Composable
fun NeedPhaseContent(
    items: List<GroceryItem>,
    categories: List<Category>,
    stores: List<Store>,
    storeInfos: List<GroceryItemStoreInfo>,
    onEvent: (GroceryUiEvent) -> Unit,
) {
    var expandedItemId by remember { mutableStateOf<String?>(null) }
    var itemToEditCategory by remember { mutableStateOf<GroceryItem?>(null) }
    var itemToTagStores by remember { mutableStateOf<GroceryItem?>(null) }

    if (itemToEditCategory != null) {
        CategorySelectionDialog(
            item = itemToEditCategory!!,
            categories = categories,
            onDismiss = { itemToEditCategory = null },
            onConfirm = { categoryId ->
                onEvent(GroceryUiEvent.UpdateItem(itemToEditCategory!!.copy(categoryId = categoryId)))
                itemToEditCategory = null
            }
        )
    }

    itemToTagStores?.let { item ->
        StoreTaggingDialog(
            stores = stores,
            itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
            onDismiss = { itemToTagStores = null },
            onToggleAvailability = { storeId, isAvailable ->
                val currentInfo = storeInfos.find { it.groceryItemId == item.id && it.storeId == storeId }
                onEvent(GroceryUiEvent.UpdateStoreInfo(
                    currentInfo?.copy(isAvailable = isAvailable)
                        ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, isAvailable = isAvailable)
                ))
            }
        )
    }

    if (items.isEmpty()) {
        GroceryEmptyState(
            headline = "Nothing needed yet.",
            hint = "Enjoy it while it lasts."
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val sortedItems = items.sortedBy { it.name }
        val grouped = sortedItems.groupBy { it.categoryId }
        val knownCategoryIds = categories.map { it.id }.toSet()

        categories.forEach { category ->
            val categoryItems = grouped[category.id] ?: emptyList()
            if (categoryItems.isNotEmpty()) {
                val icon = aisleIcon(category.icon)
                item(span = { GridItemSpan(2) }) {
                    AisleHeader(
                        name = category.name,
                        icon = icon,
                        tint = aisleTint(category.id),
                        itemCount = categoryItems.size,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(categoryItems, key = { it.id }) { item ->
                    NeedItemTile(
                        item = item,
                        tint = aisleTint(category.id),
                        aisleIcon = icon,
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
                        },
                        onEditCategory = { itemToEditCategory = item },
                        onTagStores = { itemToTagStores = item }
                    )
                }
            }
        }

        // Show items that have no category OR a category that is not in the current list
        val otherItems = sortedItems.filter { it.categoryId == null || !knownCategoryIds.contains(it.categoryId) }
        if (otherItems.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                AisleHeader(
                    name = "Everything else",
                    icon = aisleIcon(null),
                    tint = aisleTint(null),
                    itemCount = otherItems.size,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(otherItems, key = { it.id }) { item ->
                NeedItemTile(
                    item = item,
                    tint = aisleTint(null),
                    aisleIcon = aisleIcon(null),
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
                    },
                    onEditCategory = { itemToEditCategory = item },
                    onTagStores = { itemToTagStores = item }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NeedItemTile(
    item: GroceryItem,
    tint: Color,
    aisleIcon: ImageVector,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onDelete: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditCategory: () -> Unit,
    onTagStores: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) GroceryTheme.colors.danger else Color.Transparent
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GroceryTheme.colors.onSurface)
            }
        },
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .combinedClickable(
                        onClick = onToggleControls,
                        onLongClick = onTagStores
                    ),
                color = GroceryTheme.colors.card,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GroceryTheme.colors.outline)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Aisle tint edge: the same colour as the sign above this item.
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(tint)
                    )
                    AnimatedContent(
                        targetState = showControls,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "NeedItemControls",
                        modifier = Modifier.weight(1f)
                    ) { isEditing ->
                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = onDecrement) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = GroceryTheme.colors.onSurface)
                                }
                                Text(
                                    text = item.quantity,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GroceryTheme.colors.onSurface
                                )
                                IconButton(onClick = onIncrement) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = GroceryTheme.colors.onSurface)
                                }
                                IconButton(onClick = onEditCategory) {
                                    Icon(Icons.Default.Category, contentDescription = "Category", tint = GroceryTheme.colors.onSurface)
                                }
                                IconButton(onClick = onDelete) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GroceryTheme.colors.danger)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 10.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ItemLeadingMark(
                                    itemName = item.name,
                                    fallbackIcon = aisleIcon,
                                    tint = tint
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GroceryTheme.colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if ((item.quantity.isNotBlank()) && (item.quantity != "1")) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "x${item.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GroceryTheme.colors.onSurfaceMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
