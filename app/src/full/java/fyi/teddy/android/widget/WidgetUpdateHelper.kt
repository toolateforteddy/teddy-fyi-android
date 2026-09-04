package fyi.teddy.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    fun updateAllTodoWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TodoTacticalWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateHelper", "Error updating Todo widget", e)
            }
        }
    }

    fun updateAllGroceryWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                GroceryWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateHelper", "Error updating Grocery widget", e)
            }
        }
    }
}
