package fyi.teddy.android.todo.ui.components

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
    planningDate: String? = null,
    showScheduledDate: Boolean = true,
    index: Int,
    totalItems: Int,
    isRecentlyCompleted: Boolean = false,
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
    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showAddSubtaskDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showScheduleDatePicker by remember { mutableStateOf(false) }
    var showSnoozeForDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            },
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
                if (!isSubtask && subtaskCount > 0 && !isExpanded) {
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

        if (item.priority > 0 || showDelete) {
            IconButton(
                onClick = {
                    val newPriority = if (item.priority == 2) 0 else 2
                    onUpdateItem(item.copy(priority = newPriority))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (item.priority > 0) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Toggle Priority",
                    tint = if (item.priority == 2) Color(0xFFFFD700)
                           else if (item.priority == 1) Color(0xFFFFA500)
                           else Color.Gray,
                    modifier = Modifier.size(20.dp)
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
            IconButton(
                onClick = onMoveDown,
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
                    text = { Text("Edit Description") },
                    onClick = {
                        showEditDescriptionDialog = true
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set Priority...") },
                    onClick = {
                        showPriorityDialog = true
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (item.isDaily) "Make Non-Daily" else "Make Daily") },
                    onClick = {
                        onUpdateItem(item.copy(isDaily = !item.isDaily, scheduledDate = if (!item.isDaily) LocalDate.now().toString() else item.scheduledDate))
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
                    text = { Text("Schedule For...") },
                    onClick = {
                        showScheduleDatePicker = true
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Push to Tomorrow") },
                    onClick = {
                        val tomorrow = (if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()).plusDays(1)
                        onUpdateItem(item.copy(scheduledDate = tomorrow.toString()))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Snooze For...") },
                    onClick = {
                        showSnoozeForDialog = true
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
    
    if (showScheduleDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.scheduledDate?.let { 
                LocalDate.parse(it).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showScheduleDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    }
                    onUpdateItem(item.copy(scheduledDate = selectedDate))
                    showScheduleDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateItem(item.copy(scheduledDate = null))
                    showScheduleDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRecurrenceDialog) {
        RecurrenceDialog(
            initialRule = item.recurrenceRule,
            onDismiss = { showRecurrenceDialog = false },
            onConfirm = { rule ->
                onUpdateItem(item.copy(recurrenceRule = rule))
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

    if (showEditDescriptionDialog) {
        EditDescriptionDialog(
            initialDescription = item.description,
            onDismiss = { showEditDescriptionDialog = false },
            onConfirm = { desc ->
                onUpdateItem(item.copy(description = desc))
                showEditDescriptionDialog = false
            }
        )
    }

    if (showPriorityDialog) {
        PriorityDialog(
            initialPriority = item.priority,
            onDismiss = { showPriorityDialog = false },
            onConfirm = { priority ->
                onUpdateItem(item.copy(priority = priority))
                showPriorityDialog = false
            }
        )
    }

    if (showSnoozeForDialog) {
        SnoozeForDialog(
            onDismiss = { showSnoozeForDialog = false },
            onConfirm = { amount, isMonths ->
                val baseDate = if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()
                val newDate = if (isMonths) {
                    fyi.teddy.android.todo.util.TaskSchedulerUtils.snoozeForMonths(baseDate, amount)
                } else {
                    fyi.teddy.android.todo.util.TaskSchedulerUtils.snoozeForDays(baseDate, amount)
                }
                onUpdateItem(item.copy(scheduledDate = newDate.toString()))
                showSnoozeForDialog = false
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
