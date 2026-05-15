package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fyi.teddy.android.data.AppDatabase
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryScreen(onBack: () -> Unit, onManageStores: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.groceryDao()
    
    val items by dao.getAllItems().collectAsState(initial = emptyList())
    val stores by dao.getAllStores().collectAsState(initial = emptyList())
    val storeInfos by dao.getAllStoreInfo().collectAsState(initial = emptyList())
    
    var currentPhase by remember { mutableStateOf(GroceryPhase.NEED) }
    var selectedStoreIds by remember { mutableStateOf(setOf<Int>()) }
    var shoppingStoreId by remember { mutableStateOf<Int?>(null) }
    
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }

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
            scope.launch {
                dao.insertItem(GroceryItem(name = nameToSave, quantity = quantityToSave))
            }
            newItemName = ""
            newItemQuantity = "1"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grocery: ${currentPhase.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onManageStores) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Stores")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.weight(2f),
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

                val filteredItems = remember(items, storeInfos, currentPhase, selectedStoreIds, shoppingStoreId) {
                    when (currentPhase) {
                        GroceryPhase.NEED -> items
                        GroceryPhase.PLANNING -> {
                            if (selectedStoreIds.isEmpty()) items
                            else {
                                items.filter { item ->
                                    val infos = storeInfos.filter { it.groceryItemId == item.id }
                                    selectedStoreIds.any { storeId ->
                                        val info = infos.find { it.storeId == storeId }
                                        info?.isAvailable ?: true // Default available
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
                    items(filteredItems, key = { it.id }) { item ->
                        var showStoreTagging by remember { mutableStateOf(false) }
                        
                        val itemStoreInfos = storeInfos.filter { it.groceryItemId == item.id }

                        GroceryItemRow(
                            item = item,
                            currentPhase = currentPhase,
                            shoppingStoreId = shoppingStoreId,
                            itemStoreInfos = itemStoreInfos,
                            stores = stores,
                            onCheckedChange = { isChecked ->
                                scope.launch { dao.updateItem(item.copy(isBought = isChecked)) }
                            },
                            onDelete = {
                                scope.launch { dao.deleteItem(item) }
                            },
                            onTagStores = { showStoreTagging = true },
                            onUpdatePrice = { storeId, price ->
                                scope.launch {
                                    val currentInfo = itemStoreInfos.find { it.storeId == storeId }
                                    dao.insertStoreInfo(
                                        currentInfo?.copy(price = price) 
                                            ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, price = price)
                                    )
                                }
                            }
                        )
                        
                        if (showStoreTagging) {
                            StoreTaggingDialog(
                                item = item,
                                stores = stores,
                                itemStoreInfos = itemStoreInfos,
                                onDismiss = { showStoreTagging = false },
                                onToggleAvailability = { storeId, isAvailable ->
                                    scope.launch {
                                        val currentInfo = itemStoreInfos.find { it.storeId == storeId }
                                        dao.insertStoreInfo(
                                            currentInfo?.copy(isAvailable = isAvailable)
                                                ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, isAvailable = isAvailable)
                                        )
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
fun GroceryItemRow(
    item: GroceryItem,
    currentPhase: GroceryPhase,
    shoppingStoreId: Int?,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    stores: List<Store>,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTagStores: () -> Unit,
    onUpdatePrice: (Int, Double) -> Unit
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
            else onTagStores() 
        },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isBought,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray)
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = item.name,
                        color = if (item.isBought) Color.Gray else Color.White,
                        style = if (item.isBought) MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        ) else MaterialTheme.typography.bodyLarge
                    )
                    if (item.quantity.isNotBlank() && item.quantity != "1") {
                        Text(
                            text = "Quantity: ${item.quantity}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (isMoreExpensive) {
                        val cheaperStoreName = stores.find { it.id == minPriceInfo?.storeId }?.name ?: "another store"
                        Text(
                            text = "Note: $cheaperStoreName is cheaper ($${minPriceInfo?.price})",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (currentPhase != GroceryPhase.SHOPPING) {
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
    item: GroceryItem,
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
