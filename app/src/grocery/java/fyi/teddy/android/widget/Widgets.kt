package fyi.teddy.android.widget

import android.content.Context

/**
 * The grocery build's copy of the widget seam: there is nothing behind it.
 *
 * That build is for a Fire tablet, and the Fire launcher does not host app widgets, so the
 * widget code, its layouts and its manifest receivers all live in `src/full/` and this flavour
 * ships without them. The refresh calls do nothing and no intent ever came from a widget, so
 * the `opens*` questions are always answered no.
 */
object Widgets {

    fun refreshTodo(context: Context) = Unit

    fun refreshGrocery(context: Context) = Unit

    fun opensTodo(action: String?): Boolean = false

    fun opensGrocery(action: String?): Boolean = false
}
