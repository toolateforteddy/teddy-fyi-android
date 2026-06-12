package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import fyi.teddy.android.R
import fyi.teddy.android.todo.data.TodoItem
import java.time.LocalDate

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
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onIntent: (TodoItemIntent) -> Unit,
    isSubtask: Boolean = false,
    index: Int = 0,
    totalItems: Int = 1
) {
    var activeOverlay by remember { mutableStateOf<ActiveRowOverlay?>(null) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        if (!isSubtask) {
            DropdownMenuItem(
                text = { Text("Add Subtask") },
                onClick = {
                    activeOverlay = ActiveRowOverlay.AddSubtask
                    onDismissRequest()
                }
            )
        }
        DropdownMenuItem(
            text = { Text(if (item.isCompleted) "Mark as Active" else "Mark as Completed") },
            onClick = {
                onIntent(TodoItemIntent.ToggleComplete(item, !item.isCompleted))
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Edit Title") },
            onClick = {
                activeOverlay = ActiveRowOverlay.EditTitle
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Change Icon") },
            onClick = {
                activeOverlay = ActiveRowOverlay.IconPicker
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Assign Icon") },
            onClick = {
                onIntent(TodoItemIntent.AssignIcon(item))
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Edit Description") },
            onClick = {
                activeOverlay = ActiveRowOverlay.EditDescription
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Set Priority...") },
            onClick = {
                activeOverlay = ActiveRowOverlay.Priority
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(if (item.isDaily) "Make Non-Daily" else "Make Daily") },
            onClick = {
                onIntent(TodoItemIntent.Update(item.copy(isDaily = !item.isDaily, scheduledDate = if (!item.isDaily) LocalDate.now().toString() else item.scheduledDate)))
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Set Due Date") },
            onClick = {
                activeOverlay = ActiveRowOverlay.DueDatePicker
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Schedule For...") },
            onClick = {
                activeOverlay = ActiveRowOverlay.ScheduleDatePicker
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Push to Tomorrow") },
            onClick = {
                val tomorrow = (if (item.scheduledDate != null) LocalDate.parse(item.scheduledDate) else LocalDate.now()).plusDays(1)
                onIntent(TodoItemIntent.Update(item.copy(scheduledDate = tomorrow.toString())))
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Snooze For...") },
            onClick = {
                activeOverlay = ActiveRowOverlay.Snooze
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Recurrence") },
            onClick = {
                activeOverlay = ActiveRowOverlay.Recurrence
                onDismissRequest()
            }
        )
        Divider()
        if (index > 0) {
            DropdownMenuItem(
                text = { Text("Move to Top") },
                onClick = {
                    onIntent(TodoItemIntent.MoveToTop(item))
                    onDismissRequest()
                }
            )
        }
        if (index < totalItems - 1) {
            DropdownMenuItem(
                text = { Text("Move to Bottom") },
                onClick = {
                    onIntent(TodoItemIntent.MoveToBottom(item))
                    onDismissRequest()
                }
            )
        }
        Divider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete), color = Color.Red) },
            onClick = {
                onIntent(TodoItemIntent.Delete(item))
                onDismissRequest()
            }
        )
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

    if (activeOverlay == ActiveRowOverlay.IconPicker) {
        IconPickerDialog(
            onDismiss = { activeOverlay = null },
            onConfirm = { iconName ->
                onIntent(TodoItemIntent.Update(item.copy(icon = iconName)))
                activeOverlay = null
            }
        )
    }
}
