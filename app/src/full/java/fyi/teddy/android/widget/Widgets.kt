package fyi.teddy.android.widget

import android.content.Context

/**
 * The app's one way in to the home-screen widgets.
 *
 * The widgets themselves are a `full` feature: the grocery build ships on a Fire tablet, whose
 * launcher hosts no app widgets at all, so that flavour carries its own no-op copy of this
 * object and none of the widget code behind it. Shared code calls through here so it does not
 * have to know which build it is in.
 */
object Widgets {

    fun refreshTodo(context: Context) = WidgetUpdateHelper.updateAllTodoWidgets(context)

    fun refreshGrocery(context: Context) = WidgetUpdateHelper.updateAllGroceryWidgets(context)

    fun opensTodo(action: String?): Boolean = action == TodoTacticalWidget.ACTION_OPEN_TODO

    fun opensGrocery(action: String?): Boolean = action == GroceryWidget.ACTION_OPEN_GROCERY
}
