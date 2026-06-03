package fyi.teddy.android.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.todo.ui.components.ClearAllConfirmationDialog
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
    BACKLOG, TODAY_PLANNING, TODAY, SCHEDULED
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
    
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    val expandedParentIds = remember { mutableStateOf(setOf<Int>()) }
    val parties = remember { mutableStateListOf<Party>() }
    
    val todayString = LocalDate.now().toString()

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
    
    val onAddNewItem = { title: String, parentId: Int? ->
        if (title.isNotBlank()) {
            val scheduledDate = if (currentMode == TodoMode.TODAY_PLANNING) todayString else null
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
                        selected = currentMode == TodoMode.TODAY_PLANNING,
                        onClick = { viewModel.setMode(TodoMode.TODAY_PLANNING) },
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
                                    showScheduledDate = currentMode != TodoMode.TODAY,
                                    index = parentIndex,
                                    totalItems = groupedItems.size,
                                    onCheckedChange = { isChecked ->
                                        if (currentMode == TodoMode.TODAY_PLANNING) {
                                            viewModel.updateItem(parent.copy(scheduledDate = if(isChecked) todayString else null))
                                            if (isChecked) {
                                                children.forEach { 
                                                    viewModel.updateItem(it.copy(scheduledDate = todayString))
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
                                        isPlanningMode = currentMode == TodoMode.TODAY_PLANNING,
                                        showScheduledDate = currentMode != TodoMode.TODAY,
                                        index = childIndex,
                                        totalItems = children.size,
                                        onCheckedChange = { isChecked ->
                                            if (currentMode == TodoMode.TODAY_PLANNING) {
                                                viewModel.updateItem(child.copy(scheduledDate = if(isChecked) todayString else null))
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
