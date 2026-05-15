package fyi.teddy.android.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fyi.teddy.android.todo.data.TodoDatabase
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TodoDatabase.getDatabase(context) }
    val dao = database.todoDao()
    
    val realItems by dao.getAllItems().collectAsState(initial = emptyList())
    var debugItems by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var isDebugMode by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    
    val currentItems = if (isDebugMode) debugItems else realItems
    var newItemTitle by remember { mutableStateOf("") }

    val onAddNewItem = {
        if (newItemTitle.isNotBlank()) {
            val titleToSave = newItemTitle
            if (isDebugMode) {
                debugItems = listOf(TodoItem(title = titleToSave)) + debugItems
            } else {
                scope.launch {
                    dao.insertItem(TodoItem(title = titleToSave))
                }
            }
            newItemTitle = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDebugMode) "Todo List (DEBUG)" else "Todo List") },
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
                                // Populate with a copy of real list
                                debugItems = realItems.toList()
                            }
                        }
                    )
                    
                    // Edit Mode Toggle
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Edit Mode",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Clear All (Debug utility)
                    IconButton(onClick = {
                        scope.launch {
                            if (isDebugMode) {
                                debugItems = emptyList()
                            } else {
                                dao.deleteAll()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red)
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
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentItems, key = { if (isDebugMode) it.hashCode() else it.id }) { item ->
                        TodoItemRow(
                            item = item,
                            showDelete = isEditMode,
                            onCheckedChange = { isChecked ->
                                if (isDebugMode) {
                                    debugItems = debugItems.map { 
                                        if (it == item) it.copy(isCompleted = isChecked) else it 
                                    }
                                } else {
                                    scope.launch { dao.updateItem(item.copy(isCompleted = isChecked)) }
                                }
                            },
                            onDelete = {
                                if (isDebugMode) {
                                    debugItems = debugItems.filter { it != item }
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
