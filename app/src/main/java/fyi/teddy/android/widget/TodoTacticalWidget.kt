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

class TodoTacticalWidget : GlanceAppWidget() {

    companion object {
        const val ACTION_OPEN_TODO = "fyi.teddy.android.ACTION_OPEN_TODO"
    }

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val session = UserSession()
        session.load(context)
        val userId = session.userId ?: ""

        val db = AppDatabase.getDatabase(context)
        val allItems = db.todoDao().getAllItemsOneShot()
        val todayItems = allItems.filter {
            !it.isDeleted && !it.isCompleted && (it.userId == userId || userId.isBlank())
        }

        provideContent {
            GlanceTheme {
                TodoWidgetContent(todayItems = todayItems)
            }
        }
    }

    @Composable
    private fun TodoWidgetContent(todayItems: List<TodoItem>) {
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
                .clickable(actionStartActivity<MainActivity>())
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

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (todayItems.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
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
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    val displayItems = todayItems.take(maxItems)
                    displayItems.forEachIndexed { index, item ->
                        TaskRowItem(item = item, index = index)
                        if (index < displayItems.size - 1) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskRowItem(item: TodoItem, index: Int) {
        val colors = GlanceTheme.colors

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category status accent indicator
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .background(colors.primary)
                    .cornerRadius(4.dp)
            ) {}

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
