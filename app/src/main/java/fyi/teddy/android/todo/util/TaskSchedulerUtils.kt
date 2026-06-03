package fyi.teddy.android.todo.util

import java.time.LocalDate

object TaskSchedulerUtils {

    fun snoozeForMonths(date: LocalDate, months: Int): LocalDate {
        val target = date.plusMonths(months.toLong())
        
        // If the original date was the 29th, 30th or 31st and the new month is shorter, 
        // LocalDate.plusMonths(n) automatically adjusts to the last day of that month.
        // If the adjusted day is different from the original day, 
        // it means we hit the end of a shorter month. 
        // Requirement: If it was adjusted, move to the 1st of the next month.
        
        return if (target.dayOfMonth < date.dayOfMonth) {
            target.plusMonths(1).withDayOfMonth(1)
        } else {
            target
        }
    }

    fun snoozeForDays(date: LocalDate, days: Int): LocalDate {
        return date.plusDays(days.toLong())
    }

    /**
     * Returns today's date formatted as a standard string ("yyyy-MM-dd").
     */
    fun getTodayDateString(): String {
        return LocalDate.now().toString()
    }

    /**
     * Safely calculates the next recurrence time in milliseconds from a base epoch time.
     * Avoids potential integer overflow by casting intermediate values to Long.
     */
    fun calculateNextRecurrenceTime(baseTimeMs: Long, intervalDays: Int): Long {
        val intervalMs = intervalDays.toLong() * 24L * 60L * 60L * 1000L
        return baseTimeMs + intervalMs
    }
}
