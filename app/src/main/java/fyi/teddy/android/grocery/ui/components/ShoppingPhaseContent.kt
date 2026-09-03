package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryUiEvent
import fyi.teddy.android.grocery.ui.GroceryUiState

/**
 * Shopping Phase: High-velocity in-store mode.
 */
@Composable
fun ShoppingPhaseContent(
    state: GroceryUiState,
    items: List<GroceryItem>,
    inCartItems: List<GroceryItem>,
    stores: List<Store>,
    categories: List<Category>,
    onEvent: (GroceryUiEvent) -> Unit,
) {
    val activeStore = stores.find { it.id == state.shoppingStoreId }
    val expandedCategories = remember { mutableStateMapOf<String?, Boolean>() }
    var itemToEditCategory by remember { mutableStateOf<GroceryItem?>(null) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        // Heads-up Store Isolation
        Surface(
            color = if (state.shoppingStoreId == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.shoppingStoreId == null) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "WHERE ARE WE GOING?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stores.forEach { store ->
                            AssistChip(
                                onClick = { onEvent(GroceryUiEvent.SetShoppingStoreId(store.id)) },
                                label = { Text(store.name) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "YOU'RE AT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            activeStore?.name ?: "No Store Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    var showStoreSwitcher by remember { mutableStateOf(value = false) }
                    TextButton(onClick = { showStoreSwitcher = true }) {
                        Text("Switch")
                    }

                    DropdownMenu(
                        expanded = showStoreSwitcher,
                        onDismissRequest = { showStoreSwitcher = false }
                    ) {
                        stores.forEach { store ->
                            DropdownMenuItem(
                                text = { Text(store.name) },
                                onClick = {
                                    onEvent(GroceryUiEvent.SetShoppingStoreId(store.id))
                                    showStoreSwitcher = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.shoppingStoreId == null) {
            GroceryEmptyState(
                headline = "Pick a shop up top.",
                hint = "Then your list sorts itself into aisles."
            )
        } else if (items.isEmpty() && inCartItems.isEmpty()) {
            GroceryEmptyState(
                headline = "Every aisle is clear.",
                hint = "Nothing left to grab here."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
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
                        val isExpanded = expandedCategories[category.id] ?: true
                        val icon = aisleIcon(category.icon)
                        item(span = { GridItemSpan(2) }) {
                            AisleHeader(
                                name = category.name,
                                icon = icon,
                                tint = aisleTint(category.id),
                                itemCount = categoryItems.size,
                                doneCount = categoryItems.count { it.isBought },
                                isExpanded = isExpanded,
                                onToggle = { expandedCategories[category.id] = !isExpanded },
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        if (isExpanded) {
                            items(categoryItems, key = { it.id }) { item ->
                                ShoppingItemTile(
                                    item = item,
                                    tint = aisleTint(category.id),
                                    aisleIcon = icon,
                                    onToggleBought = { onEvent(GroceryUiEvent.ToggleBought(item, it)) },
                                    onEditCategory = { itemToEditCategory = item }
                                )
                            }
                        }
                    }
                }

                val otherItems = sortedItems.filter { it.categoryId == null || !knownCategoryIds.contains(it.categoryId) }
                if (otherItems.isNotEmpty()) {
                    val isExpanded = expandedCategories[null] ?: true
                    item(span = { GridItemSpan(2) }) {
                        AisleHeader(
                            name = "Everything else",
                            icon = aisleIcon(null),
                            tint = aisleTint(null),
                            itemCount = otherItems.size,
                            doneCount = otherItems.count { it.isBought },
                            isExpanded = isExpanded,
                            onToggle = { expandedCategories[null] = !isExpanded },
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    if (isExpanded) {
                        items(otherItems, key = { it.id }) { item ->
                            ShoppingItemTile(
                                item = item,
                                tint = aisleTint(null),
                                aisleIcon = aisleIcon(null),
                                onToggleBought = { onEvent(GroceryUiEvent.ToggleBought(item, it)) },
                                onEditCategory = { itemToEditCategory = item }
                            )
                        }
                    }
                }

                if (inCartItems.isNotEmpty()) {
                    val sortedInCartItems = inCartItems.sortedBy { it.name }
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "In the cart (${sortedInCartItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = GroceryTheme.colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                    }
                    items(sortedInCartItems, key = { it.id }) { item ->
                        ShoppingItemTile(
                            item = item,
                            tint = aisleTint(item.categoryId),
                            aisleIcon = aisleIcon(categories.find { it.id == item.categoryId }?.icon),
                            onToggleBought = { onEvent(GroceryUiEvent.ToggleBought(item, false)) },
                            onEditCategory = { itemToEditCategory = item }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingItemTile(
    item: GroceryItem,
    tint: Color,
    aisleIcon: ImageVector,
    onToggleBought: (Boolean) -> Unit,
    onEditCategory: () -> Unit
) {
    val alpha by animateFloatAsState(targetValue = if (item.isBought) 0.3f else 1f, label = "alpha")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(
                onClick = { onToggleBought(!item.isBought) },
                onLongClick = onEditCategory
            ),
        color = GroceryTheme.colors.card,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GroceryTheme.colors.outline)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Aisle tint edge: the same colour as the sign above this item. It stays at
            // full strength once an item is bought so the aisle stays readable as a block.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(tint)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 12.dp)
                    .alpha(alpha),
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = GroceryTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if ((item.quantity.isNotBlank()) && (item.quantity != "1")) {
                    Text(
                        text = "x${item.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GroceryTheme.colors.onSurfaceMuted,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
