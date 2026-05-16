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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import kotlinx.coroutines.launch
import java.util.*

enum class GroceryPhase {
    NEED, PLANNING, SHOPPING;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroceryScreen(onBack: () -> Unit, onManageStores: () -> Unit, onManageCategories: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.groceryDao()
    
    val items by dao.getAllItems().collectAsState(initial = emptyList())
    val stores by dao.getAllStores().collectAsState(initial = emptyList())
    val categories by dao.getAllCategories().collectAsState(initial = emptyList())
    val storeInfos by dao.getAllStoreInfo().collectAsState(initial = emptyList())
    
    var currentPhase by remember { mutableStateOf(GroceryPhase.NEED) }
    var selectedStoreIds by remember { mutableStateOf(setOf<Int>()) }
    var shoppingStoreId by remember { mutableStateOf<Int?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    
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
            val nameToSave = newItemName
            val quantityToSave = newItemQuantity
            val categoryToSave = selectedCategoryId
            scope.launch {
                val maxPos = items.maxByOrNull { it.position }?.position ?: -1
                val itemId = dao.insertItem(GroceryItem(name = nameToSave, quantity = quantityToSave, position = maxPos + 1, categoryId = categoryToSave))
                
                stores.forEach { store ->
                    if (!store.isDefaultSupported) {
                        dao.insertStoreInfo(GroceryItemStoreInfo(groceryItemId = itemId.toInt(), storeId = store.id, isAvailable = false))
                    }
                }
                
                nameFocusRequester.requestFocus()
            }
            newItemName = ""
            newItemQuantity = "1"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grocery: ${currentPhase.displayName}") },
                actions = {
                    if (currentPhase != GroceryPhase.SHOPPING) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Edit Mode",
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.List, contentDescription = "Manage Categories")
                    }
                    IconButton(onClick = onManageStores) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Stores")
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
                    onClick = { currentPhase = GroceryPhase.NEED },
                    icon = { Icon(Icons.Default.List, contentDescription = "Need") },
                    label = { Text("Need") }
                )
                NavigationBarItem(
                    selected = currentPhase == GroceryPhase.PLANNING,
                    onClick = { currentPhase = GroceryPhase.PLANNING },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Planning") },
                    label = { Text("Planning") }
                )
                NavigationBarItem(
                    selected = currentPhase == GroceryPhase.SHOPPING,
                    onClick = { currentPhase = GroceryPhase.SHOPPING },
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
                if (currentPhase == GroceryPhase.PLANNING) {
                    Text("Select Stores to Plan for:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stores.forEach { store ->
                            FilterChip(
                                selected = selectedStoreIds.contains(store.id),
                                onClick = {
                                    selectedStoreIds = if (selectedStoreIds.contains(store.id)) {
                                        selectedStoreIds - store.id
                                    } else {
                                        selectedStoreIds + store.id
                                    }
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
                                    onClick = { shoppingStoreId = store.id },
                                    text = { Text(store.name) }
                                )
                            }
                        }
                        if (shoppingStoreId == null) {
                            shoppingStoreId = stores.first().id
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
                                onValueChange = { newItemName = it },
                                modifier = Modifier.weight(2f).focusRequester(nameFocusRequester),
                                placeholder = { Text("Item name...", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1A1A1A),
                                    unfocusedContainerColor = Color(0xFF1A1A1A)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = newItemQuantity,
                                onValueChange = { newItemQuantity = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Qty", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1A1A1A),
                                    unfocusedContainerColor = Color(0xFF1A1A1A)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onAddNewItem() })
                            )
                            IconButton(onClick = { onAddNewItem() }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
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
                                        onClick = { selectedCategoryId = null },
                                        label = { Text("No Category") }
                                    )
                                }
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategoryId == category.id,
                                        onClick = { selectedCategoryId = category.id },
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
                                        onClick = { newItemName = suggestion },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val baseFilteredItems = remember(items, storeInfos, currentPhase, selectedStoreIds, shoppingStoreId) {
                    when (currentPhase) {
                        GroceryPhase.NEED -> items
                        GroceryPhase.PLANNING -> {
                            if (selectedStoreIds.isEmpty()) items
                            else {
                                items.filter { item ->
                                    val infos = storeInfos.filter { it.groceryItemId == item.id }
                                    selectedStoreIds.any { storeId ->
                                        val info = infos.find { it.storeId == storeId }
                                        info?.isAvailable ?: true
                                    }
                                }
                            }
                        }
                        GroceryPhase.SHOPPING -> {
                            if (shoppingStoreId == null) emptyList()
                            else {
                                items.filter { item ->
                                    val info = storeInfos.find { it.groceryItemId == item.id && it.storeId == shoppingStoreId }
                                    info?.isAvailable ?: true
                                }
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Group items by category
                    val groupedItems = baseFilteredItems.groupBy { it.categoryId }
                    
                    // Show uncategorized first or at the end? Let's show categories first.
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
                                        scope.launch { dao.updateItem(updatedItem) }
                                    },
                                    onDeleteItem = {
                                        scope.launch { dao.deleteItem(item) }
                                    },
                                    onUpdateStoreInfo = { info ->
                                        scope.launch { dao.insertStoreInfo(info) }
                                    },
                                    onMoveItem = { fromIndex, toIndex ->
                                        val targetItem = categoryItems[toIndex]
                                        scope.launch {
                                            dao.updateItem(item.copy(position = targetItem.position))
                                            dao.updateItem(targetItem.copy(position = item.position))
                                        }
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
                                    scope.launch { dao.updateItem(updatedItem) }
                                },
                                onDeleteItem = {
                                    scope.launch { dao.deleteItem(item) }
                                },
                                onUpdateStoreInfo = { info ->
                                    scope.launch { dao.insertStoreInfo(info) }
                                },
                                onMoveItem = { fromIndex, toIndex ->
                                    val targetItem = uncategorizedItems[toIndex]
                                    scope.launch {
                                        dao.updateItem(item.copy(position = targetItem.position))
                                        dao.updateItem(targetItem.copy(position = item.position))
                                    }
                                }
                            )
                        }
                    }
                }
            }
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

