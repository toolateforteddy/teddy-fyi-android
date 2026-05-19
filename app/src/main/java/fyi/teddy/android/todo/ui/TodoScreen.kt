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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.R
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.ui.components.AddSubtaskDialog
import fyi.teddy.android.todo.ui.components.EditTitleDialog
import fyi.teddy.android.todo.ui.components.RecurrenceDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class TodoMode {
    BACKLOG, TODAY_PLANNING, TODAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(userId: String, onBack: () -> Unit) {
    // ...
    var currentMode by remember { mutableStateOf(TodoMode.BACKLOG) }
    // ...
    
    val baseItems = when(currentMode) {
        TodoMode.TODAY -> todayItems
        TodoMode.BACKLOG -> allItems.filter { !it.isPlannedForToday }
        TodoMode.TODAY_PLANNING -> allItems
    }
    // ...

    NavigationBar(containerColor = Color.Black) {
        NavigationBarItem(
            selected = currentMode == TodoMode.BACKLOG,
            onClick = { currentMode = TodoMode.BACKLOG },
            icon = { Icon(Icons.Default.List, contentDescription = "Backlog") },
            label = { Text("Backlog") }
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
                                    onDone = { 
                                        onAddNewItem(newItemTitle, null)
                                        newItemTitle = ""
                                    }
                                )
                            )
                            IconButton(onClick = { 
                                onAddNewItem(newItemTitle, null)
                                newItemTitle = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        groupedItems.forEachIndexed { parentIndex, (parent, children) ->
                            item(key = parent.id) {
                                val isExpanded = expandedParentIds.value.contains(parent.id)
                                TodoItemRow(
                                    item = parent,
                                    subtaskCount = children.size,
                                    completedSubtaskCount = children.count { it.isCompleted },
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedParentIds.value = if (isExpanded) {
                                            expandedParentIds.value - parent.id
                                        } else {
                                            expandedParentIds.value + parent.id
                                        }
                                    },
                                    showDelete = isEditMode,
                                    isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                    index = parentIndex,
                                    totalItems = groupedItems.size,
                                    onCheckedChange = { isChecked ->
                                        if (currentMode == TodoMode.TODAY_PLANNING) {
                                            scope.launch { 
                                                repository.updateItem(parent.copy(isPlannedForToday = isChecked))
                                                if (isChecked) {
                                                    children.forEach { 
                                                        repository.updateItem(it.copy(isPlannedForToday = true))
                                                    }
                                                }
                                            }
                                            return@TodoItemRow
                                        }

                                        if (isChecked && !showCompletedOnly) {
                                            recentlyCompletedIds.add(parent.id)
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
                                                recentlyCompletedIds.remove(parent.id)
                                                if (parent.recurrenceIntervalDays != null) {
                                                    val nextTime = System.currentTimeMillis() + parent.recurrenceIntervalDays * 24 * 60 * 60 * 1000L
                                                    repository.updateItem(parent.copy(isCompleted = false, scheduledAt = nextTime, isPlannedForToday = false))
                                                }
                                                delay(1000) 
                                                parties.remove(party)
                                            }
                                        } else if (!isChecked) {
                                            recentlyCompletedIds.remove(parent.id)
                                        }
                                        scope.launch { repository.updateItem(parent.copy(isCompleted = isChecked)) }
                                    },
                                    onDelete = { scope.launch { repository.deleteItem(parent) } },
                                    onUpdateItem = { scope.launch { repository.updateItem(it) } },
                                    onAddSubtask = { title -> onAddNewItem(title, parent.id) },
                                    onMoveToTop = {
                                        scope.launch {
                                            val minPos = allItems.filter { it.parentId == null }.minByOrNull { it.position }?.position ?: 0
                                            repository.updateItem(parent.copy(position = minPos - 1))
                                        }
                                    },
                                    onMoveToBottom = {
                                        scope.launch {
                                            val maxPos = allItems.filter { it.parentId == null }.maxByOrNull { it.position }?.position ?: 0
                                            repository.updateItem(parent.copy(position = maxPos + 1))
                                        }
                                    },
                                    onMoveUp = {
                                        if (parentIndex > 0) {
                                            val prevParent = groupedItems[parentIndex - 1].first
                                            scope.launch {
                                                repository.swapPositions(parent, prevParent)
                                            }
                                        }
                                    },
                                    onMoveDown = {
                                        if (parentIndex < groupedItems.size - 1) {
                                            val nextParent = groupedItems[parentIndex + 1].first
                                            scope.launch {
                                                repository.swapPositions(parent, nextParent)
                                            }
                                        }
                                    }
                                )
                            }
                            
                            if (expandedParentIds.value.contains(parent.id) || currentMode == TodoMode.TODAY) {
                                itemsIndexed(children, key = { _, it -> it.id }) { childIndex, child ->
                                    TodoItemRow(
                                        item = child,
                                        isSubtask = true,
                                        showDelete = isEditMode,
                                        isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                        index = childIndex,
                                        totalItems = children.size,
                                        onCheckedChange = { isChecked ->
                                            if (currentMode == TodoMode.TODAY_PLANNING) {
                                                scope.launch { repository.updateItem(child.copy(isPlannedForToday = isChecked)) }
                                                return@TodoItemRow
                                            }
                                            if (isChecked && !showCompletedOnly) {
                                                recentlyCompletedIds.add(child.id)
                                                scope.launch {
                                                    delay(2000)
                                                    recentlyCompletedIds.remove(child.id)
                                                }
                                            } else if (!isChecked) {
                                                recentlyCompletedIds.remove(child.id)
                                            }
                                            scope.launch { repository.updateItem(child.copy(isCompleted = isChecked)) }
                                        },
                                        onDelete = { scope.launch { repository.deleteItem(child) } },
                                        onUpdateItem = { scope.launch { repository.updateItem(it) } },
                                        onMoveToTop = {
                                            scope.launch {
                                                val minPos = children.minByOrNull { it.position }?.position ?: 0
                                                repository.updateItem(child.copy(position = minPos - 1))
                                            }
                                        },
                                        onMoveToBottom = {
                                            scope.launch {
                                                val maxPos = children.maxByOrNull { it.position }?.position ?: 0
                                                repository.updateItem(child.copy(position = maxPos + 1))
                                            }
                                        },
                                        onMoveUp = {
                                            if (childIndex > 0) {
                                                val prevChild = children[childIndex - 1]
                                                scope.launch {
                                                    repository.swapPositions(child, prevChild)
                                                }
                                            }
                                        },
                                        onMoveDown = {
                                            if (childIndex < children.size - 1) {
                                                val nextChild = children[childIndex + 1]
                                                scope.launch {
                                                    repository.swapPositions(child, nextChild)
                                                }
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
        
        if (parties.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = parties,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItemRow(
    item: TodoItem,
    isSubtask: Boolean = false,
    subtaskCount: Int = 0,
    completedSubtaskCount: Int = 0,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    showDelete: Boolean,
    isPlanningMode: Boolean,
    index: Int,
    totalItems: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdateItem: (TodoItem) -> Unit,
    onAddSubtask: (String) -> Unit = {},
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRecurrenceDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showAddSubtaskDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSubtask) {
            Spacer(modifier = Modifier.width(16.dp))
            Text("-", color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
        }

        Checkbox(
            checked = if (isPlanningMode) item.isPlannedForToday else item.isCompleted,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                uncheckedColor = if (isPlanningMode) Color.Cyan else Color.Gray,
                checkedColor = if (isPlanningMode) Color.Cyan else MaterialTheme.colorScheme.primary
            )
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = if (!isPlanningMode && item.isCompleted) Color.Gray else Color.White,
                    style = if (!isPlanningMode && item.isCompleted) MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    ) else MaterialTheme.typography.bodyLarge
                )
                if (item.isDaily) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Daily", 
                        tint = Color.Cyan, 
                        modifier = Modifier.padding(start = 4.dp).size(14.dp)
                    )
                }
                if (!isSubtask && subtaskCount > 0 && !isExpanded) {
                    Text(
                        text = " ($completedSubtaskCount/$subtaskCount)",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.recurrenceIntervalDays != null) {
                    Text(
                        text = "Every ${item.recurrenceIntervalDays} days",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (item.dueDate != null) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = "Due: ${sdf.format(Date(item.dueDate))}",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        // Caret moved to the right
        if (!isSubtask && subtaskCount > 0) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showDelete) {
            IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.KeyboardArrowUp, 
                    contentDescription = "Move Up", 
                    tint = if (index > 0) Color.White else Color.Transparent,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = index < totalItems - 1, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Move Down", 
                    tint = if (index < totalItems - 1) Color.White else Color.Transparent,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isSubtask) {
                        DropdownMenuItem(
                            text = { Text("Add Subtask") },
                            onClick = {
                                showAddSubtaskDialog = true
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Title") },
                        onClick = {
                            showEditTitleDialog = true
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isDaily) "Make Non-Daily" else "Make Daily") },
                        onClick = {
                            onUpdateItem(item.copy(isDaily = !item.isDaily, isPlannedForToday = if (!item.isDaily) true else item.isPlannedForToday))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Due Date") },
                        onClick = {
                            showDatePicker = true
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
                        text = { Text(stringResource(R.string.delete), color = Color.Red) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateItem(item.copy(dueDate = datePickerState.selectedDateMillis))
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateItem(item.copy(dueDate = null))
                    showDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRecurrenceDialog) {
        RecurrenceDialog(
            initialInterval = item.recurrenceIntervalDays,
            onDismiss = { showRecurrenceDialog = false },
            onConfirm = { days ->
                onUpdateItem(item.copy(recurrenceIntervalDays = days))
                showRecurrenceDialog = false
            }
        )
    }

    if (showEditTitleDialog) {
        EditTitleDialog(
            initialTitle = item.title,
            onDismiss = { showEditTitleDialog = false },
            onConfirm = { title ->
                onUpdateItem(item.copy(title = title))
                showEditTitleDialog = false
            }
        )
    }

    if (showAddSubtaskDialog) {
        AddSubtaskDialog(
            onDismiss = { showAddSubtaskDialog = false },
            onAdd = onAddSubtask
        )
    }
}
