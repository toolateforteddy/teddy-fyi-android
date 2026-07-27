package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
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
                        "SELECT A STORE TO SHOP",
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
                            "SHOPPING AT",
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select a store above to see your list",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
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
                        item(span = { GridItemSpan(2) }) {
                            ShoppingCategoryHeader(
                                categoryName = category.name,
                                isExpanded = isExpanded,
                                onToggle = { expandedCategories[category.id] = !isExpanded }
                            )
                        }
                        if (isExpanded) {
                            items(categoryItems, key = { it.id }) { item ->
                                ShoppingItemTile(
                                    item = item,
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
                        ShoppingCategoryHeader(
                            categoryName = "Uncategorized",
                            isExpanded = isExpanded,
                            onToggle = { expandedCategories[null] = !isExpanded }
                        )
                    }
                    if (isExpanded) {
                        items(otherItems, key = { it.id }) { item ->
                            ShoppingItemTile(
                                item = item,
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
                            "In Cart (${sortedInCartItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                    }
                    items(sortedInCartItems, key = { it.id }) { item ->
                        ShoppingItemTile(
                            item = item,
                            onToggleBought = { onEvent(GroceryUiEvent.ToggleBought(item, false)) },
                            onEditCategory = { itemToEditCategory = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingCategoryHeader(
    categoryName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = categoryName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingItemTile(
    item: GroceryItem,
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
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if ((item.quantity.isNotBlank()) && (item.quantity != "1")) {
                Text(
                    text = "x${item.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
