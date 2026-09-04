package fyi.teddy.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import fyi.teddy.android.MainActivity
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.util.TaskSchedulerUtils
import kotlinx.coroutines.flow.first

import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.core.graphics.toColorInt
import androidx.glance.color.ColorProvider
import fyi.teddy.android.todo.data.TodoList

import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.action.actionRunCallback
import fyi.teddy.android.widget.ToggleTodoTaskAction
import fyi.teddy.android.todo.ui.theme.TodoTheme

class TodoTacticalWidget : GlanceAppWidget() {

    companion object {
        const val ACTION_OPEN_TODO = "fyi.teddy.android.ACTION_OPEN_TODO"
        val WIDGET_ACTION_KEY = ActionParameters.Key<String>("widget_action")
    }

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val session = UserSession()
        session.load(context)
        val userId = session.userId ?: "unauthed"

        val db = AppDatabase.getDatabase(context)
        val listsMap = try {
            db.todoDao().getAllListsOneShot().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        val todayStr = TaskSchedulerUtils.getTodayDateString()
        val rawTodayItems = try {
            db.todoDao().getTodayItems(userId, todayStr).first()
        } catch (_: Exception) {
            emptyList()
        }
        val uncompleted = rawTodayItems.filter {
            !it.isDeleted && !it.isCompleted && (it.userId == userId || (userId == "unauthed" && it.userId == null))
        }
        val parents = uncompleted.filter { it.parentId == null }
        val children = uncompleted.filter { it.parentId != null }.groupBy { it.parentId }

        val todayItems = parents.filter { parent ->
            parent.scheduledDate == todayStr || children[parent.id]?.any { it.scheduledDate == todayStr } == true
        }

        provideContent {
            GlanceTheme {
                TodoWidgetContent(todayItems = todayItems, listsMap = listsMap)
            }
        }
    }

    @Composable
    private fun TodoWidgetContent(todayItems: List<TodoItem>, listsMap: Map<String, TodoList>) {
        val size = LocalSize.current
        val colors = GlanceTheme.colors

        val maxItems = when {
            size.height >= 220.dp -> 6
            size.height >= 160.dp -> 4
            else -> 2
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(WIDGET_ACTION_KEY to ACTION_OPEN_TODO)
                    )
                )
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TASKS",
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Counter Badge
                Box(
                    modifier = GlanceModifier
                        .background(colors.primaryContainer)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${todayItems.size}",
                        style = TextStyle(
                            color = colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Grounded Header Divider
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline)
            ) {}

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Task List Container with Weight to fill upper space
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                if (todayItems.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ALL CLEAR FOR TODAY",
                            style = TextStyle(
                                color = colors.outline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                } else {
                    val displayItems = todayItems.take(maxItems)
                    displayItems.forEachIndexed { index, item ->
                        val list = item.listId?.let { listsMap[it] }
                        TaskRowItem(item = item, list = list)
                        if (index < displayItems.size - 1) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }

            // Bottom Quick Action Row to eliminate dead space void
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(WIDGET_ACTION_KEY to ACTION_OPEN_TODO)
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ Add Task",
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun TaskRowItem(item: TodoItem, list: TodoList?) {
        val colors = GlanceTheme.colors

        val itemColor = list?.colorHex?.let { hex ->
            try {
                if (hex.isNotBlank() && hex != "#000000") {
                    androidx.compose.ui.graphics.Color(hex.toColorInt())
                } else null
            } catch (_: Exception) { null }
        } ?: TodoTheme.staticColors.accent

        val badgeColorProvider = ColorProvider(day = itemColor, night = itemColor)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Control
            CheckBox(
                checked = item.isCompleted,
                onCheckedChange = actionRunCallback<ToggleTodoTaskAction>(
                    actionParametersOf(ToggleTodoTaskAction.TASK_ID_KEY to item.id)
                ),
                colors = CheckboxDefaults.colors(
                    checkedColor = badgeColorProvider,
                    uncheckedColor = badgeColorProvider
                )
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Space color tinted icon/badge
            Box(
                modifier = GlanceModifier
                    .size(20.dp)
                    .background(badgeColorProvider)
                    .cornerRadius(6.dp)
                    .clickable(
                        actionRunCallback<ToggleTodoTaskAction>(
                            actionParametersOf(ToggleTodoTaskAction.TASK_ID_KEY to item.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (WidgetIconUtils.isEmoji(item.icon)) {
                    Text(
                        text = item.icon!!,
                        style = TextStyle(fontSize = 11.sp)
                    )
                } else {
                    val iconRes = WidgetIconUtils.getWidgetIconRes(item.icon, item.title)
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = null,
                        modifier = GlanceModifier.size(12.dp),
                        colorFilter = ColorFilter.tint(colors.onPrimary)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = item.title,
                style = TextStyle(
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
