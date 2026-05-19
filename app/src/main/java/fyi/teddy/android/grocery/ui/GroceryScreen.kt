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
import fyi.teddy.android.R
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.grocery.ui.components.GroceryItemRowContainer
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { GroceryRepository(database.groceryDao()) }
    
    val items by repository.getAllItems(userId).collectAsState(initial = emptyList())
    val stores by repository.getAllStores(userId).collectAsState(initial = emptyList())
    val categories by repository.getAllCategories(userId).collectAsState(initial = emptyList())
    val storeInfos by repository.getAllStoreInfo().collectAsState(initial = emptyList())
    val recommendedItems by repository.getRecommendedItems(userId).collectAsState(initial = emptyList())
    
    var currentPhase by remember { mutableStateOf(GroceryPhase.NEED) }
    var selectedStoreIds by remember { mutableStateOf(setOf<Int>()) }
    var shoppingStoreId by remember { mutableStateOf<Int?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showRecommendedDialog by remember { mutableStateOf(false) }
    
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(userId) {
        repository.claimEverything(userId)
    }

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
                val item = GroceryItem(
                    name = nameToSave,
                    quantity = quantityToSave,
                    categoryId = categoryToSave,
                    userId = userId
                )
                val itemId = repository.insertItem(item)
                
                stores.forEach { store ->
                    if (!store.isDefaultSupported) {
                        repository.insertStoreInfo(
                            GroceryItemStoreInfo(
                                groceryItemId = itemId.toInt(),
                                storeId = store.id,
                                isAvailable = false
                            )
                        )
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
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    IconButton(onClick = onManageConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stores:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showRecommendedDialog = true },
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
                            shoppingStoreId = stores.firstOrNull()?.id
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
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                )
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
                    val groupedItems = baseFilteredItems.groupBy { it.categoryId }
                    
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
                                        scope.launch { repository.updateItem(updatedItem) }
                                    },
                                    onDeleteItem = {
                                        scope.launch { repository.deleteItem(item) }
                                    },
                                    onUpdateStoreInfo = { info ->
                                        scope.launch { repository.insertStoreInfo(info) }
                                    },
                                    onMoveItem = { _, toIndex ->
                                        val targetItem = categoryItems[toIndex]
                                        scope.launch {
                                            repository.swapItemPositions(item, targetItem)
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
                                    scope.launch { repository.updateItem(updatedItem) }
                                },
                                onDeleteItem = {
                                    scope.launch { repository.deleteItem(item) }
                                },
                                onUpdateStoreInfo = { info ->
                                    scope.launch { repository.insertStoreInfo(info) }
                                },
                                onMoveItem = { _, toIndex ->
                                    val targetItem = uncategorizedItems[toIndex]
                                    scope.launch {
                                        repository.swapItemPositions(item, targetItem)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (showRecommendedDialog) {
            val unboughtNames = items.filter { !it.isBought }.map { it.name }.toSet()
            val availableRecommendations = recommendedItems.filter { !unboughtNames.contains(it.name) }
            val selectedItemIds = remember { mutableStateListOf<Int>() }

            AlertDialog(
                onDismissRequest = { showRecommendedDialog = false },
                title = { Text("Recommended Items") },
                text = {
                    if (availableRecommendations.isEmpty()) {
                        Text("No recommendations yet. Buy items to see them here!")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(availableRecommendations) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        if (selectedItemIds.contains(item.id)) selectedItemIds.remove(item.id)
                                        else selectedItemIds.add(item.id)
                                    }
                                ) {
                                    Checkbox(
                                        checked = selectedItemIds.contains(item.id), 
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) selectedItemIds.add(item.id)
                                            else selectedItemIds.remove(item.id)
                                        } 
                                    )
                                    Text(item.name, modifier = Modifier.weight(1f))
                                    Text("(${item.timesBought})", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (availableRecommendations.isNotEmpty()) {
                        TextButton(onClick = {
                            scope.launch {
                                availableRecommendations.filter { selectedItemIds.contains(it.id) }.forEach { item ->
                                    repository.updateItem(item.copy(isBought = false))
                                }
                                showRecommendedDialog = false
                            }
                        }) { Text(stringResource(R.string.add)) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecommendedDialog = false }) { Text(stringResource(R.string.cancel)) }
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
