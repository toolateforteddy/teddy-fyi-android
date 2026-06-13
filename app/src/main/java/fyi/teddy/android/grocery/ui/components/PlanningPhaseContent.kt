package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryUiEvent
import fyi.teddy.android.grocery.ui.GroceryUiState

/**
 * Planning Phase: Multi-store mapping and smart recommendations.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanningPhaseContent(
    state: GroceryUiState,
    items: List<GroceryItem>,
    stores: List<Store>,
    storeInfos: List<GroceryItemStoreInfo>,
    recommendedItems: List<GroceryItem>,
    onEvent: (GroceryUiEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Store Selector (Chip-based for multi-select)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedStoreIds.isEmpty(),
                    onClick = { onEvent(GroceryUiEvent.ToggleStoreSelection(-2)) }, // Special case to clear
                    label = { Text("All Items") }
                )
            }
            item {
                FilterChip(
                    selected = state.selectedStoreIds.contains(-1),
                    onClick = { onEvent(GroceryUiEvent.ToggleStoreSelection(-1)) },
                    label = { Text("Unassigned") }
                )
            }
            items(stores) { store ->
                FilterChip(
                    selected = state.selectedStoreIds.contains(store.id),
                    onClick = { onEvent(GroceryUiEvent.ToggleStoreSelection(store.id)) },
                    label = { Text(store.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendations Tray (Expandable)
        var recommendationsExpanded by remember { mutableStateOf(false) }
        val storeSpecificRecs = remember(state.selectedStoreIds, recommendedItems, storeInfos) {
            if (state.selectedStoreIds.isEmpty()) {
                // In Unassigned view, show recommendations that haven't been mapped anywhere yet
                recommendedItems.filter { rec ->
                    storeInfos.none { it.groceryItemId == rec.id && it.isAvailable }
                }
            } else {
                val selectedIds = state.selectedStoreIds
                recommendedItems.filter { rec ->
                    val infos = storeInfos.filter { it.groceryItemId == rec.id }
                    infos.any { it.storeId in selectedIds && it.isAvailable }
                }
            }
        }

        if (storeSpecificRecs.isNotEmpty()) {
            Surface(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recommendationsExpanded = !recommendationsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Commonly bought ${if (state.selectedStoreIds.size == 1) "here" else "items"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            if (recommendationsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    if (recommendationsExpanded) {
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            storeSpecificRecs.take(6).forEach { rec ->
                                SuggestionChip(
                                    onClick = { onEvent(GroceryUiEvent.AddRecommendedItems(listOf(rec.id))) },
                                    label = { Text(rec.name) },
                                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
                PlanningItemRow(
                    item = item,
                    stores = stores,
                    itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                    onToggleStore = { storeId ->
                        val current = storeInfos.find { it.groceryItemId == item.id && it.storeId == storeId }
                        val nextInfo = when (current?.isAvailable) {
                            null -> GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, isAvailable = true)
                            true -> current.copy(isAvailable = false)
                            false -> null // We'll handle deletion in the event
                        }
                        
                        if (nextInfo != null) {
                            onEvent(GroceryUiEvent.UpdateStoreInfo(nextInfo))
                        } else if (current != null) {
                            onEvent(GroceryUiEvent.DeleteStoreInfo(current))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanningItemRow(
    item: GroceryItem,
    stores: List<Store>,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    onToggleStore: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                stores.forEach { store ->
                    val info = itemStoreInfos.find { it.storeId == store.id }
                    val isAvailable = info?.isAvailable
                    
                    AssistChip(
                        onClick = { onToggleStore(store.id) },
                        label = { 
                            Text(
                                text = store.name, 
                                fontSize = 10.sp,
                                style = if (isAvailable == false) {
                                    MaterialTheme.typography.labelSmall.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                } else {
                                    MaterialTheme.typography.labelSmall
                                }
                            ) 
                        },
                        leadingIcon = {
                            if (isAvailable == true) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            } else if (isAvailable == false) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        },
                        colors = when (isAvailable) {
                            true -> AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            false -> AssistChipDefaults.assistChipColors(
                                containerColor = Color.DarkGray.copy(alpha = 0.2f),
                                labelColor = Color.Gray,
                                leadingIconContentColor = Color.Gray
                            )
                            else -> AssistChipDefaults.assistChipColors() // Unmapped
                        },
                        border = if (isAvailable == null) {
                            AssistChipDefaults.assistChipBorder(borderColor = Color.DarkGray)
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
