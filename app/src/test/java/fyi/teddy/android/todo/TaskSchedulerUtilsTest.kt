package fyi.teddy.android.todo

import fyi.teddy.android.todo.util.TaskSchedulerUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskSchedulerUtilsTest {

    @Test
    fun snoozeForMonths_handlesRollover() {
        // Jan 30 + 1 month -> March 1st (Feb has 28/29 days)
        val jan30 = LocalDate.of(2024, 1, 30)
        val result = TaskSchedulerUtils.snoozeForMonths(jan30, 1)
        
        assertEquals(LocalDate.of(2024, 3, 1), result)
    }

    @Test
    fun snoozeForMonths_normalCase() {
        // Jan 10 + 1 month -> Feb 10
        val jan10 = LocalDate.of(2024, 1, 10)
        val result = TaskSchedulerUtils.snoozeForMonths(jan10, 1)
        
        assertEquals(LocalDate.of(2024, 2, 10), result)
    }

    @Test
    fun snoozeForDays_addsDays() {
        val today = LocalDate.of(2024, 1, 1)
        val result = TaskSchedulerUtils.snoozeForDays(today, 5)
        
        assertEquals(LocalDate.of(2024, 1, 6), result)
    }
}
