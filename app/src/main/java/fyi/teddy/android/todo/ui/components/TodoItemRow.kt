@file:Suppress("MatchingDeclarationName")
package fyi.teddy.android.todo.ui.components

import androidx.core.graphics.toColorInt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.utils.getIconByName
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

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
    subtasks: List<TodoItem> = emptyList(),
    allLists: List<TodoList> = emptyList(),
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
    var isConfirmed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val itemColor = remember(item.listId, allLists) {
        allLists.find { it.id == item.listId }?.let { list ->
            try {
                Color(list.colorHex.toColorInt())
            } catch (_: Exception) {
                NeonTeal
            }
        } ?: NeonTeal
    }

    val today = LocalDate.now().toString()
    val isScheduledForToday = item.scheduledDate == today
    val isScheduled = item.scheduledDate != null

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            delay(1000.milliseconds)
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                isConfirmed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            isConfirmed = false
        }
    }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            if (isConfirmed) {
                when (dismissState.currentValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (!isScheduledForToday) {
                            onIntent(TodoItemIntent.Update(item.copy(scheduledDate = today)))
                        }
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        if (isScheduled) {
                            onIntent(TodoItemIntent.Update(item.copy(scheduledDate = null)))
                        }
                    }

                    else -> {}
                }
            }
            dismissState.reset()
            isConfirmed = false
        }
    }

    val iconScale by animateFloatAsState(if (isConfirmed) 1.3f else 1.0f, label = "iconScale")

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isScheduledForToday,
        enableDismissFromEndToStart = isScheduled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> if (!isScheduledForToday) {
                        if (isConfirmed) NeonTeal.copy(alpha = 0.6f) else NeonTeal.copy(alpha = 0.15f)
                    } else Color.Transparent

                    SwipeToDismissBoxValue.EndToStart -> if (isScheduled) {
                        if (isConfirmed) Color.Red.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.15f)
                    } else Color.Transparent

                    else -> Color.Transparent
                },
                label = "backgroundColor"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(ClippedCornerShape(8f))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd && !isScheduledForToday) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "Schedule for Today",
                                tint = if (isConfirmed) Color.White else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.scale(iconScale)
                            )
                        }
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart && isScheduled) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = "Unschedule",
                                tint = if (isConfirmed) Color.White else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.scale(iconScale)
                            )
                        }
                    }

                    else -> {}
                }
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(ClippedCornerShape(8f))
                    .then(
                        if (item.lastScheduledDate != null && !isScheduled) {
                            Modifier.border(
                                1.dp,
                                Color(0xFFFFA500).copy(alpha = 0.3f),
                                ClippedCornerShape(8f)
                            )
                        } else Modifier
                    )
                    .background(if (isSubtask) Color.Transparent else Color(0xFF0B0B0F))
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
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSubtask) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 16.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .background(itemColor.copy(alpha = 0.3f))
                    )
                }

                val isChecked = if (isPlanningMode) {
                    if (planningDate != null) item.scheduledDate == planningDate else item.scheduledDate != null
                } else item.isCompleted || isRecentlyCompleted

                HexCheckbox(
                    checked = isChecked,
                    modifier = Modifier.clickable { onCheckedChange(!isChecked) },
                    color = itemColor
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.lastScheduledDate != null && !isScheduled) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Rolled Over",
                                tint = Color(0xFFFFA500).copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(14.dp)
                            )
                        }
                        val explicitIcon = getIconByName(item.icon)
                        if (explicitIcon != null) {
                            Icon(
                                imageVector = explicitIcon,
                                contentDescription = null,
                                tint = itemColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = item.title,
                            color = if (!isPlanningMode && isChecked) MutedGrey else Color.White,
                            style = if (!isPlanningMode && isChecked) MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ) else MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (item.isDaily) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Daily",
                                tint = itemColor,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(12.dp)
                            )
                        }
                        if (!isSubtask && (subtaskCount > 0) && !isExpanded) {
                            Text(
                                text = " ($completedSubtaskCount/$subtaskCount)",
                                color = MutedGrey,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.recurrenceRule != null) {
                            Text(
                                text = formatRecurrenceRule(item.recurrenceRule),
                                color = MutedGrey,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        if (item.dueDate != null) {
                            Text(
                                text = "Due: ${LocalDate.ofEpochDay(item.dueDate / 86400000)}",
                                color = Color(0xFFFF4B4B),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (item.scheduledDate != null && showScheduledDate) {
                            Text(
                                text = "Scheduled: ${item.scheduledDate}",
                                color = itemColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                if (!isSubtask && subtaskCount > 0) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MutedGrey,
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
                                else -> MutedGrey
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isEditing) {
                    IconButton(
                        onClick = { onIntent(TodoItemIntent.MoveUp(item)) },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
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
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box {
                    TodoItemMenu(
                        item = item,
                        subtasks = subtasks,
                        allLists = allLists,
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        context = TodoMenuContext.LIST_ROW,
                        onIntent = { intent ->
                            if (intent is TodoItemIntent.ToggleComplete) {
                                onCheckedChange(intent.isChecked)
                            } else {
                                onIntent(intent)
                            }
                        },
                        isSubtask = isSubtask
                    )
                }
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Suppress("CyclomaticComplexMethod")
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
