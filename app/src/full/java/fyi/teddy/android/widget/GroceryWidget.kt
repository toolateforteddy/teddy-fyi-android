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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import fyi.teddy.android.grocery.data.GroceryItem

import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf

class GroceryWidget : GlanceAppWidget() {

    companion object {
        const val ACTION_OPEN_GROCERY = "fyi.teddy.android.ACTION_OPEN_GROCERY"
        val WIDGET_ACTION_KEY = ActionParameters.Key<String>("widget_action")
    }

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val session = UserSession()
        session.load(context)
        val userId = session.userId ?: ""

        val db = AppDatabase.getDatabase(context)
        val allItems = db.groceryDao().getAllItemsOneShot()
        val activeItems = allItems.filter {
            !it.isDeleted && it.isActive && (it.userId == userId || userId.isBlank())
        }

        provideContent {
            GlanceTheme {
                GroceryWidgetContent(activeItems = activeItems)
            }
        }
    }

    @Composable
    private fun GroceryWidgetContent(activeItems: List<GroceryItem>) {
        val size = LocalSize.current
        val colors = GlanceTheme.colors

        val unboughtItems = activeItems
            .filter { !it.isBought }
            .sortedBy { it.name.lowercase() }
        val toBuyCount = unboughtItems.size

        val isCompact = size.height < 110.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(WIDGET_ACTION_KEY to ACTION_OPEN_GROCERY)
                    )
                )
        ) {
            if (isCompact) {
                // Compact Horizontal View
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$toBuyCount",
                        style = TextStyle(
                            color = colors.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Column {
                        Text(
                            text = if (toBuyCount == 1) "ITEM ON LIST" else "ITEMS ON LIST",
                            style = TextStyle(
                                color = colors.onBackground,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${activeItems.size} TOTAL ON LIST",
                            style = TextStyle(
                                color = colors.outline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            } else {
                // Expanded View with Header + Item List
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GROCERY LIST",
                        style = TextStyle(
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Box(
                        modifier = GlanceModifier
                            .background(colors.primaryContainer)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$toBuyCount",
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

                // Item List Container with Weight to fill upper space
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    if (unboughtItems.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ALL STOCKED!",
                                style = TextStyle(
                                    color = colors.outline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(unboughtItems, itemId = { it.id.hashCode().toLong() }) { item ->
                                Column(modifier = GlanceModifier.fillMaxWidth()) {
                                    GroceryRowItem(item = item)
                                    Spacer(modifier = GlanceModifier.height(5.dp))
                                }
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
                                actionParametersOf(WIDGET_ACTION_KEY to ACTION_OPEN_GROCERY)
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Add Item",
                        style = TextStyle(
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun GroceryRowItem(item: GroceryItem) {
        val colors = GlanceTheme.colors

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                // The scrolling list swallows the root container's click, so each
                // row carries its own way back into the app.
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(WIDGET_ACTION_KEY to ACTION_OPEN_GROCERY)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(colors.primary)
                    .cornerRadius(3.dp)
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = item.name,
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
