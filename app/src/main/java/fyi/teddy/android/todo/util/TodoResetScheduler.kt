package fyi.teddy.android.todo.util

import android.content.Context
import androidx.core.content.edit
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.repository.resetDailyItems
import fyi.teddy.android.todo.repository.resetPlannedItems
import java.util.Calendar

/**
 * Helper class responsible for checking periodic time boundaries
 * and resetting daily and planned to-do tasks accordingly.
 * Decouples SharedPreferences and Calendar computations from the ViewModel.
 */
class TodoResetScheduler(
    private val context: Context,
    private val repository: TodoRepository
) {
    /**
     * Checks if the time has crossed midnight or 8:00 AM since the last reset
     * and performs database resets for the specified user.
     */
    suspend fun checkAndResetDailyTasks(userId: String) {
        val sharedPref = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
        val lastReset = sharedPref.getLong("last_reset_time", 0)
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Midnight Reset check
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val midnight = calendar.timeInMillis
        if (now >= midnight && lastReset < midnight) {
            repository.resetPlannedItems(userId)
        }

        // 8 AM Reset check
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        val eightAm = calendar.timeInMillis
        if (now >= eightAm && lastReset < eightAm) {
            repository.resetDailyItems(userId)
        }
        sharedPref.edit { putLong("last_reset_time", now) }
    }
}
