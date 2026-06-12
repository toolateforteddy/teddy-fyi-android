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
import fyi.teddy.android.utils.getIconByName
import java.time.LocalDate

sealed interface TodoItemIntent {
    data class Delete(val item: TodoItem) : TodoItemIntent
    data class Update(val item: TodoItem) : TodoItemIntent
    data class AddSubtask(val parentId: String, val title: String) : TodoItemIntent
    data class MoveToTop(val item: TodoItem) : TodoItemIntent
    data class MoveToBottom(val item: TodoItem) : TodoItemIntent
    data class MoveUp(val item: TodoItem) : TodoItemIntent
    data class MoveDown(val item: TodoItem) : TodoItemIntent
    data class AssignIcon(val item: TodoItem) : TodoItemIntent
    data class ToggleComplete(val item: TodoItem, val isChecked: Boolean) : TodoItemIntent
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
                val explicitIcon = getIconByName(item.icon)
                if (explicitIcon != null) {
                    Icon(
                        imageVector = explicitIcon,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                }
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
            TodoItemMenu(
                item = item,
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                onIntent = { intent ->
                    if (intent is TodoItemIntent.ToggleComplete) {
                        onCheckedChange(intent.isChecked)
                    } else {
                        onIntent(intent)
                    }
                },
                isSubtask = isSubtask,
                index = index,
                totalItems = totalItems
            )
        }
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
