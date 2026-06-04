package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.grocery.ui.components.AddListDialog
import fyi.teddy.android.grocery.ui.components.GroceryItemRowContainer
import fyi.teddy.android.grocery.ui.components.RecommendedItemsDialog
import fyi.teddy.android.grocery.ui.components.ShareListDialog
import java.util.*

enum class GroceryPhase {
    NEED, PLANNING, SHOPPING;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroceryScreen(userId: String, onBack: () -> Unit, onManageConfig: () -> Unit) {
    val context = LocalContext.current
    val viewModel: GroceryViewModel = viewModel(
        factory = GroceryViewModelFactory(context.applicationContext as android.app.Application, userId)
    )
    
    val state by viewModel.state.collectAsState()
    
    val items by viewModel.items.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val storeInfos by viewModel.storeInfos.collectAsState()
    val recommendedItems by viewModel.recommendedItems.collectAsState()

    val standardCategoryItems by viewModel.standardCategoryItems.collectAsState()
    val inCartItems by viewModel.inCartItems.collectAsState()
    
    val lists by viewModel.lists.collectAsState()
    
    var showListSelectorMenu by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showShareListDialog by remember { mutableStateOf(false) }
    
    val nameFocusRequester = remember { FocusRequester() }

    val uniqueNames = remember(items) {
        items.map { it.name }.distinct().sorted()
    }
    
    val suggestions = remember(state.newItemName, uniqueNames) {
        if (state.newItemName.length < 2) emptyList()
        else uniqueNames.filter { it.contains(state.newItemName, ignoreCase = true) && !it.equals(state.newItemName, ignoreCase = true) }
    }

    val onAddNewItem = {
        if (state.newItemName.isNotBlank()) {
            viewModel.onEvent(GroceryUiEvent.InsertItem(state.newItemName, state.newItemQuantity, state.selectedCategoryId, state.newItemUnit))
            viewModel.onEvent(GroceryUiEvent.SetNewItemName(""))
            viewModel.onEvent(GroceryUiEvent.SetNewItemQuantity("1"))
            viewModel.onEvent(GroceryUiEvent.SetNewItemUnit(null))
            nameFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grocery: ${state.currentPhase.displayName}") },
                actions = {
                    if (state.currentPhase != GroceryPhase.SHOPPING) {
                        IconButton(onClick = { viewModel.onEvent(GroceryUiEvent.SetEditMode(!state.isEditMode)) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (state.isEditMode) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    IconButton(onClick = onManageConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (state.currentPhase == GroceryPhase.SHOPPING) {
                        var showConfirmTripDone by remember { mutableStateOf(false) }
                        IconButton(onClick = { showConfirmTripDone = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Trip Complete")
                        }
                        if (showConfirmTripDone) {
                            AlertDialog(
                                onDismissRequest = { showConfirmTripDone = false },
                                title = { Text("Complete Trip?") },
                                text = { Text("Are you sure you want to mark all In Cart items as done and move them to history?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.onEvent(GroceryUiEvent.MarkDoneForTrip)
                                        showConfirmTripDone = false
                                    }) { Text("Confirm") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmTripDone = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Black) {
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.NEED,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.NEED)) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Need") },
                    label = { Text("Need") }
                )
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.PLANNING,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.PLANNING)) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Planning") },
                    label = { Text("Planning") }
                )
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.SHOPPING,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.SHOPPING)) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping") },
                    label = { Text("Shopping") }
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // List / Space selector Row for Shared Lists
                val activeList = lists.find { it.id == state.selectedListId }
                val activeListName = activeList?.name ?: "Default List"

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { showListSelectorMenu = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Lists", tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeListName,
                                color = Color.White,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch List", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showListSelectorMenu,
                            onDismissRequest = { showListSelectorMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default List") },
                                onClick = {
                                    viewModel.onEvent(GroceryUiEvent.SetSelectedListId(null))
                                    showListSelectorMenu = false
                                }
                            )
                            lists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name) },
                                    onClick = {
                                        viewModel.onEvent(GroceryUiEvent.SetSelectedListId(list.id))
                                        showListSelectorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAddListDialog = true }) {
                            Icon(Icons.Default.Create, contentDescription = "New List", tint = Color.White)
                        }
                        if (state.selectedListId != null) {
                            IconButton(onClick = { showShareListDialog = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Share List", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.onEvent(GroceryUiEvent.DeleteList(activeList!!)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete List", tint = Color.Red)
                            }
                        }
                    }
                }

                // Add List Dialog
                if (showAddListDialog) {
                    AddListDialog(
                        onDismiss = { showAddListDialog = false },
                        onConfirm = { name ->
                            viewModel.onEvent(GroceryUiEvent.InsertList(name))
                            showAddListDialog = false
                        }
                    )
                }

                // Share List Dialog
                if (showShareListDialog && state.selectedListId != null) {
                    ShareListDialog(
                        listName = activeListName,
                        membersFlow = remember(state.selectedListId) { viewModel.getListMembers(state.selectedListId!!) },
                        onDismiss = { showShareListDialog = false },
                        onShare = { userId ->
                            viewModel.onEvent(GroceryUiEvent.ShareList(state.selectedListId!!, userId))
                        },
                        onRemoveMember = { member ->
                            viewModel.onEvent(GroceryUiEvent.RemoveListMember(member))
                        }
                    )
                }

                if (state.currentPhase == GroceryPhase.PLANNING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stores:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.onEvent(GroceryUiEvent.SetShowRecommendedDialog(true)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Recommended", fontSize = 10.sp)
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stores.forEach { store ->
                            FilterChip(
                                selected = state.selectedStoreIds.contains(store.id),
                                onClick = {
                                    viewModel.onEvent(GroceryUiEvent.ToggleStoreSelection(store.id))
                                },
                                label = { Text(store.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.currentPhase == GroceryPhase.SHOPPING) {
                    if (stores.isNotEmpty()) {
                        Text("Shopping at:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        ScrollableTabRow(
                            selectedTabIndex = stores.indexOfFirst { it.id == state.shoppingStoreId }.coerceAtLeast(0),
                            containerColor = Color.Black,
                            edgePadding = 0.dp
                        ) {
                            stores.forEach { store ->
                                Tab(
                                    selected = state.shoppingStoreId == store.id,
                                    onClick = { viewModel.onEvent(GroceryUiEvent.SetShoppingStoreId(store.id)) },
                                    text = { Text(store.name) }
                                )
                            }
                        }
                        if (state.shoppingStoreId == null) {
                            viewModel.onEvent(GroceryUiEvent.SetShoppingStoreId(stores.firstOrNull()?.id))
                        }
                    } else {
                        Text("No stores defined. Please add stores in settings.", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.currentPhase != GroceryPhase.SHOPPING) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = state.newItemName,
                                onValueChange = { viewModel.onEvent(GroceryUiEvent.SetNewItemName(it)) },
                                modifier = Modifier.weight(2f).focusRequester(nameFocusRequester),
                                placeholder = { Text("Item name...", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1A1A1A),
                                    unfocusedContainerColor = Color(0xFF1A1A1A)
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = state.newItemQuantity,
                                onValueChange = { viewModel.onEvent(GroceryUiEvent.SetNewItemQuantity(it)) },
                                modifier = Modifier.weight(0.9f),
                                placeholder = { Text("Qty", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1A1A1A),
                                    unfocusedContainerColor = Color(0xFF1A1A1A)
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onAddNewItem() }
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Inline Unit selector
                            var expandedAddUnitDropdown by remember { mutableStateOf(false) }
                            val commonUnits = listOf("pcs", "lbs", "oz", "g", "kg", "ml", "L", "cans", "packs", "bottles", "bags")

                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedButton(
                                    onClick = { expandedAddUnitDropdown = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color(0xFF1A1A1A)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = state.newItemUnit ?: "Unit",
                                            color = if (state.newItemUnit == null) Color.Gray else Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Unit", tint = Color.Gray)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedAddUnitDropdown,
                                    onDismissRequest = { expandedAddUnitDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("No Unit") },
                                        onClick = {
                                            viewModel.onEvent(GroceryUiEvent.SetNewItemUnit(null))
                                            expandedAddUnitDropdown = false
                                        }
                                    )
                                    commonUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                viewModel.onEvent(GroceryUiEvent.SetNewItemUnit(u))
                                                expandedAddUnitDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onAddNewItem() }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add), tint = Color.White,
                                )
                            }
                        }
                        
                        // Category selection for new item
                        if (categories.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = state.selectedCategoryId == null,
                                        onClick = { viewModel.onEvent(GroceryUiEvent.SetSelectedCategoryId(null)) },
                                        label = { Text("No Category") }
                                    )
                                }
                                items(categories) { category ->
                                    FilterChip(
                                        selected = state.selectedCategoryId == category.id,
                                        onClick = { viewModel.onEvent(GroceryUiEvent.SetSelectedCategoryId(category.id)) },
                                        label = { Text(category.name) }
                                    )
                                }
                            }
                        }

                        if (suggestions.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestions) { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            val inactiveItem = items.find { it.name.equals(suggestion, ignoreCase = true) && !it.isActive }
                                            if (inactiveItem != null) {
                                                viewModel.onEvent(GroceryUiEvent.UpdateItem(inactiveItem.copy(isActive = true)))
                                            } else {
                                                viewModel.onEvent(GroceryUiEvent.SetNewItemName(suggestion))
                                            }
                                            viewModel.onEvent(GroceryUiEvent.SetNewItemName(""))
                                            viewModel.onEvent(GroceryUiEvent.SetNewItemQuantity("1"))
                                        },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val groupedItems = standardCategoryItems.groupBy { it.categoryId }
                    
                    categories.forEach { category ->
                        val categoryItems = groupedItems[category.id] ?: emptyList()
                        if (categoryItems.isNotEmpty()) {
                            item {
                                CategoryHeader(category.name)
                            }
                            itemsIndexed(categoryItems, key = { _, item -> item.id }) { index, item ->
                                GroceryItemRowContainer(
                                    item = item,
                                    currentPhase = state.currentPhase,
                                    shoppingStoreId = state.shoppingStoreId,
                                    itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                    stores = stores,
                                    categories = categories,
                                    isEditMode = state.isEditMode,
                                    index = index,
                                    totalItems = categoryItems.size,
                                    onUpdateItem = { updatedItem ->
                                        viewModel.onEvent(GroceryUiEvent.UpdateItem(updatedItem))
                                    },
                                    onDeleteItem = {
                                        viewModel.onEvent(GroceryUiEvent.DeleteItem(item))
                                    },
                                    onUpdateStoreInfo = { info ->
                                        viewModel.onEvent(GroceryUiEvent.UpdateStoreInfo(info))
                                    },
                                    onMoveItem = { _, toIndex ->
                                        if (toIndex < index) {
                                            viewModel.onEvent(GroceryUiEvent.MoveItemUp(item, categoryItems))
                                        } else {
                                            viewModel.onEvent(GroceryUiEvent.MoveItemDown(item, categoryItems))
                                        }
                                    },
                                    onToggleBought = { groceryItem, isChecked ->
                                        viewModel.onEvent(GroceryUiEvent.ToggleBought(groceryItem, isChecked))
                                    }
                                )
                            }
                        }
                    }
                    
                    val uncategorizedItems = groupedItems[null] ?: emptyList()
                    if (uncategorizedItems.isNotEmpty()) {
                        item {
                            CategoryHeader("Uncategorized")
                        }
                        itemsIndexed(uncategorizedItems, key = { _, item -> item.id }) { index, item ->
                            GroceryItemRowContainer(
                                item = item,
                                currentPhase = state.currentPhase,
                                shoppingStoreId = state.shoppingStoreId,
                                itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                stores = stores,
                                categories = categories,
                                isEditMode = state.isEditMode,
                                index = index,
                                totalItems = uncategorizedItems.size,
                                onUpdateItem = { updatedItem ->
                                    viewModel.onEvent(GroceryUiEvent.UpdateItem(updatedItem))
                                },
                                onDeleteItem = {
                                    viewModel.onEvent(GroceryUiEvent.DeleteItem(item))
                                },
                                onUpdateStoreInfo = { info ->
                                    viewModel.onEvent(GroceryUiEvent.UpdateStoreInfo(info))
                                },
                                onMoveItem = { _, toIndex ->
                                    if (toIndex < index) {
                                        viewModel.onEvent(GroceryUiEvent.MoveItemUp(item, uncategorizedItems))
                                    } else {
                                        viewModel.onEvent(GroceryUiEvent.MoveItemDown(item, uncategorizedItems))
                                    }
                                },
                                onToggleBought = { groceryItem, isChecked ->
                                    viewModel.onEvent(GroceryUiEvent.ToggleBought(groceryItem, isChecked))
                                }
                            )
                        }
                    }

                    // "In Cart" category for Shopping mode - ALWAYS AT BOTTOM
                    if (state.currentPhase == GroceryPhase.SHOPPING) {
                        if (inCartItems.isNotEmpty()) {
                            item {
                                CategoryHeader("In Cart")
                            }
                            items(inCartItems, key = { it.id }) { item ->
                                GroceryItemRowContainer(
                                    item = item,
                                    currentPhase = state.currentPhase,
                                    shoppingStoreId = state.shoppingStoreId,
                                    itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                    stores = stores,
                                    categories = categories,
                                    isEditMode = state.isEditMode,
                                    index = 0,
                                    totalItems = 1,
                                    onUpdateItem = { updatedItem ->
                                        viewModel.onEvent(GroceryUiEvent.UpdateItem(updatedItem))
                                    },
                                    onDeleteItem = {
                                        viewModel.onEvent(GroceryUiEvent.DeleteItem(item))
                                    },
                                    onUpdateStoreInfo = { info ->
                                        viewModel.onEvent(GroceryUiEvent.UpdateStoreInfo(info))
                                    },
                                    onMoveItem = { _, _ -> },
                                    onToggleBought = { groceryItem, isChecked ->
                                        viewModel.onEvent(GroceryUiEvent.ToggleBought(groceryItem, isChecked))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (state.showRecommendedDialog) {
            RecommendedItemsDialog(
                recommendedItems = recommendedItems,
                activeItems = items,
                onDismiss = { viewModel.onEvent(GroceryUiEvent.SetShowRecommendedDialog(false)) },
                onAddItems = { selectedIds ->
                    viewModel.onEvent(GroceryUiEvent.AddRecommendedItems(selectedIds))
                }
            )
        }
    }
}

@Composable
fun CategoryHeader(name: String) {
    Text(
        text = name,
        color = Color.Gray,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
