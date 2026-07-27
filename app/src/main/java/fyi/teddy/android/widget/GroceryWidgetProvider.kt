package fyi.teddy.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import fyi.teddy.android.MainActivity
import fyi.teddy.android.R
import fyi.teddy.android.auth.UserSession
import fyi.teddy.android.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroceryWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_OPEN_GROCERY = "fyi.teddy.android.ACTION_OPEN_GROCERY"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150)
                val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)

                val density = context.resources.displayMetrics.density
                val widthPx = (minWidthDp * density).toInt().coerceAtLeast(100)
                val heightPx = (minHeightDp * density).toInt().coerceAtLeast(60)

                val session = UserSession()
                session.load(context)
                val userId = session.userId ?: ""

                val db = AppDatabase.getDatabase(context)
                val items = db.groceryDao().getAllItemsOneShot().filter {
                    !it.isDeleted && (it.userId == userId || userId.isBlank())
                }

                val bitmap = GroceryWidgetRenderer.renderGroceryCard(
                    groceryItems = items,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    density = density
                )

                val views = RemoteViews(context.packageName, R.layout.widget_grocery)
                views.setImageViewBitmap(R.id.widget_grocery_canvas_image, bitmap)

                val intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_GROCERY
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_grocery_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