@Composable
fun GroceryItemRowContainer(
    item: GroceryItem,
    currentPhase: GroceryPhase,
    shoppingStoreId: Int?,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    stores: List<Store>,
    categories: List<Category>,
    isEditMode: Boolean,
    index: Int,
    totalItems: Int,
    onUpdateItem: (GroceryItem) -> Unit,
    onDeleteItem: () -> Unit,
    onUpdateStoreInfo: (GroceryItemStoreInfo) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showStoreTagging by remember { mutableStateOf(false) }
    var showEditQuantity by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf(false) }

    GroceryItemRow(
        item = item,
        currentPhase = currentPhase,
        shoppingStoreId = shoppingStoreId,
        itemStoreInfos = itemStoreInfos,
        stores = stores,
        isEditMode = isEditMode,
        onCheckedChange = { isChecked ->
            onUpdateItem(item.copy(isBought = isChecked))
        },
        onDelete = onDeleteItem,
        onTagStores = { showStoreTagging = true },
        onEditQuantity = { showEditQuantity = true },
        onEditCategory = { showEditCategory = true },
        onUpdatePrice = { storeId, price ->
            val currentInfo = itemStoreInfos.find { it.storeId == storeId }
            onUpdateStoreInfo(
                currentInfo?.copy(price = price) 
                    ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, price = price)
            )
        },
        onMoveUp = { if (index > 0) onMoveItem(index, index - 1) },
        onMoveDown = { if (index < totalItems - 1) onMoveItem(index, index + 1) }
    )
    
    if (showStoreTagging) {
        StoreTaggingDialog(
            stores = stores,
            itemStoreInfos = itemStoreInfos,
            onDismiss = { showStoreTagging = false },
            onToggleAvailability = { storeId, isAvailable ->
                val currentInfo = itemStoreInfos.find { it.storeId == storeId }
                onUpdateStoreInfo(
                    currentInfo?.copy(isAvailable = isAvailable)
                        ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, isAvailable = isAvailable)
                )
            }
        )
    }

    if (showEditQuantity) {
        var editedQuantity by remember { mutableStateOf(item.quantity) }
        AlertDialog(
            onDismissRequest = { showEditQuantity = false },
            title = { Text("Edit Quantity") },
            text = {
                TextField(
                    value = editedQuantity,
                    onValueChange = { editedQuantity = it },
                    label = { Text("Quantity") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateItem(item.copy(quantity = editedQuantity))
                    showEditQuantity = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditQuantity = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditCategory) {
        AlertDialog(
            onDismissRequest = { showEditCategory = false },
            title = { Text("Change Category") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onUpdateItem(item.copy(categoryId = null))
                            showEditCategory = false
                        }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = item.categoryId == null, onClick = null)
                        Text("No Category", modifier = Modifier.padding(start = 8.dp))
                    }
                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdateItem(item.copy(categoryId = category.id))
                                showEditCategory = false
                            }.padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = item.categoryId == category.id, onClick = null)
                            Text(category.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditCategory = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    currentPhase: GroceryPhase,
    shoppingStoreId: Int?,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    stores: List<Store>,
    isEditMode: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTagStores: () -> Unit,
    onEditQuantity: () -> Unit,
    onEditCategory: () -> Unit,
    onUpdatePrice: (Int, Double) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showPriceInput by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }

    val minPriceInfo = itemStoreInfos.filter { it.price != null }.minByOrNull { it.price!! }
    val shoppingStoreInfo = itemStoreInfos.find { it.storeId == shoppingStoreId }
    
    val isMoreExpensive = shoppingStoreId != null && 
                          shoppingStoreInfo?.price != null && 
                          minPriceInfo?.price != null && 
                          shoppingStoreInfo.price!! > minPriceInfo.price!!

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
            if (currentPhase == GroceryPhase.SHOPPING) showPriceInput = !showPriceInput
            else if (!isEditMode) onTagStores() 
        },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentPhase == GroceryPhase.SHOPPING) {
                    Checkbox(
                        checked = item.isBought,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray)
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = item.name,
                        color = if (item.isBought) Color.Gray else Color.White,
                        style = if (item.isBought) MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        ) else MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { if (currentPhase != GroceryPhase.SHOPPING) onEditCategory() }
                    )
                    Text(
                        text = "Quantity: ${item.quantity}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { if (currentPhase != GroceryPhase.SHOPPING) onEditQuantity() }
                    )
                    
                    if (isMoreExpensive) {
                        val cheaperStoreName = stores.find { it.id == minPriceInfo?.storeId }?.name ?: "another store"
                        Text(
                            text = "Note: $cheaperStoreName is cheaper ($${minPriceInfo?.price})",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                if (isEditMode) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
            
            if (showPriceInput && shoppingStoreId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter price paid...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            priceText.toDoubleOrNull()?.let { onUpdatePrice(shoppingStoreId, it) }
                            showPriceInput = false
                        })
                    )
                    Button(onClick = {
                        priceText.toDoubleOrNull()?.let { onUpdatePrice(shoppingStoreId, it) }
                        showPriceInput = false
                    }) {
                        Text("Save")
                    }
                }
                
                if (itemStoreInfos.isNotEmpty()) {
                    Text("Price History:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    itemStoreInfos.filter { it.price != null }.forEach { info ->
                        val storeName = stores.find { it.id == info.storeId }?.name ?: "Unknown"
                        Text("- $storeName: $${info.price}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StoreTaggingDialog(
    stores: List<Store>,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    onDismiss: () -> Unit,
    onToggleAvailability: (Int, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available at Stores") },
        text = {
            Column {
                stores.forEach { store ->
                    val info = itemStoreInfos.find { it.storeId == store.id }
                    val isAvailable = info?.isAvailable ?: true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAvailable,
                            onCheckedChange = { onToggleAvailability(store.id, it) }
                        )
                        Text(store.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
