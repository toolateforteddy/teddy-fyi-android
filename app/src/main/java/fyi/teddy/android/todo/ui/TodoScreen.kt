package fyi.teddy.android.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.todoDao()
    
    val realItems by dao.getAllItems().collectAsState(initial = emptyList())
    var debugItems by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var isDebugMode by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showCompletedOnly by remember { mutableStateOf(false) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    // Track recently completed items to hide them after 2 seconds
    val recentlyCompletedIds = remember { mutableStateListOf<Int>() }
    
    val baseItems = if (isDebugMode) debugItems else realItems
    
    val displayedItems = remember(baseItems, showCompletedOnly, recentlyCompletedIds.toList()) {
        if (showCompletedOnly) {
            baseItems.filter { it.isCompleted }
        } else {
            baseItems.filter { !it.isCompleted || recentlyCompletedIds.contains(it.id) }
        }
    }
    
    var newItemTitle by remember { mutableStateOf("") }

    val onAddNewItem = {
        if (newItemTitle.isNotBlank()) {
            val titleToSave = newItemTitle
            if (isDebugMode) {
                // For debug mode, we use a negative ID for newly added items to avoid clashes, 
                // but since it's just a copy, hashCode is fine for the key.
                debugItems = listOf(TodoItem(id = -(debugItems.size + 1), title = titleToSave)) + debugItems
            } else {
                scope.launch {
                    dao.insertItem(TodoItem(title = titleToSave))
                }
            }
            newItemTitle = ""
        }
    }

    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text("Clear All Tasks?") },
            text = { Text("This will permanently delete all tasks in the current ${if (isDebugMode) "sandbox" else "database"}. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (isDebugMode) {
                                debugItems = emptyList()
                            } else {
                                dao.deleteAll()
                            }
                            showClearAllConfirmation = false
                        }
                    }
                ) {
                    Text("Confirm Clear", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmation = false }) {
                    Text("Abort")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when {
                            isDebugMode -> "Todo (DEBUG)"
                            showCompletedOnly -> "Completed Tasks"
                            else -> "Todo List"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Debug Mode Toggle
                    Text("Debug", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = isDebugMode,
                        onCheckedChange = { 
                            isDebugMode = it
                            if (it) {
                                debugItems = realItems.toList()
                            }
                        }
                    )
                    
                    // Show Completed Toggle
                    IconButton(onClick = { showCompletedOnly = !showCompletedOnly }) {
                        Icon(
                            if (showCompletedOnly) Icons.Default.List else Icons.Default.CheckCircle, 
                            contentDescription = if (showCompletedOnly) "Show Active" else "Show Completed",
                            tint = if (showCompletedOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    // Edit Mode Toggle
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Edit Mode",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    
                    // Clear All (Only in Edit Mode)
                    if (isEditMode) {
                        IconButton(onClick = { showClearAllConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                if (!showCompletedOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add new task...", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1A1A1A),
                                unfocusedContainerColor = Color(0xFF1A1A1A),
                                cursorColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onAddNewItem() }
                            )
                        )
                        IconButton(onClick = { onAddNewItem() }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(displayedItems, key = { if (isDebugMode) it.hashCode() else it.id }) { item ->
                        TodoItemRow(
                            item = item,
                            showDelete = isEditMode,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !showCompletedOnly) {
                                    // Start the 2-second delay flow
                                    recentlyCompletedIds.add(item.id)
                                    scope.launch {
                                        delay(2000)
                                        recentlyCompletedIds.remove(item.id)
                                    }
                                } else if (!isChecked) {
                                    recentlyCompletedIds.remove(item.id)
                                }

                                if (isDebugMode) {
                                    debugItems = debugItems.map { 
                                        if (it.id == item.id) it.copy(isCompleted = isChecked) else it 
                                    }
                                } else {
                                    scope.launch { dao.updateItem(item.copy(isCompleted = isChecked)) }
                                }
                            },
                            onDelete = {
                                if (isDebugMode) {
                                    debugItems = debugItems.filter { it.id != item.id }
                                } else {
                                    scope.launch { dao.deleteItem(item) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    showDelete: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray)
        )
        Text(
            text = item.title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color = if (item.isCompleted) Color.Gray else Color.White,
            style = if (item.isCompleted) MaterialTheme.typography.bodyLarge.copy(
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
            ) else MaterialTheme.typography.bodyLarge
        )
        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
