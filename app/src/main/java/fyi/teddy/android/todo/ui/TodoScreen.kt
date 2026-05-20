package fyi.teddy.android.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.ui.components.ClearAllConfirmationDialog
import fyi.teddy.android.todo.ui.components.TodoInputBar
import fyi.teddy.android.todo.ui.components.TodoItemRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

enum class TodoMode {
    BACKLOG, TODAY_PLANNING, TODAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(userId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(context.applicationContext as android.app.Application, userId)
    )
    val scope = rememberCoroutineScope()
    
    var currentMode by remember { mutableStateOf(TodoMode.BACKLOG) }
    val allItems by viewModel.allItems.collectAsState()
    val todayItems by viewModel.todayItems.collectAsState()
    
    var isEditMode by remember { mutableStateOf(false) }
    var showCompletedOnly by remember { mutableStateOf(false) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    val expandedParentIds = remember { mutableStateOf(setOf<Int>()) }
    val parties = remember { mutableStateListOf<Party>() }
    val recentlyCompletedIds = remember { mutableStateListOf<Int>() }
    
    val baseItems = when(currentMode) {
        TodoMode.TODAY -> todayItems
        TodoMode.BACKLOG -> allItems.filter { !it.isPlannedForToday }
        TodoMode.TODAY_PLANNING -> allItems
    }
    
    val filteredItems = remember(baseItems, showCompletedOnly, recentlyCompletedIds.toList()) {
        baseItems.filter { item ->
            if (showCompletedOnly) item.isCompleted
            else !item.isCompleted || recentlyCompletedIds.contains(item.id)
        }
    }

    val groupedItems = remember(filteredItems, currentMode) {
        val allParents = filteredItems.filter { it.parentId == null }
        val allChildren = filteredItems.filter { it.parentId != null }.groupBy { it.parentId }
        
        if (currentMode == TodoMode.TODAY) {
            allParents.filter { parent ->
                parent.isPlannedForToday || allChildren[parent.id]?.any { it.isPlannedForToday } == true
            }.map { parent ->
                parent to (allChildren[parent.id] ?: emptyList())
            }
        } else {
            allParents.map { it to (allChildren[it.id] ?: emptyList()) }
        }
    }
    
    val onAddNewItem = { title: String, parentId: Int? ->
        if (title.isNotBlank()) {
            viewModel.insertItem(TodoItem(
                title = title, 
                userId = userId,
                parentId = parentId,
                isPlannedForToday = currentMode == TodoMode.TODAY_PLANNING
            ))
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = when {
                                currentMode == TodoMode.TODAY_PLANNING -> "Planning Today"
                                currentMode == TodoMode.TODAY -> "Today's Tasks"
                                showCompletedOnly -> stringResource(R.string.show_completed)
                                else -> stringResource(R.string.app_name) + " List"
                            }
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { showCompletedOnly = !showCompletedOnly }) {
                            Icon(
                                if (showCompletedOnly) Icons.Default.List else Icons.Default.CheckCircle, 
                                contentDescription = if (showCompletedOnly) stringResource(R.string.show_active) else stringResource(R.string.show_completed),
                                tint = if (showCompletedOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }

                        IconButton(onClick = { isEditMode = !isEditMode }) {
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
                                    isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                    index = parentIndex,
                                    totalItems = groupedItems.size,
                                    onCheckedChange = { isChecked ->
                                        if (currentMode == TodoMode.TODAY_PLANNING) {
                                            viewModel.updateItem(parent.copy(isPlannedForToday = isChecked))
                                            if (isChecked) {
                                                children.forEach { 
                                                    viewModel.updateItem(it.copy(isPlannedForToday = true))
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
                                                    val interval = parent.recurrenceIntervalDays * 24 * 60 * 60 * 1000L
                                                    val nextTime = System.currentTimeMillis() + interval
                                                    viewModel.updateItem(parent.copy(
                                                        isCompleted = false, 
                                                        scheduledAt = nextTime, 
                                                        isPlannedForToday = false
                                                    ))
                                                }
                                                delay(1000) 
                                                parties.remove(party)
                                            }
                                        } else if (!isChecked) {
                                            recentlyCompletedIds.remove(parent.id)
                                        }
                                        viewModel.updateItem(parent.copy(isCompleted = isChecked))
                                    },
                                    onDelete = { viewModel.deleteItem(parent) },
                                    onUpdateItem = { viewModel.updateItem(it) },
                                    onAddSubtask = { title -> onAddNewItem(title, parent.id) },
                                    onMoveToTop = {
                                        val minPos = allItems.filter { it.parentId == null }.minByOrNull { it.position }?.position ?: 0
                                        viewModel.updateItem(parent.copy(position = minPos - 1))
                                    },
                                    onMoveToBottom = {
                                        val maxPos = allItems.filter { it.parentId == null }.maxByOrNull { it.position }?.position ?: 0
                                        viewModel.updateItem(parent.copy(position = maxPos + 1))
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
                                val childrenToShow = if (currentMode == TodoMode.TODAY) {
                                    children.filter { it.isPlannedForToday || it.isCompleted }
                                } else {
                                    children
                                }
                                itemsIndexed(childrenToShow, key = { _, it -> it.id }) { childIndex, child ->
                                    TodoItemRow(
                                        item = child,
                                        isSubtask = true,
                                        showDelete = isEditMode,
                                        isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                        index = childIndex,
                                        totalItems = children.size,
                                        onCheckedChange = { isChecked ->
                                            if (currentMode == TodoMode.TODAY_PLANNING) {
                                                viewModel.updateItem(child.copy(isPlannedForToday = isChecked))
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
                                            viewModel.updateItem(child.copy(isCompleted = isChecked))
                                        },
                                        onDelete = { viewModel.deleteItem(child) },
                                        onUpdateItem = { viewModel.updateItem(it) },
                                        onMoveToTop = {
                                            val minPos = children.minByOrNull { it.position }?.position ?: 0
                                            viewModel.updateItem(child.copy(position = minPos - 1))
                                        },
                                        onMoveToBottom = {
                                            val maxPos = children.maxByOrNull { it.position }?.position ?: 0
                                            viewModel.updateItem(child.copy(position = maxPos + 1))
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
