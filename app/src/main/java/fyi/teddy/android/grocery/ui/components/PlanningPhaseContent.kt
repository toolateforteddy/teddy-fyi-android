package fyi.teddy.android.grocery.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryUiEvent
import fyi.teddy.android.grocery.ui.GroceryUiState

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
        // 1. The Top Store Bar: Horizontal chips for store context
        Text(
            text = "Where are you heading?",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = state.planningStoreContextId == null,
                    onClick = { onEvent(GroceryUiEvent.SetPlanningStoreContext(null)) },
                    label = { Text("General") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            items(stores) { store ->
                FilterChip(
                    selected = state.planningStoreContextId == store.id,
                    onClick = { onEvent(GroceryUiEvent.SetPlanningStoreContext(store.id)) },
                    label = { Text(store.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. The "Commonly Bought" Recommendation Tray
        val storeSpecificRecs = remember(state.planningStoreContextId, recommendedItems, storeInfos, state.dismissedRecommendationIds) {
            val storeId = state.planningStoreContextId
            val availableRecs = recommendedItems.filter { rec ->
                !state.dismissedRecommendationIds.contains(rec.id)
            }
            if (storeId == null) {
                // General: items with high frequency
                availableRecs.take(8)
            } else {
                // Filter OUT items that are explicitly marked as unavailable for this store
                // Items with NO mapping for this store are included by default
                val filtered = availableRecs.filter { rec ->
                    val info = storeInfos.find { (it.groceryItemId == rec.id) && (it.storeId == storeId) }
                    info?.isAvailable ?: true
                }
                
                filtered.take(8)
            }
        }

        if (storeSpecificRecs.isNotEmpty()) {
            val selectedStoreName = stores.find { it.id == state.planningStoreContextId }?.name ?: "Common"
            
            Text(
                text = "$selectedStoreName Recommendations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Two-column grid of recommendations
            Box(modifier = Modifier.heightIn(max = 200.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(storeSpecificRecs, key = { it.id }) { rec ->
                        RecommendationTile(
                            name = rec.name,
                            onClick = { onEvent(GroceryUiEvent.AddRecommendedItems(listOf(rec.id))) },
                            onDismiss = { onEvent(GroceryUiEvent.DismissRecommendation(rec.id)) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. The Main List View
        Text(
            text = "Your List",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
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
                    onToggleControls = {
                        expandedItemId = if (expandedItemId == item.id) null else item.id
                    },
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
            
            if (sortedItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "List is empty. Tap recommendations to add items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
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

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDismiss()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Dismiss", tint = Color.White)
            }
        }
    ) {
        Surface(
            onClick = onClick,
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(8.dp),
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
                    color = Color.White,
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
        color = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                    IconButton(onClick = onEditCategory, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Category, contentDescription = "Change Category", tint = Color.LightGray, modifier = Modifier.size(18.dp))
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
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.quantity.isNotBlank() && item.quantity != "1") {
                        Text(
                            text = item.quantity + (item.unit?.let { " $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
