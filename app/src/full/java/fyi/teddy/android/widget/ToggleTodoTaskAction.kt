package fyi.teddy.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import fyi.teddy.android.data.AppDatabase

class ToggleTodoTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val db = AppDatabase.getDatabase(context)
        val item = db.todoDao().getItemByIdOneShot(taskId)
        if (item != null) {
            val updated = item.copy(
                isCompleted = !item.isCompleted,
                syncState = "PENDING_UPDATE"
            )
            db.todoDao().updateItem(updated)
            WidgetUpdateHelper.updateAllTodoWidgets(context)
        }
    }

    companion object {
        val TASK_ID_KEY = ActionParameters.Key<String>("task_id")
    }
}
