package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.todo.data.TodoItem
import java.time.LocalDate

enum class TodoMenuContext {
    DASHBOARD_HEX,
    LIST_ROW
}

sealed interface ActiveRowOverlay {
    object Recurrence : ActiveRowOverlay
    object EditTitle : ActiveRowOverlay
    object EditDescription : ActiveRowOverlay
    object Priority : ActiveRowOverlay
    object Snooze : ActiveRowOverlay
    object AddSubtask : ActiveRowOverlay
    object DueDatePicker : ActiveRowOverlay
    object ScheduleDatePicker : ActiveRowOverlay
    object IconPicker : ActiveRowOverlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItemMenu(
    item: TodoItem,
    subtasks: List<TodoItem> = emptyList(),
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onIntent: (TodoItemIntent) -> Unit,
    context: TodoMenuContext = TodoMenuContext.LIST_ROW,
    isSubtask: Boolean = false
) {
    var activeOverlay by remember { mutableStateOf<ActiveRowOverlay?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    when (context) {
                        TodoMenuContext.DASHBOARD_HEX -> {
                            DashboardHexMenuContent(
                                item = item,
                                subtasks = subtasks,
                                onIntent = onIntent,
                                onDismissRequest = onDismissRequest,
                                onShowOverlay = { activeOverlay = it }
                            )
                        }
                        TodoMenuContext.LIST_ROW -> {
                            ListRowMenuContent(
                                item = item,
                                isSubtask = isSubtask,
                                subtasks = subtasks,
                                onIntent = onIntent,
                                onDismissRequest = onDismissRequest,
                                onShowOverlay = { activeOverlay = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Overlays / Dialogs
    if (activeOverlay == ActiveRowOverlay.DueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { activeOverlay = null },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(TodoItemIntent.Update(item.copy(dueDate = datePickerState.selectedDateMillis)))
                    activeOverlay = null
                    onDismissRequest()
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onIntent(TodoItemIntent.Update(item.copy(dueDate = null)))
                    activeOverlay = null
                    onDismissRequest()
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (activeOverlay == ActiveRowOverlay.ScheduleDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.scheduledDate?.let { 
                LocalDate.parse(it).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { activeOverlay = null },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    }
                    onIntent(TodoItemIntent.Update(item.copy(scheduledDate = selectedDate)))
                    activeOverlay = null
                    onDismissRequest()
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onIntent(TodoItemIntent.Update(item.copy(scheduledDate = null)))
                    activeOverlay = null
                    onDismissRequest()
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (activeOverlay == ActiveRowOverlay.Recurrence) {
        RecurrenceDialog(
            initialRule = item.recurrenceRule,
            onDismiss = { activeOverlay = null },
            onConfirm = { rule ->
                onIntent(TodoItemIntent.Update(item.copy(recurrenceRule = rule)))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.EditTitle) {
        EditTitleDialog(
            initialTitle = item.title,
            onDismiss = { activeOverlay = null },
            onConfirm = { title ->
                onIntent(TodoItemIntent.Update(item.copy(title = title)))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.EditDescription) {
        EditDescriptionDialog(
            initialDescription = item.description,
            onDismiss = { activeOverlay = null },
            onConfirm = { desc ->
                onIntent(TodoItemIntent.Update(item.copy(description = desc)))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.Priority) {
        PriorityDialog(
            initialPriority = item.priority,
            onDismiss = { activeOverlay = null },
            onConfirm = { priority ->
                onIntent(TodoItemIntent.Update(item.copy(priority = priority)))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.Snooze) {
        SnoozeForDialog(
            onDismiss = { activeOverlay = null },
            onConfirm = { amount, isMonths ->
                val baseDate = if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()
                val newDate = if (isMonths) {
                    fyi.teddy.android.todo.util.TaskSchedulerUtils.snoozeForMonths(baseDate, amount)
                } else {
                    fyi.teddy.android.todo.util.TaskSchedulerUtils.snoozeForDays(baseDate, amount)
                }
                onIntent(TodoItemIntent.Update(item.copy(scheduledDate = newDate.toString())))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.AddSubtask) {
        AddSubtaskDialog(
            parentTaskTitle = item.title,
            onDismiss = {
                activeOverlay = null
            },
            onAdd = { title ->
                onIntent(TodoItemIntent.AddSubtask(item.id, title))
            },
            onFinish = { title ->
                if (title.isNotBlank()) {
                    onIntent(TodoItemIntent.AddSubtask(item.id, title))
                }
                activeOverlay = null
                onDismissRequest()
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.IconPicker) {
        IconPickerDialog(
            initialIcon = item.icon,
            onDismiss = { activeOverlay = null },
            onConfirm = { iconName ->
                onIntent(TodoItemIntent.Update(item.copy(icon = iconName)))
                activeOverlay = null
                onDismissRequest()
            },
            onAutoAssign = {
                onIntent(TodoItemIntent.AssignIcon(item))
                activeOverlay = null
                onDismissRequest()
            }
        )
    }
}

@Composable
fun DashboardHexMenuContent(
    item: TodoItem,
    subtasks: List<TodoItem>,
    onIntent: (TodoItemIntent) -> Unit,
    onDismissRequest: () -> Unit,
    onShowOverlay: (ActiveRowOverlay) -> Unit
) {
    // Top Action Row: Large Mark as Completed button
    Button(
        onClick = {
            onIntent(TodoItemIntent.ToggleComplete(item, !item.isCompleted))
            onDismissRequest()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (item.isCompleted) Color.Gray else Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(if (item.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (item.isCompleted) "Mark as Active" else "Mark as Completed", style = MaterialTheme.typography.titleMedium)
    }

    Spacer(Modifier.height(24.dp))

    // Secondary Quick Grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MenuIconButton(
            icon = Icons.Default.Edit,
            label = "Edit",
            onClick = {
                onShowOverlay(ActiveRowOverlay.EditTitle)
            }
        )
        MenuIconButton(
            icon = Icons.Default.ArrowForward,
            label = "Tomorrow",
            onClick = {
                val tomorrow = (if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()).plusDays(1)
                onIntent(TodoItemIntent.Update(item.copy(scheduledDate = tomorrow.toString())))
                onDismissRequest()
            }
        )
        MenuIconButton(
            icon = Icons.Default.Assignment,
            label = if (subtasks.isNotEmpty()) "Subtasks (${subtasks.size})" else "Subtasks",
            onClick = {
                onShowOverlay(ActiveRowOverlay.AddSubtask)
            }
        )
        MenuIconButton(
            icon = Icons.Default.Delete,
            label = "Delete",
            tint = Color.Red,
            onClick = {
                onIntent(TodoItemIntent.Delete(item))
                onDismissRequest()
            }
        )
    }

    if (subtasks.isNotEmpty()) {
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = "Subtasks",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
        ) {
            items(subtasks) { subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = subtask.isCompleted,
                        onCheckedChange = { isChecked ->
                            onIntent(TodoItemIntent.ToggleComplete(subtask, isChecked))
                        }
                    )
                    Text(
                        text = subtask.title,
                        style = if (subtask.isCompleted) MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        ) else MaterialTheme.typography.bodyMedium,
                        color = if (subtask.isCompleted) Color.Gray else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun ListRowMenuContent(
    item: TodoItem,
    isSubtask: Boolean,
    subtasks: List<TodoItem>,
    onIntent: (TodoItemIntent) -> Unit,
    onDismissRequest: () -> Unit,
    onShowOverlay: (ActiveRowOverlay) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // Quick Info Area
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onShowOverlay(ActiveRowOverlay.EditTitle) }) {
                    Text("Edit Title")
                }
                TextButton(onClick = { onShowOverlay(ActiveRowOverlay.EditDescription) }) {
                    Text("Edit Description")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Visual Action Grid (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionGridItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "Schedule",
                    onClick = { onShowOverlay(ActiveRowOverlay.ScheduleDatePicker) }
                )
                ActionGridItem(
                    icon = Icons.Default.Repeat,
                    label = "Recurrence",
                    onClick = { onShowOverlay(ActiveRowOverlay.Recurrence) }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionGridItem(
                    icon = Icons.Default.Face, // Asset cell
                    label = "Icon",
                    onClick = { onShowOverlay(ActiveRowOverlay.IconPicker) }
                )
                ActionGridItem(
                    icon = Icons.Default.Flag,
                    label = "Priority",
                    onClick = { onShowOverlay(ActiveRowOverlay.Priority) }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Management Tray
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSubtask) {
                Button(
                    onClick = { onShowOverlay(ActiveRowOverlay.AddSubtask) },
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (subtasks.isNotEmpty()) "Add Subtask (${subtasks.size})" else "Add Subtask")
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            IconButton(onClick = {
                onIntent(TodoItemIntent.Delete(item))
                onDismissRequest()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        if (subtasks.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Subtasks",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(subtasks) { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { isChecked ->
                                onIntent(TodoItemIntent.ToggleComplete(subtask, isChecked))
                            }
                        )
                        Text(
                            text = subtask.title,
                            style = if (subtask.isCompleted) MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ) else MaterialTheme.typography.bodyMedium,
                            color = if (subtask.isCompleted) Color.Gray else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun ActionGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
