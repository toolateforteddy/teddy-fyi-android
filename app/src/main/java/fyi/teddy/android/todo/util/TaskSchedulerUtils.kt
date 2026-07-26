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

    fun calculateNextRecurrenceTime(baseTimeMs: Long, rrule: String): Long {
        val parts = parseRrule(rrule)
        val freq = parts["FREQ"] ?: "DAILY"
        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1

        val baseDate = java.time.Instant.ofEpochMilli(baseTimeMs)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        val nextDate = when (freq) {
            "DAILY" -> baseDate.plusDays(interval.toLong())
            "WEEKLY" -> calculateNextWeeklyDate(baseDate, interval, parts["BYDAY"])
            "MONTHLY" -> baseDate.plusMonths(interval.toLong())
            else -> baseDate.plusDays(interval.toLong())
        }
        return nextDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseRrule(rrule: String): Map<String, String> {
        return rrule.split(";").associate {
            val kv = it.split("=")
            if (kv.size == 2) kv[0].uppercase() to kv[1].uppercase() else "" to ""
        }
    }

    private fun calculateNextWeeklyDate(baseDate: LocalDate, interval: Int, byDayStr: String?): LocalDate {
        val targetDays = byDayStr?.split(",")?.mapNotNull { parseDayOfWeek(it) }.orEmpty()
        if (targetDays.isEmpty()) {
            return baseDate.plusWeeks(interval.toLong())
        }
        var candidate = baseDate.plusDays(1)
        while (!targetDays.contains(candidate.dayOfWeek)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun parseDayOfWeek(dayCode: String): java.time.DayOfWeek? {
        return when (dayCode) {
            "MO" -> java.time.DayOfWeek.MONDAY
            "TU" -> java.time.DayOfWeek.TUESDAY
            "WE" -> java.time.DayOfWeek.WEDNESDAY
            "TH" -> java.time.DayOfWeek.THURSDAY
            "FR" -> java.time.DayOfWeek.FRIDAY
            "SA" -> java.time.DayOfWeek.SATURDAY
            "SU" -> java.time.DayOfWeek.SUNDAY
            else -> null
        }
    }
}
