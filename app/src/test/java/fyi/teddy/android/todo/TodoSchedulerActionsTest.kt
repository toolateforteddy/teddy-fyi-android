package fyi.teddy.android.todo

import fyi.teddy.android.todo.util.TaskSchedulerUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodoSchedulerActionsTest {

    // Testing SnoozeForMonths (10 tests for edge cases)
    @Test fun testSnoozeMonths_Jan30_1Month() = assertEquals(LocalDate.of(2024, 3, 1), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 1, 30), 1))
    @Test fun testSnoozeMonths_Feb29_1Month() = assertEquals(LocalDate.of(2024, 3, 29), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 2, 29), 1))
    @Test fun testSnoozeMonths_NormalCase() = assertEquals(LocalDate.of(2024, 5, 15), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 4, 15), 1))
    @Test fun testSnoozeMonths_YearRollover() = assertEquals(LocalDate.of(2025, 1, 15), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 12, 15), 1))
    @Test fun testSnoozeMonths_MultipleMonths() = assertEquals(LocalDate.of(2024, 8, 15), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 4, 15), 4))
    @Test fun testSnoozeMonths_LeapYear_Feb29() = assertEquals(LocalDate.of(2025, 3, 1), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 2, 29), 12))
    @Test fun testSnoozeMonths_March31_1Month() = assertEquals(LocalDate.of(2024, 5, 1), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 3, 31), 1))
    @Test fun testSnoozeMonths_ZeroMonths() = assertEquals(LocalDate.of(2024, 1, 30), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 1, 30), 0))
    @Test fun testSnoozeMonths_LargeAmount() = assertEquals(LocalDate.of(2025, 4, 15), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 4, 15), 12))
    @Test fun testSnoozeMonths_LeapYearFeb30Fake() = assertEquals(LocalDate.of(2024, 3, 1), TaskSchedulerUtils.snoozeForMonths(LocalDate.of(2024, 1, 30), 1))

    // Testing SnoozeForDays (10 tests for edge cases)
    @Test fun testSnoozeDays_Standard() = assertEquals(LocalDate.of(2024, 1, 6), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 1, 1), 5))
    @Test fun testSnoozeDays_AcrossMonths() = assertEquals(LocalDate.of(2024, 2, 2), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 1, 31), 2))
    @Test fun testSnoozeDays_AcrossYear() = assertEquals(LocalDate.of(2025, 1, 2), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 12, 31), 2))
    @Test fun testSnoozeDays_ZeroDays() = assertEquals(LocalDate.of(2024, 1, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 1, 1), 0))
    @Test fun testSnoozeDays_Negative() = assertEquals(LocalDate.of(2023, 12, 31), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 1, 1), -1))
    @Test fun testSnoozeDays_LeapYear() = assertEquals(LocalDate.of(2024, 3, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 2, 28), 2))
    @Test fun testSnoozeDays_NonLeapYear() = assertEquals(LocalDate.of(2023, 3, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2023, 2, 28), 1))
    @Test fun testSnoozeDays_LargeValue() = assertEquals(LocalDate.of(2025, 1, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 1, 1), 366))
    @Test fun testSnoozeDays_AcrossMonthBoundary() = assertEquals(LocalDate.of(2024, 4, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 3, 31), 1))
    @Test fun testSnoozeDays_MultipleMonths() = assertEquals(LocalDate.of(2024, 5, 1), TaskSchedulerUtils.snoozeForDays(LocalDate.of(2024, 3, 1), 61))
}
