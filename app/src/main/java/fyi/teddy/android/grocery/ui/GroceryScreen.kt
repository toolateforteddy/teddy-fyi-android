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
import fyi.teddy.android.grocery.ui.components.GroceryItemRowContainer
import fyi.teddy.android.grocery.ui.components.RecommendedItemsDialog
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
    
    val currentPhase by viewModel.currentPhase.collectAsState()
    val selectedStoreIds by viewModel.selectedStoreIds.collectAsState()
    val shoppingStoreId by viewModel.shoppingStoreId.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val showRecommendedDialog by viewModel.showRecommendedDialog.collectAsState()
    
    val selectedListId by viewModel.selectedListId.collectAsState()
    val lists by viewModel.lists.collectAsState()

    var showListSelectorMenu by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showShareListDialog by remember { mutableStateOf(false) }

    val newItemName by viewModel.newItemName.collectAsState()
    val newItemQuantity by viewModel.newItemQuantity.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    
    val items by viewModel.items.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val storeInfos by viewModel.storeInfos.collectAsState()
    val recommendedItems by viewModel.recommendedItems.collectAsState()

    val standardCategoryItems by viewModel.standardCategoryItems.collectAsState()
    val inCartItems by viewModel.inCartItems.collectAsState()
    
    val nameFocusRequester = remember { FocusRequester() }

    val uniqueNames = remember(items) {
        items.map { it.name }.distinct().sorted()
    }
    
    val suggestions = remember(newItemName, uniqueNames) {
        if (newItemName.length < 2) emptyList()
        else uniqueNames.filter { it.contains(newItemName, ignoreCase = true) && !it.equals(newItemName, ignoreCase = true) }
    }

    val onAddNewItem = {
        if (newItemName.isNotBlank()) {
            viewModel.insertItem(newItemName, newItemQuantity, selectedCategoryId)
            viewModel.setNewItemName("")
            viewModel.setNewItemQuantity("1")
            nameFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grocery: ${currentPhase.displayName}") },
                actions = {
                    if (currentPhase != GroceryPhase.SHOPPING) {
                        IconButton(onClick = { viewModel.setEditMode(!isEditMode) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    IconButton(onClick = onManageConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (currentPhase == GroceryPhase.SHOPPING) {
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
                                        viewModel.markDoneForTrip()
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
                    selected = currentPhase == GroceryPhase.NEED,
                    onClick = { viewModel.setPhase(GroceryPhase.NEED) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Need") },
                    label = { Text("Need") }
                )
                NavigationBarItem(
                    selected = currentPhase == GroceryPhase.PLANNING,
                    onClick = { viewModel.setPhase(GroceryPhase.PLANNING) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Planning") },
                    label = { Text("Planning") }
                )
                NavigationBarItem(
                    selected = currentPhase == GroceryPhase.SHOPPING,
                    onClick = { viewModel.setPhase(GroceryPhase.SHOPPING) },
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
                val activeList = lists.find { it.id == selectedListId }
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
                                    viewModel.setSelectedListId(null)
                                    showListSelectorMenu = false
                                }
                            )
                            lists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name) },
                                    onClick = {
                                        viewModel.setSelectedListId(list.id)
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
                        if (selectedListId != null) {
                            IconButton(onClick = { showShareListDialog = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Share List", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.deleteList(activeList!!) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete List", tint = Color.Red)
                            }
                        }
                    }
                }

                // Add List Dialog
                if (showAddListDialog) {
                    var newListName by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showAddListDialog = false },
                        title = { Text("Create New List") },
                        text = {
                            OutlinedTextField(
                                value = newListName,
                                onValueChange = { newListName = it },
                                label = { Text("List Name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newListName.isNotBlank()) {
                                        viewModel.insertList(newListName)
                                        showAddListDialog = false
                                    }
                                }
                            ) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddListDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // Share List Dialog
                if (showShareListDialog && selectedListId != null) {
                    val membersFlow = remember(selectedListId) { viewModel.getListMembers(selectedListId!!) }
                    val members by membersFlow.collectAsState(initial = emptyList())
                    var inviteUserId by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showShareListDialog = false },
                        title = { Text("Share '${activeListName}'") },
                        text = {
                            Column {
                                Text("Share this list with another user by entering their User ID:")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inviteUserId,
                                    onValueChange = { inviteUserId = it },
                                    label = { Text("User ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Current Members:", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (members.isEmpty()) {
                                    Text("Only you have access to this list.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                                        items(members) { member ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(member.userId, modifier = Modifier.weight(1f))
                                                IconButton(onClick = { viewModel.removeListMember(member) }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (inviteUserId.isNotBlank()) {
                                        viewModel.shareListWithUser(selectedListId!!, inviteUserId)
                                        inviteUserId = ""
                                    }
                                }
                            ) { Text("Share") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showShareListDialog = false }) { Text("Close") }
                        }
                    )
                }

                if (currentPhase == GroceryPhase.PLANNING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stores:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.setShowRecommendedDialog(true) },
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
                                selected = selectedStoreIds.contains(store.id),
                                onClick = {
                                    viewModel.toggleStoreSelection(store.id)
                                },
                                label = { Text(store.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (currentPhase == GroceryPhase.SHOPPING) {
                    if (stores.isNotEmpty()) {
                        Text("Shopping at:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        ScrollableTabRow(
                            selectedTabIndex = stores.indexOfFirst { it.id == shoppingStoreId }.coerceAtLeast(0),
                            containerColor = Color.Black,
                            edgePadding = 0.dp
                        ) {
                            stores.forEach { store ->
                                Tab(
                                    selected = shoppingStoreId == store.id,
                                    onClick = { viewModel.setShoppingStoreId(store.id) },
                                    text = { Text(store.name) }
                                )
                            }
                        }
                        if (shoppingStoreId == null) {
                            viewModel.setShoppingStoreId(stores.firstOrNull()?.id)
                        }
                    } else {
                        Text("No stores defined. Please add stores in settings.", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (currentPhase != GroceryPhase.SHOPPING) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = newItemName,
                                onValueChange = { viewModel.setNewItemName(it) },
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
                                value = newItemQuantity,
                                onValueChange = { viewModel.setNewItemQuantity(it) },
                                modifier = Modifier.weight(1f),
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
                                        selected = selectedCategoryId == null,
                                        onClick = { viewModel.setSelectedCategoryId(null) },
                                        label = { Text("No Category") }
                                    )
                                }
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategoryId == category.id,
                                        onClick = { viewModel.setSelectedCategoryId(category.id) },
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
                                                viewModel.updateItem(inactiveItem.copy(isActive = true))
                                            } else {
                                                viewModel.setNewItemName(suggestion)
                                            }
                                            viewModel.setNewItemName("")
                                            viewModel.setNewItemQuantity("1")
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
                                    currentPhase = currentPhase,
                                    shoppingStoreId = shoppingStoreId,
                                    itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                    stores = stores,
                                    categories = categories,
                                    isEditMode = isEditMode,
                                    index = index,
                                    totalItems = categoryItems.size,
                                    onUpdateItem = { updatedItem ->
                                        viewModel.updateItem(updatedItem)
                                    },
                                    onDeleteItem = {
                                        viewModel.deleteItem(item)
                                    },
                                    onUpdateStoreInfo = { info ->
                                        viewModel.updateStoreInfo(info)
                                    },
                                    onMoveItem = { _, toIndex ->
                                        val targetItem = categoryItems[toIndex]
                                        viewModel.swapItemPositions(item, targetItem)
                                    },
                                    onToggleBought = { groceryItem, isChecked ->
                                        viewModel.toggleBought(groceryItem, isChecked)
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
                                currentPhase = currentPhase,
                                shoppingStoreId = shoppingStoreId,
                                itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                stores = stores,
                                categories = categories,
                                isEditMode = isEditMode,
                                index = index,
                                totalItems = uncategorizedItems.size,
                                onUpdateItem = { updatedItem ->
                                    viewModel.updateItem(updatedItem)
                                },
                                onDeleteItem = {
                                    viewModel.deleteItem(item)
                                },
                                onUpdateStoreInfo = { info ->
                                    viewModel.updateStoreInfo(info)
                                },
                                onMoveItem = { _, toIndex ->
                                    val targetItem = uncategorizedItems[toIndex]
                                    viewModel.swapItemPositions(item, targetItem)
                                },
                                onToggleBought = { groceryItem, isChecked ->
                                    viewModel.toggleBought(groceryItem, isChecked)
                                }
                            )
                        }
                    }

                    // "In Cart" category for Shopping mode - ALWAYS AT BOTTOM
                    if (currentPhase == GroceryPhase.SHOPPING) {
                        if (inCartItems.isNotEmpty()) {
                            item {
                                CategoryHeader("In Cart")
                            }
                            items(inCartItems, key = { it.id }) { item ->
                                GroceryItemRowContainer(
                                    item = item,
                                    currentPhase = currentPhase,
                                    shoppingStoreId = shoppingStoreId,
                                    itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id },
                                    stores = stores,
                                    categories = categories,
                                    isEditMode = isEditMode,
                                    index = 0,
                                    totalItems = 1,
                                    onUpdateItem = { updatedItem ->
                                        viewModel.updateItem(updatedItem)
                                    },
                                    onDeleteItem = {
                                        viewModel.deleteItem(item)
                                    },
                                    onUpdateStoreInfo = { info ->
                                        viewModel.updateStoreInfo(info)
                                    },
                                    onMoveItem = { _, _ -> },
                                    onToggleBought = { groceryItem, isChecked ->
                                        viewModel.toggleBought(groceryItem, isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (showRecommendedDialog) {
            RecommendedItemsDialog(
                recommendedItems = recommendedItems,
                activeItems = items,
                onDismiss = { viewModel.setShowRecommendedDialog(false) },
                onAddItems = { selectedIds ->
                    viewModel.addRecommendedItems(selectedIds)
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
