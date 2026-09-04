package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
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
import kotlinx.coroutines.launch

/**
 * Narrowest grid the aisle jump rail earns its place on. Below this it would eat a column
 * of tiles on a phone to save a scroll gesture, which is a bad trade.
 */
private val RailBreakpoint = 600.dp

/** Room under the last row for the bottom bar, so the final tile is not trapped behind it. */
private val GridBottomInset = 88.dp

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
    val expandedAisles = remember { mutableStateMapOf<String, Boolean>() }
    var itemToEditCategory by remember { mutableStateOf<GroceryItem?>(null) }

    itemToEditCategory?.let { editing ->
        CategorySelectionDialog(
            item = editing,
            categories = categories,
            onDismiss = { itemToEditCategory = null },
            onConfirm = { categoryId ->
                onEvent(GroceryUiEvent.UpdateItem(editing.copy(categoryId = categoryId)))
                itemToEditCategory = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ShoppingStoreBar(state = state, stores = stores, onEvent = onEvent)

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
            ShoppingAisleGrid(
                aisles = shoppingAisles(items, inCartItems, categories, expandedAisles),
                onToggleAisle = { key -> expandedAisles[key] = !(expandedAisles[key] ?: true) },
                onToggleBought = { item, isBought -> onEvent(GroceryUiEvent.ToggleBought(item, isBought)) },
                onEditCategory = { itemToEditCategory = it },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Heads-up store isolation: which shop we are standing in, or a prompt to say. */
@Composable
private fun ShoppingStoreBar(
    state: GroceryUiState,
    stores: List<Store>,
    onEvent: (GroceryUiEvent) -> Unit,
) {
    val activeStore = stores.find { it.id == state.shoppingStoreId }

    Surface(
        color = if (state.shoppingStoreId == null) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
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
}

/**
 * The trip in the order it is walked: one aisle per category that has anything in it, then
 * whatever has no aisle, then the cart.
 */
@Composable
private fun shoppingAisles(
    items: List<GroceryItem>,
    inCartItems: List<GroceryItem>,
    categories: List<Category>,
    expandedAisles: Map<String, Boolean>,
): List<ShoppingAisle> {
    val sortedItems = items.sortedBy { it.name }
    val grouped = sortedItems.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()
    val aisles = mutableListOf<ShoppingAisle>()

    categories.forEach { category ->
        val categoryItems = grouped[category.id].orEmpty()
        if (categoryItems.isNotEmpty()) {
            aisles += ShoppingAisle(
                key = category.id,
                name = category.name,
                icon = aisleIcon(category.icon),
                tint = aisleTint(category.id),
                items = categoryItems,
                isExpanded = expandedAisles[category.id] ?: true,
            )
        }
    }

    val orphans = sortedItems.filter { it.categoryId == null || it.categoryId !in knownCategoryIds }
    if (orphans.isNotEmpty()) {
        aisles += ShoppingAisle(
            key = ShoppingAisle.UNCATEGORIZED_KEY,
            name = "Everything else",
            icon = aisleIcon(null),
            tint = aisleTint(null),
            items = orphans,
            isExpanded = expandedAisles[ShoppingAisle.UNCATEGORIZED_KEY] ?: true,
        )
    }

    if (inCartItems.isNotEmpty()) {
        aisles += ShoppingAisle(
            key = ShoppingAisle.CART_KEY,
            name = "In the cart",
            icon = Icons.Default.ShoppingCart,
            tint = GroceryTheme.colors.accent,
            items = inCartItems.sortedBy { it.name },
            isCollapsible = false,
            isCart = true,
        )
    }

    return aisles
}

/**
 * The list itself: an adaptive grid of tiles under aisle signs, with the jump rail beside
 * it once the screen is wide enough to spare the room.
 */
@Composable
private fun ShoppingAisleGrid(
    aisles: List<ShoppingAisle>,
    onToggleAisle: (String) -> Unit,
    onToggleBought: (GroceryItem, Boolean) -> Unit,
    onEditCategory: (GroceryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = GroceryTheme.metrics
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val showRail = maxWidth >= RailBreakpoint && aisles.size > 1

        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = metrics.minTileWidth),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = GridBottomInset, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.gutter),
                verticalArrangement = Arrangement.spacedBy(metrics.gutter),
            ) {
                aisles.forEach { aisle ->
                    item(key = aisle.key, span = { GridItemSpan(maxLineSpan) }) {
                        AisleHeader(
                            name = aisle.name,
                            icon = aisle.icon,
                            tint = aisle.tint,
                            itemCount = aisle.items.size,
                            doneCount = if (aisle.isCart) null else aisle.items.count { it.isBought },
                            isExpanded = if (aisle.isCollapsible) aisle.isExpanded else null,
                            onToggle = if (aisle.isCollapsible) ({ onToggleAisle(aisle.key) }) else null,
                            modifier = Modifier.padding(top = metrics.gutter),
                        )
                    }
                    if (aisle.isExpanded) {
                        items(aisle.items, key = { it.id }) { item ->
                            ShoppingItemTile(
                                item = item,
                                tint = aisle.tint,
                                aisleIcon = aisle.icon,
                                onToggleBought = { isBought ->
                                    onToggleBought(item, if (aisle.isCart) false else isBought)
                                },
                                onEditCategory = { onEditCategory(item) }
                            )
                        }
                    }
                }
            }

            if (showRail) {
                Spacer(modifier = Modifier.width(metrics.gutter))
                AisleJumpRail(
                    aisles = aisles,
                    onJumpTo = { index -> scope.launch { gridState.animateScrollToItem(index) } },
                    modifier = Modifier.padding(bottom = GridBottomInset),
                )
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
    val metrics = GroceryTheme.metrics

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.tileHeight)
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
                        fontSize = metrics.itemFontSize,
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
