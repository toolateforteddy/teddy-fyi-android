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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.Store
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.groceryDao()
    
    val stores by dao.getAllStores().collectAsState(initial = emptyList())
    var newStoreName by remember { mutableStateOf("") }

    val onAddStore = {
        if (newStoreName.isNotBlank()) {
            scope.launch {
                val maxPos = stores.maxByOrNull { it.position }?.position ?: -1
                dao.insertStore(Store(name = newStoreName, position = maxPos + 1))
                newStoreName = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Stores") },
                // Back arrow removed as requested
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
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
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(stores) { index, store ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(store.name, color = Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = store.isDefaultSupported,
                                        onCheckedChange = { isChecked ->
                                            scope.launch { dao.updateStore(store.copy(isDefaultSupported = isChecked)) }
                                        }
                                    )
                                    Text("Default ON for new items", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val prevStore = stores[index - 1]
                                        scope.launch {
                                            dao.updateStore(store.copy(position = prevStore.position))
                                            dao.updateStore(prevStore.copy(position = store.position))
                                        }
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                            }
                            
                            IconButton(
                                onClick = {
                                    if (index < stores.size - 1) {
                                        val nextStore = stores[index + 1]
                                        scope.launch {
                                            dao.updateStore(store.copy(position = nextStore.position))
                                            dao.updateStore(nextStore.copy(position = store.position))
                                        }
                                    }
                                },
                                enabled = index < stores.size - 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                            }

                            IconButton(onClick = {
                                scope.launch { dao.deleteStore(store) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
