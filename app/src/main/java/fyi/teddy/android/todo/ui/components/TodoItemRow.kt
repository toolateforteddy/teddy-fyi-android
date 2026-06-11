package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.todo.data.TodoItem
import java.time.LocalDate

sealed interface TodoItemIntent {
    data class Delete(val item: TodoItem) : TodoItemIntent
    data class Update(val item: TodoItem) : TodoItemIntent
    data class AddSubtask(val parentId: String, val title: String) : TodoItemIntent
    data class MoveToTop(val item: TodoItem) : TodoItemIntent
    data class MoveToBottom(val item: TodoItem) : TodoItemIntent
    data class MoveUp(val item: TodoItem) : TodoItemIntent
    data class MoveDown(val item: TodoItem) : TodoItemIntent
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
    isEditing: Boolean,
    isPlanningMode: Boolean,
    planningDate: String? = null,
    showScheduledDate: Boolean = true,
    index: Int,
    totalItems: Int,
    isRecentlyCompleted: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onIntent: (TodoItemIntent) -> Unit,
) {
    var showMenu by remember { mutableStateOf(value = false) }
    var activeOverlay by remember { mutableStateOf<ActiveRowOverlay?>(null) }
    var dragOffsetY by remember { mutableStateOf(value = 0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(
                if (isEditing) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragOffsetY = 0f },
                            onDragEnd = { dragOffsetY = 0f },
                            onDragCancel = { dragOffsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val threshold = 120f
                                if (dragOffsetY < -threshold) {
                                    onIntent(TodoItemIntent.MoveUp(item))
                                    dragOffsetY = 0f
                                } else if (dragOffsetY > threshold) {
                                    onIntent(TodoItemIntent.MoveDown(item))
                                    dragOffsetY = 0f
                                }
                            }
                        )
                    }
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { showMenu = true })
                    }
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSubtask) {
            Spacer(modifier = Modifier.width(16.dp))
            Text("-", color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
        }

        Checkbox(
            checked = if (isPlanningMode) {
                if (planningDate != null) item.scheduledDate == planningDate else item.scheduledDate != null
            } else item.isCompleted || isRecentlyCompleted,
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
                    color = if (!isPlanningMode && (item.isCompleted || isRecentlyCompleted)) Color.Gray else Color.White,
                    style = if (!isPlanningMode && (item.isCompleted || isRecentlyCompleted)) MaterialTheme.typography.bodyLarge.copy(
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
                if (!isSubtask && (subtaskCount > 0) && !isExpanded) {
                    Text(
                        text = " ($completedSubtaskCount/$subtaskCount)",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            if (!item.description.isNullOrEmpty()) {
                Text(
                    text = item.description,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.recurrenceRule != null) {
                    Text(
                        text = formatRecurrenceRule(item.recurrenceRule),
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (item.dueDate != null) {
                    Text(
                        text = "Due: ${LocalDate.ofEpochDay(item.dueDate / 86400000)}",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (item.scheduledDate != null && showScheduledDate) {
                    Text(
                        text = "Scheduled: ${item.scheduledDate}",
                        color = Color.Cyan,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        
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

        if (item.priority > 0 || isEditing) {
            IconButton(
                onClick = {
                    val newPriority = if (item.priority == 2) 0 else 2
                    onIntent(TodoItemIntent.Update(item.copy(priority = newPriority)))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (item.priority > 0) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Toggle Priority",
                    tint = when (item.priority) {
                        2 -> Color(0xFFFFD700)
                        1 -> Color(0xFFFFA500)
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isEditing) {
            IconButton(onClick = { onIntent(TodoItemIntent.MoveUp(item)) }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (index > 0) Color.White else Color.Transparent,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { onIntent(TodoItemIntent.MoveDown(item)) },
                enabled = index < totalItems - 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (index < totalItems - 1) Color.White else Color.Transparent,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isSubtask) {
                    DropdownMenuItem(
                        text = { Text("Add Subtask") },
                        onClick = {
                            activeOverlay = ActiveRowOverlay.AddSubtask
                            showMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Edit Title") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.EditTitle
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Description") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.EditDescription
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set Priority...") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.Priority
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (item.isDaily) "Make Non-Daily" else "Make Daily") },
                    onClick = {
                        onIntent(TodoItemIntent.Update(item.copy(isDaily = !item.isDaily, scheduledDate = if (!item.isDaily) LocalDate.now().toString() else item.scheduledDate)))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set Due Date") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.DueDatePicker
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Schedule For...") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.ScheduleDatePicker
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Push to Tomorrow") },
                    onClick = {
                        val tomorrow = (if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()).plusDays(1)
                        onIntent(TodoItemIntent.Update(item.copy(scheduledDate = tomorrow.toString())))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Snooze For...") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.Snooze
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Recurrence") },
                    onClick = {
                        activeOverlay = ActiveRowOverlay.Recurrence
                        showMenu = false
                    }
                )
                Divider()
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Move to Top") },
                        onClick = {
                            onIntent(TodoItemIntent.MoveToTop(item))
                            showMenu = false
                        }
                    )
                }
                if (index < totalItems - 1) {
                    DropdownMenuItem(
                        text = { Text("Move to Bottom") },
                        onClick = {
                            onIntent(TodoItemIntent.MoveToBottom(item))
                            showMenu = false
                        }
                    )
                }
                Divider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = Color.Red) },
                    onClick = {
                        onIntent(TodoItemIntent.Delete(item))
                        showMenu = false
                    }
                )
            }

        }
    }

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
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onIntent(TodoItemIntent.Update(item.copy(dueDate = null)))
                    activeOverlay = null
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
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onIntent(TodoItemIntent.Update(item.copy(scheduledDate = null)))
                    activeOverlay = null
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
            }
        )
    }

    if (activeOverlay == ActiveRowOverlay.AddSubtask) {
        AddSubtaskDialog(
            onDismiss = { activeOverlay = null },
            onAdd = { title ->
                onIntent(TodoItemIntent.AddSubtask(item.id, title))
                activeOverlay = null
            }
        )
    }
}

private fun formatRecurrenceRule(rule: String): String {
    val parts = rule.split(";").associate { 
        val kv = it.split("=")
        if (kv.size == 2) kv[0].uppercase() to kv[1].uppercase() else "" to ""
    }
    val freq = parts["FREQ"]
    val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
    val byDay = parts["BYDAY"]
    
    return when {
        freq == "DAILY" && interval == 1 -> "Every day"
        freq == "DAILY" && interval == 7 -> "Every week"
        freq == "DAILY" -> "Every $interval days"
        freq == "WEEKLY" && byDay == "TU,TH" -> "Tuesday & Thursday"
        freq == "WEEKLY" && byDay == "MO,WE,FR" -> "Mon, Wed & Fri"
        freq == "WEEKLY" -> "Every week"
        freq == "MONTHLY" -> "Every month"
        else -> "Recurring"
    }
}
