package fyi.teddy.android.todo.ui

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import java.util.*

enum class TodoMode {
    NORMAL, TODAY_PLANNING, TODAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(userId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.todoDao()
    
    var currentMode by remember { mutableStateOf(TodoMode.NORMAL) }
    
    val realItems by dao.getAllItems(userId).collectAsState(initial = emptyList())
    val todayItems by dao.getTodayItems(userId).collectAsState(initial = emptyList())
    
    var isEditMode by remember { mutableStateOf(false) }
    var showCompletedOnly by remember { mutableStateOf(false) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    // Confetti state
    val parties = remember { mutableStateListOf<Party>() }

    // Track recently completed items to hide them after 2 seconds
    val recentlyCompletedIds = remember { mutableStateListOf<Int>() }
    
    val baseItems = if (currentMode == TodoMode.TODAY) todayItems else realItems
    
    // Check for midnight reset on load and claim unowned items
    LaunchedEffect(userId) {
        dao.claimUnownedItems(userId)
        
        val sharedPref = context.getSharedPreferences("todo_prefs", android.content.Context.MODE_PRIVATE)
        val lastReset = sharedPref.getLong("last_reset_day", 0)
        val currentDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        if (currentDay > lastReset) {
            dao.resetPlannedItems(userId)
            sharedPref.edit().putLong("last_reset_day", currentDay).apply()
        }
    }
    
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
            scope.launch {
                // To avoid the "hidden new item" bug even further, 
                // ensure the new item has a smaller position than the smallest if we want it at top,
                // or just follow the DAO query fix.
                val maxPos = realItems.maxByOrNull { it.position }?.position ?: -1
                dao.insertItem(TodoItem(title = titleToSave, position = maxPos + 1, userId = userId))
            }
            newItemTitle = ""
        }
    }

    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text("Clear All Tasks?") },
            text = { Text("This will permanently delete all tasks in the database. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dao.deleteAll(userId)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = when {
                                currentMode == TodoMode.TODAY_PLANNING -> "Planning Today"
                                currentMode == TodoMode.TODAY -> "Today's Tasks"
                                showCompletedOnly -> "Completed Tasks"
                                else -> "Todo List"
                            }
                        ) 
                    },
                    actions = {
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
            },
            bottomBar = {
                NavigationBar(containerColor = Color.Black) {
                    NavigationBarItem(
                        selected = currentMode == TodoMode.NORMAL,
                        onClick = { currentMode = TodoMode.NORMAL },
                        icon = { Icon(Icons.Default.List, contentDescription = "Normal") },
                        label = { Text("Normal") }
                    )
                    NavigationBarItem(
                        selected = currentMode == TodoMode.TODAY_PLANNING,
                        onClick = { currentMode = TodoMode.TODAY_PLANNING },
                        icon = { Icon(Icons.Default.EditCalendar, contentDescription = "Planning") },
                        label = { Text("Planning") }
                    )
                    NavigationBarItem(
                        selected = currentMode == TodoMode.TODAY,
                        onClick = { currentMode = TodoMode.TODAY },
                        icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                        label = { Text("Today") }
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
                    if (!showCompletedOnly && currentMode != TodoMode.TODAY && !isEditMode) {
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
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
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
                        itemsIndexed(displayedItems, key = { _, it -> it.id }) { index, item ->
                            TodoItemRow(
                                item = item,
                                showDelete = isEditMode,
                                isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                index = index,
                                totalItems = displayedItems.size,
                                onCheckedChange = { isChecked ->
                                    if (currentMode == TodoMode.TODAY_PLANNING) {
                                        scope.launch { dao.updateItem(item.copy(isPlannedForToday = isChecked)) }
                                        return@TodoItemRow
                                    }

                                    if (isChecked && !showCompletedOnly) {
                                        recentlyCompletedIds.add(item.id)
                                        val party = Party(
                                            speed = 0f,
                                            maxSpeed = 30f,
                                            damping = 0.9f,
                                            spread = 360,
                                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                                            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                                            position = Position.Relative(0.5, 0.3)
                                        )
                                        parties.add(party)
                                        
                                        scope.launch {
                                            delay(2000)
                                            recentlyCompletedIds.remove(item.id)
                                            
                                            // Handle recurrence
                                            if (item.recurrenceIntervalDays != null) {
                                                val nextTime = System.currentTimeMillis() + item.recurrenceIntervalDays * 24 * 60 * 60 * 1000L
                                                dao.updateItem(item.copy(
                                                    isCompleted = false, 
                                                    scheduledAt = nextTime,
                                                    isPlannedForToday = false
                                                ))
                                            }
                                            
                                            delay(1000) 
                                            parties.remove(party)
                                        }
                                    } else if (!isChecked) {
                                        recentlyCompletedIds.remove(item.id)
                                    }

                                    scope.launch { dao.updateItem(item.copy(isCompleted = isChecked)) }
                                },
                                onDelete = {
                                    scope.launch { dao.deleteItem(item) }
                                },
                                onUpdateItem = { updatedItem ->
                                    scope.launch { dao.updateItem(updatedItem) }
                                },
                                onMoveToTop = {
                                    scope.launch {
                                        val minPos = realItems.minByOrNull { it.position }?.position ?: 0
                                        dao.updateItem(item.copy(position = minPos - 1))
                                    }
                                },
                                onMoveToBottom = {
                                    scope.launch {
                                        val maxPos = realItems.maxByOrNull { it.position }?.position ?: 0
                                        dao.updateItem(item.copy(position = maxPos + 1))
                                    }
                                },
                                onMoveUp = {
                                    if (index > 0) {
                                        val prevItem = displayedItems[index - 1]
                                        scope.launch {
                                            if (item.position == prevItem.position) {
                                                // Force distinct positions if they are equal (migration artifact)
                                                dao.updateItem(item.copy(position = prevItem.position - 1))
                                            } else {
                                                val oldPos = item.position
                                                dao.updateItem(item.copy(position = prevItem.position))
                                                dao.updateItem(prevItem.copy(position = oldPos))
                                            }
                                        }
                                    }
                                },
                                onMoveDown = {
                                    if (index < displayedItems.size - 1) {
                                        val nextItem = displayedItems[index + 1]
                                        scope.launch {
                                            if (item.position == nextItem.position) {
                                                // Force distinct positions
                                                dao.updateItem(item.copy(position = nextItem.position + 1))
                                            } else {
                                                val oldPos = item.position
                                                dao.updateItem(item.copy(position = nextItem.position))
                                                dao.updateItem(nextItem.copy(position = oldPos))
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Confetti layer
        if (parties.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = parties,
            )
        }
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    showDelete: Boolean,
    isPlanningMode: Boolean,
    index: Int,
    totalItems: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdateItem: (TodoItem) -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRecurrenceDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = if (isPlanningMode) item.isPlannedForToday else item.isCompleted,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                uncheckedColor = if (isPlanningMode) Color.Cyan else Color.Gray,
                checkedColor = if (isPlanningMode) Color.Cyan else MaterialTheme.colorScheme.primary
            )
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = item.title,
                color = if (!isPlanningMode && item.isCompleted) Color.Gray else Color.White,
                style = if (!isPlanningMode && item.isCompleted) MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                ) else MaterialTheme.typography.bodyLarge
            )
            if (item.recurrenceIntervalDays != null) {
                Text(
                    text = "Every ${item.recurrenceIntervalDays} days",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        if (showDelete) {
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(
                    Icons.Default.KeyboardArrowUp, 
                    contentDescription = "Move Up", 
                    tint = if (index > 0) Color.White else Color.Transparent
                )
            }
            IconButton(onClick = onMoveDown, enabled = index < totalItems - 1) {
                Icon(
                    Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Move Down", 
                    tint = if (index < totalItems - 1) Color.White else Color.Transparent
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Title") },
                        onClick = {
                            showEditTitleDialog = true
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recurrence") },
                        onClick = {
                            showRecurrenceDialog = true
                            showMenu = false
                        }
                    )
                    Divider()
                    if (index > 0) {
                        DropdownMenuItem(
                            text = { Text("Move to Top") },
                            onClick = {
                                onMoveToTop()
                                showMenu = false
                            }
                        )
                    }
                    if (index < totalItems - 1) {
                        DropdownMenuItem(
                            text = { Text("Move to Bottom") },
                            onClick = {
                                onMoveToBottom()
                                showMenu = false
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }

    if (showRecurrenceDialog) {
        var daysText by remember { mutableStateOf(item.recurrenceIntervalDays?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showRecurrenceDialog = false },
            title = { Text("Set Recurrence") },
            text = {
                Column {
                    Text("Re-schedule this task X days after completion.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        label = { Text("Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = daysText.toIntOrNull()
                    onUpdateItem(item.copy(recurrenceIntervalDays = days))
                    showRecurrenceDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateItem(item.copy(recurrenceIntervalDays = null))
                    showRecurrenceDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }

    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Task Title") },
            text = {
                TextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Title") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editedTitle.isNotBlank()) {
                        onUpdateItem(item.copy(title = editedTitle))
                        showEditTitleDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
