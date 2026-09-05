package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val expandedAisles = remember { mutableStateMapOf<String?, Boolean>() }
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
                aisles = shoppingAisles(items, categories),
                inCartItems = inCartItems.sortedBy { it.name },
                categories = categories,
                isExpanded = { categoryId -> expandedAisles[categoryId] ?: true },
                onToggleAisle = { categoryId ->
                    expandedAisles[categoryId] = !(expandedAisles[categoryId] ?: true)
                },
                onToggleBought = { item, isBought -> onEvent(GroceryUiEvent.ToggleBought(item, isBought)) },
                onEditCategory = { itemToEditCategory = it },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Heads-up store isolation: which shop we are standing in, or a prompt to say. */
@OptIn(ExperimentalLayoutApi::class)
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
 * The aisles in the order the grid draws them: every category that has something left to
 * buy, then whatever has no aisle of its own.
 */
@Composable
private fun shoppingAisles(
    items: List<GroceryItem>,
    categories: List<Category>,
): List<ShoppingAisle> {
    val sortedItems = items.sortedBy { it.name }
    val grouped = sortedItems.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()

    return buildList {
        categories.forEach { category ->
            val categoryItems = grouped[category.id].orEmpty()
            if (categoryItems.isNotEmpty()) {
                add(ShoppingAisle(category.id, category.name, aisleIcon(category.icon), categoryItems))
            }
        }
        val otherItems = sortedItems.filter {
            it.categoryId == null || !knownCategoryIds.contains(it.categoryId)
        }
        if (otherItems.isNotEmpty()) {
            add(ShoppingAisle(null, "Everything else", aisleIcon(null), otherItems))
        }
    }
}

/**
 * The list itself: an adaptive grid of tiles under aisle signs, the sign of the aisle you
 * are in pinned above it, and the jump rail beside it once the screen is wide enough.
 *
 * All three navigate by the same [aisleSpans] arithmetic, so a tap on the rail and the sign
 * that pins can never disagree about where an aisle starts.
 */
@Composable
private fun ShoppingAisleGrid(
    aisles: List<ShoppingAisle>,
    inCartItems: List<GroceryItem>,
    categories: List<Category>,
    isExpanded: (String?) -> Boolean,
    onToggleAisle: (String?) -> Unit,
    onToggleBought: (GroceryItem, Boolean) -> Unit,
    onEditCategory: (GroceryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = GroceryTheme.metrics
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val spans = aisleSpans(aisles.map { it.categoryId to it.items.size }, isExpanded)
    // In a shop you are standing in one aisle at a time, so the sign for the aisle you are
    // in stays put once its own sign has scrolled off the top.
    val pinned by remember(spans) {
        derivedStateOf { pinnedAisle(spans, gridState.firstVisibleItemIndex) }
    }

    BoxWithConstraints(modifier = modifier) {
        val showRail = maxWidth >= RailBreakpoint && aisles.size > 1

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = metrics.minTileWidth),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = GridBottomInset, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(metrics.gutter),
                    verticalArrangement = Arrangement.spacedBy(metrics.gutter),
                ) {
                    aisles.forEach { aisle ->
                        val expanded = isExpanded(aisle.categoryId)
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AisleHeader(
                                name = aisle.name,
                                icon = aisle.icon,
                                tint = aisleTint(aisle.categoryId),
                                itemCount = aisle.items.size,
                                doneCount = aisle.items.count { it.isBought },
                                isExpanded = expanded,
                                onToggle = { onToggleAisle(aisle.categoryId) },
                                modifier = Modifier.padding(top = metrics.gutter),
                            )
                        }
                        if (expanded) {
                            items(aisle.items, key = { it.id }) { item ->
                                ShoppingItemTile(
                                    item = item,
                                    tint = aisleTint(aisle.categoryId),
                                    aisleIcon = aisle.icon,
                                    onToggleBought = { onToggleBought(item, it) },
                                    onEditCategory = { onEditCategory(item) }
                                )
                            }
                        }
                    }

                    if (inCartItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "In the cart (${inCartItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = GroceryTheme.colors.onSurfaceMuted,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                            )
                        }
                        items(inCartItems, key = { it.id }) { item ->
                            ShoppingItemTile(
                                item = item,
                                tint = aisleTint(item.categoryId),
                                aisleIcon = aisleIcon(categories.find { it.id == item.categoryId }?.icon),
                                onToggleBought = { onToggleBought(item, false) },
                                onEditCategory = { onEditCategory(item) }
                            )
                        }
                    }
                }

                PinnedAisleSign(
                    span = pinned,
                    aisles = aisles,
                    onClick = { headerIndex -> scope.launch { gridState.animateScrollToItem(headerIndex) } },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            if (showRail) {
                Spacer(modifier = Modifier.width(metrics.gutter))
                AisleJumpRail(
                    aisles = aisles,
                    spans = spans,
                    onJumpTo = { headerIndex -> scope.launch { gridState.animateScrollToItem(headerIndex) } },
                    modifier = Modifier.padding(bottom = GridBottomInset),
                )
            }
        }
    }
}

/** The sign for the aisle being scrolled through, held above the list. */
@Composable
private fun PinnedAisleSign(
    span: AisleSpan?,
    aisles: List<ShoppingAisle>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinnedSpan = span ?: return
    val aisle = aisles.firstOrNull { it.categoryId == pinnedSpan.categoryId } ?: return

    // Opaque backdrop: the tiles scroll underneath the pinned sign.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GroceryTheme.colors.screen)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AisleHeader(
            name = aisle.name,
            icon = aisle.icon,
            tint = aisleTint(aisle.categoryId),
            itemCount = aisle.items.size,
            doneCount = aisle.items.count { it.isBought },
            modifier = Modifier.clickable(
                onClickLabel = "Scroll to ${aisle.name}"
            ) { onClick(pinnedSpan.headerIndex) }
        )
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
