package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.repository.GroceryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { GroceryRepository(database.groceryDao()) }
    
    val stores by repository.getAllStores().collectAsState(initial = emptyList())
    var newStoreName by remember { mutableStateOf("") }

    val onAddStore = {
        if (newStoreName.isNotBlank()) {
            val nameToSave = newStoreName
            scope.launch {
                repository.insertStore(Store(name = nameToSave))
            }
            newStoreName = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Stores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Store name...", color = Color.Gray) },
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
                        keyboardActions = KeyboardActions(onDone = { onAddStore() })
                    )
                    IconButton(onClick = { onAddStore() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(stores, key = { _, store -> store.id }) { index, store ->
                        StoreItemRow(
                            store = store,
                            onDelete = {
                                scope.launch { repository.deleteStore(store) }
                            },
                            onUpdate = { updatedStore ->
                                scope.launch { repository.updateStore(updatedStore) }
                            },
                            onMoveUp = {
                                val targetStore = stores[index - 1]
                                scope.launch {
                                    repository.swapStorePositions(store, targetStore)
                                }
                            },
                            onMoveDown = {
                                val targetStore = stores[index + 1]
                                scope.launch {
                                    repository.swapStorePositions(store, targetStore)
                                }
                            },
                            isFirst = index == 0,
                            isLast = index == stores.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreItemRow(
    store: Store,
    onDelete: () -> Unit,
    onUpdate: (Store) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = store.isDefaultSupported,
                        onCheckedChange = { onUpdate(store.copy(isDefaultSupported = it)) }
                    )
                    Text("Supported by default", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (isFirst) Color.Gray else Color.White)
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (isLast) Color.Gray else Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
            }
        }
    }
}
