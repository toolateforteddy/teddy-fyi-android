package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryUiEvent
import fyi.teddy.android.grocery.ui.GroceryUiState
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/**
 * Planning Phase: Memory-jogging tool to build the global list.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanningPhaseContent(
    state: GroceryUiState,
    items: List<GroceryItem>,
    stores: List<Store>,
    storeInfos: List<GroceryItemStoreInfo>,
    recommendedItems: List<GroceryItem>,
    categories: List<Category>,
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

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. The Top Store Bar: wrapping chips for store context
        StoreContextBar(
            stores = stores,
            selectedStoreId = state.planningStoreContextId,
            onSelectStore = { onEvent(GroceryUiEvent.SetPlanningStoreContext(it)) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Planning is a memory-jogging screen: where there is width for it, the
            // jogs and the list sit side by side instead of the tray being squeezed
            // into a strip above the list.
            val twoPane = maxWidth >= TwoPaneMinWidth

            val recommendationLimit = if (twoPane) WIDE_RECOMMENDATION_COUNT else STACKED_RECOMMENDATION_COUNT
            val storeSpecificRecs = remember(
                state.planningStoreContextId,
                recommendedItems,
                storeInfos,
                state.dismissedRecommendationIds,
                recommendationLimit
            ) {
                val storeId = state.planningStoreContextId
                val availableRecs = recommendedItems.filter { rec ->
                    !state.dismissedRecommendationIds.contains(rec.id)
                }
                if (storeId == null) {
                    // General: items with high frequency
                    availableRecs.take(recommendationLimit)
                } else {
                    // Filter OUT items that are explicitly marked as unavailable for this store
                    // Items with NO mapping for this store are included by default
                    val filtered = availableRecs.filter { rec ->
                        val info = storeInfos.find { (it.groceryItemId == rec.id) && (it.storeId == storeId) }
                        info?.isAvailable ?: true
                    }

                    filtered.take(recommendationLimit)
                }
            }

            val recommendationTitle =
                (stores.find { it.id == state.planningStoreContextId }?.name ?: "Common") + " Recommendations"

            val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
                PlanningListPane(
                    items = items,
                    expandedItemId = expandedItemId,
                    onToggleControls = { item ->
                        expandedItemId = if (expandedItemId == item.id) null else item.id
                    },
                    onEditCategory = { itemToEditCategory = it },
                    onTagStores = { itemToTagStores = it },
                    onEvent = onEvent,
                    modifier = paneModifier
                )
            }

            if (twoPane) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (storeSpecificRecs.isNotEmpty()) {
                        RecommendationTray(
                            title = recommendationTitle,
                            recommendations = storeSpecificRecs,
                            onEvent = onEvent,
                            modifier = Modifier
                                .weight(TRAY_PANE_WEIGHT)
                                .fillMaxHeight()
                        )
                    }
                    listPane(
                        Modifier
                            .weight(LIST_PANE_WEIGHT)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (storeSpecificRecs.isNotEmpty()) {
                        RecommendationTray(
                            title = recommendationTitle,
                            recommendations = storeSpecificRecs,
                            onEvent = onEvent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = StackedTrayMaxHeight)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    listPane(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Below this much *content* width the tray stacks above the list, as it always has.
 * Same number as GroceryScreen's compact/medium breakpoint, but measured after the
 * NavigationRail has taken its share rather than against the whole screen.
 */
private val TwoPaneMinWidth = 600.dp

/** Height the tray is allowed when it is stacked above the list. */
private val StackedTrayMaxHeight = 200.dp

private const val TRAY_PANE_WEIGHT = 0.4f
private const val LIST_PANE_WEIGHT = 0.6f

private const val STACKED_RECOMMENDATION_COUNT = 8

/** A full-height tray has room for more jogs than a 200dp strip does. */
private const val WIDE_RECOMMENDATION_COUNT = 16

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreContextBar(
    stores: List<Store>,
    selectedStoreId: String?,
    onSelectStore: (String?) -> Unit
) {
    Text(
        text = "Where are you heading?",
        style = MaterialTheme.typography.labelMedium,
        color = GroceryTheme.colors.onSurfaceMuted,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selectedStoreId == null,
            onClick = { onSelectStore(null) },
            label = { Text("General") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        stores.forEach { store ->
            FilterChip(
                selected = selectedStoreId == store.id,
                onClick = { onSelectStore(store.id) },
                label = { Text(store.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/** The "Commonly Bought" recommendation tray. */
@Composable
private fun RecommendationTray(
    title: String,
    recommendations: List<GroceryItem>,
    onEvent: (GroceryUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = RecommendationTileMinWidth),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recommendations, key = { it.id }) { rec ->
                RecommendationTile(
                    name = rec.name,
                    onClick = { onEvent(GroceryUiEvent.AddRecommendedItems(listOf(rec.id))) },
                    onDismiss = { onEvent(GroceryUiEvent.DismissRecommendation(rec.id)) }
                )
            }
        }
    }
}

/** Tiles below this are too narrow for an item name; wider panes get more columns. */
private val RecommendationTileMinWidth = 220.dp

/** "Your List": every active item, whatever else is on screen beside it. */
@Composable
private fun PlanningListPane(
    items: List<GroceryItem>,
    expandedItemId: String?,
    onToggleControls: (GroceryItem) -> Unit,
    onEditCategory: (GroceryItem) -> Unit,
    onTagStores: (GroceryItem) -> Unit,
    onEvent: (GroceryUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Your List",
            style = MaterialTheme.typography.titleSmall,
            color = GroceryTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sortedItems = items.sortedBy { it.name }
            items(sortedItems) { item ->
                PlanningItemTile(
                    item = item,
                    showControls = expandedItemId == item.id,
                    onToggleControls = { onToggleControls(item) },
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
                    onEditCategory = { onEditCategory(item) },
                    onTagStores = { onTagStores(item) }
                )
            }

            if (sortedItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "List is empty. Tap recommendations to add items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GroceryTheme.colors.onSurfaceMuted
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationTile(
    name: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val tileShape = RoundedCornerShape(8.dp)

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDismiss()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.clip(tileShape),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Only paint the dismiss backdrop while a swipe is actually in progress,
            // otherwise it shows as a red fringe around every resting tile.
            if (direction != SwipeToDismissBoxValue.Settled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(GroceryTheme.colors.dangerSurface, tileShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Dismiss",
                        tint = GroceryTheme.colors.onDangerSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) {
        Surface(
            onClick = onClick,
            color = GroceryTheme.colors.card,
            shape = tileShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroceryTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanningItemTile(
    item: GroceryItem,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditCategory: () -> Unit,
    onTagStores: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(
                onClick = onToggleControls,
                onLongClick = onTagStores
            ),
        color = GroceryTheme.colors.well,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GroceryTheme.colors.outline)
    ) {
        AnimatedContent(
            targetState = showControls,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "PlanningItemControls"
        ) { isEditing ->
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = GroceryTheme.colors.onSurface, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = item.quantity,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GroceryTheme.colors.onSurface, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEditCategory, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Category, contentDescription = "Change Category", tint = GroceryTheme.colors.onSurfaceMuted, modifier = Modifier.size(18.dp))
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroceryTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.quantity.isNotBlank() && item.quantity != "1") {
                        Text(
                            text = item.quantity + (item.unit?.let { " $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = GroceryTheme.colors.onSurfaceMuted
                        )
                    }
                }
            }
        }
    }
}
