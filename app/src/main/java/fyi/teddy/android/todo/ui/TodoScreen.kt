package fyi.teddy.android.todo.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.todo.ui.components.AddListDialog
import fyi.teddy.android.todo.ui.components.ClearAllConfirmationDialog
import fyi.teddy.android.todo.ui.components.EditListDialog
import fyi.teddy.android.todo.ui.components.TodoInputBar
import fyi.teddy.android.todo.ui.components.TodoItemRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.LocalDate
import java.util.concurrent.TimeUnit

enum class TodoMode {
    BACKLOG, PLANNING, TODAY, SCHEDULED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(userId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(context.applicationContext as android.app.Application, userId)
    )
    
    val currentMode by viewModel.currentMode.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val showCompletedOnly by viewModel.showCompletedOnly.collectAsState()
    val groupedItems by viewModel.groupedItems.collectAsState()
    val selectedPlanningDate by viewModel.selectedPlanningDate.collectAsState()
    val recentlyCompletedIds by viewModel.recentlyCompletedIds.collectAsState()
    val allLists by viewModel.allLists.collectAsState()
    val selectedListId by viewModel.selectedListId.collectAsState()
    
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var showPlanningDatePicker by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<fyi.teddy.android.todo.data.TodoList?>(null) }
    val expandedParentIds = remember { mutableStateOf(setOf<String>()) }
    val parties = remember { mutableStateListOf<Party>() }
    


    // Collect visual confetti triggers from ViewModel
    LaunchedEffect(Unit) {
        viewModel.confettiTrigger.collect {
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
            launch {
                delay(3000)
                parties.remove(party)
            }
        }
    }
    
    val onAddNewItem = { title: String, parentId: String? ->
        if (title.isNotBlank()) {
            val scheduledDate = if (currentMode == TodoMode.PLANNING) selectedPlanningDate else null
            viewModel.insertItem(title, userId, parentId, scheduledDate)
        }
    }

    if (showClearAllConfirmation) {
        ClearAllConfirmationDialog(
            onDismiss = { showClearAllConfirmation = false },
            onConfirm = {
                viewModel.deleteAll(userId)
                showClearAllConfirmation = false
            }
        )
    }

    if (showAddListDialog) {
        AddListDialog(
            onDismiss = { showAddListDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.insertList(name, colorHex)
                showAddListDialog = false
            }
        )
    }

    listToEdit?.let { list ->
        EditListDialog(
            list = list,
            onDismiss = { listToEdit = null },
            onConfirm = { updated ->
                viewModel.updateList(updated)
                listToEdit = null
            },
            onDelete = {
                viewModel.deleteList(list)
                listToEdit = null
            }
        )
    }

    if (showPlanningDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(selectedPlanningDate)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPlanningDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    }
                    if (selectedDate != null) {
                        viewModel.setSelectedPlanningDate(selectedDate)
                    }
                    showPlanningDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPlanningDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        val titleText = when (currentMode) {
                            TodoMode.BACKLOG -> if (showCompletedOnly) "Completed Backlog" else "Backlog"
                            TodoMode.SCHEDULED -> if (showCompletedOnly) "Completed Future" else "Future"
                            TodoMode.PLANNING -> {
                                val today = LocalDate.now()
                                val selected = LocalDate.parse(selectedPlanningDate)
                                when {
                                    selected == today -> "Planning: Today"
                                    selected == today.plusDays(1) -> "Planning: Tomorrow"
                                    else -> "Planning: $selectedPlanningDate"
                                }
                            }
                            TodoMode.TODAY -> "Today's Tasks"
                        }
                        Text(
                            text = titleText,
                            modifier = if (currentMode == TodoMode.PLANNING) {
                                Modifier.clickable { showPlanningDatePicker = true }
                            } else {
                                Modifier
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setShowCompletedOnly(!showCompletedOnly) }) {
                            Icon(
                                if (showCompletedOnly) Icons.Default.List else Icons.Default.CheckCircle, 
                                contentDescription = if (showCompletedOnly) stringResource(R.string.show_active) else stringResource(R.string.show_completed),
                                tint = if (showCompletedOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }

                        IconButton(onClick = { viewModel.setEditMode(!isEditMode) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        
                        if (isEditMode) {
                            IconButton(onClick = { showClearAllConfirmation = true }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all), tint = Color.Red)
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
                        selected = currentMode == TodoMode.BACKLOG,
                        onClick = { viewModel.setMode(TodoMode.BACKLOG) },
                        icon = { Icon(Icons.Default.List, contentDescription = "Backlog") },
                        label = { Text("Backlog") }
                    )
                    NavigationBarItem(
                        selected = currentMode == TodoMode.PLANNING,
                        onClick = { viewModel.setMode(TodoMode.PLANNING) },
                        icon = { Icon(Icons.Default.EditCalendar, contentDescription = "Planning") },
                        label = { Text("Planning") }
                    )
                    NavigationBarItem(
                        selected = currentMode == TodoMode.TODAY,
                        onClick = { viewModel.setMode(TodoMode.TODAY) },
                        icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                        label = { Text("Today") }
                    )
                    NavigationBarItem(
                        selected = currentMode == TodoMode.SCHEDULED,
                        onClick = { viewModel.setMode(TodoMode.SCHEDULED) },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Scheduled") },
                        label = { Text("Scheduled") }
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
                    // Space list selection row
                    if (allLists.isNotEmpty() || isEditMode) {
                        @OptIn(ExperimentalFoundationApi::class)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All" space chip
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedListId == null) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { viewModel.selectList(null) }
                            ) {
                                Text(
                                    text = "All",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Custom list chips
                            allLists.forEach { list ->
                                val isSelected = selectedListId == list.id
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else Color(android.graphics.Color.parseColor(list.colorHex)).copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .combinedClickable(
                                            onClick = { viewModel.selectList(list.id) },
                                            onLongClick = { listToEdit = list }
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        // Little color dot for the list
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(list.colorHex)),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = list.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            // Add List Button
                            if (isEditMode) {
                                IconButton(
                                    onClick = { showAddListDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Space", tint = Color.Cyan)
                                }
                            }
                        }
                    }

                    if (!showCompletedOnly && currentMode != TodoMode.TODAY && currentMode != TodoMode.SCHEDULED && !isEditMode) {
                        TodoInputBar(onAddNewItem = { title -> onAddNewItem(title, null) })
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
                                    isPlanningMode = currentMode == TodoMode.PLANNING,
                                    planningDate = selectedPlanningDate,
                                    showScheduledDate = currentMode != TodoMode.TODAY,
                                    index = parentIndex,
                                    totalItems = groupedItems.size,
                                    isRecentlyCompleted = recentlyCompletedIds.contains(parent.id),
                                    onCheckedChange = { isChecked ->
                                        if (currentMode == TodoMode.PLANNING) {
                                            viewModel.updateItem(parent.copy(scheduledDate = if(isChecked) selectedPlanningDate else null))
                                            if (isChecked) {
                                                children.forEach { 
                                                    viewModel.updateItem(it.copy(scheduledDate = selectedPlanningDate))
                                                }
                                            }
                                            return@TodoItemRow
                                        }

                                        viewModel.toggleComplete(parent, isChecked)
                                    },
                                    onDelete = { viewModel.deleteItem(parent) },
                                    onUpdateItem = { viewModel.updateItem(it) },
                                    onAddSubtask = { title -> onAddNewItem(title, parent.id) },
                                    onMoveToTop = {
                                        viewModel.moveParentToTop(parent)
                                    },
                                    onMoveToBottom = {
                                        viewModel.moveParentToBottom(parent)
                                    },
                                    onMoveUp = {
                                        if (parentIndex > 0) {
                                            val prevParent = groupedItems[parentIndex - 1].first
                                            viewModel.swapPositions(parent, prevParent)
                                        }
                                    },
                                    onMoveDown = {
                                        if (parentIndex < groupedItems.size - 1) {
                                            val nextParent = groupedItems[parentIndex + 1].first
                                            viewModel.swapPositions(parent, nextParent)
                                        }
                                    }
                                )
                            }
                            
                            val isExpanded = expandedParentIds.value.contains(parent.id)
                            if (isExpanded) {
                                val childrenToShow = if (currentMode == TodoMode.TODAY || currentMode == TodoMode.SCHEDULED) {
                                    children.filter { it.scheduledDate != null || it.isCompleted }
                                } else {
                                    children
                                }
                                itemsIndexed(childrenToShow, key = { _, it -> it.id }) { childIndex, child ->
                                    TodoItemRow(
                                        item = child,
                                        isSubtask = true,
                                        showDelete = isEditMode,
                                        isPlanningMode = currentMode == TodoMode.PLANNING,
                                        planningDate = selectedPlanningDate,
                                        showScheduledDate = currentMode != TodoMode.TODAY,
                                        index = childIndex,
                                        totalItems = children.size,
                                        isRecentlyCompleted = recentlyCompletedIds.contains(child.id),
                                        onCheckedChange = { isChecked ->
                                            if (currentMode == TodoMode.PLANNING) {
                                                viewModel.updateItem(child.copy(scheduledDate = if(isChecked) selectedPlanningDate else null))
                                                return@TodoItemRow
                                            }
                                            viewModel.toggleComplete(child, isChecked)
                                        },
                                        onDelete = { viewModel.deleteItem(child) },
                                        onUpdateItem = { viewModel.updateItem(it) },
                                        onMoveToTop = {
                                            viewModel.moveChildToTop(child)
                                        },
                                        onMoveToBottom = {
                                            viewModel.moveChildToBottom(child)
                                        },
                                        onMoveUp = {
                                            if (childIndex > 0) {
                                                val prevChild = children[childIndex - 1]
                                                viewModel.swapPositions(child, prevChild)
                                            }
                                        },
                                        onMoveDown = {
                                            if (childIndex < children.size - 1) {
                                                val nextChild = children[childIndex + 1]
                                                viewModel.swapPositions(child, nextChild)
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
