package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.groceryDao()
    
    val items by dao.getAllItems().collectAsState(initial = emptyList())
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }

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
                title = { Text("Grocery List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items, key = { it.id }) { item ->
                        GroceryItemRow(
                            item = item,
                            onCheckedChange = { isChecked ->
                                scope.launch { dao.updateItem(item.copy(isBought = isChecked)) }
                            },
                            onDelete = {
                                scope.launch { dao.deleteItem(item) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }
    }
}
