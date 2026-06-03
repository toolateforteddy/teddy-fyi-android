# Scheduling & Synchronization Logic Rules

This document outlines the source of truth for date manipulation and scheduling logic in the Teddy FYI Android project.

## 1. Date Format
*   **Storage**: All scheduled dates must be stored as **ISO 8601 (`yyyy-MM-dd`)** Strings in Room (`scheduledDate`).
*   **Nullable**: A `null` value indicates the task is not scheduled.

## 2. Snooze Logic (Business Rules)
Any changes to task scheduling must utilize the `TaskSchedulerUtils` helper class.
*   **Snooze for Months**: 
    *   Adds `N` months to the current date.
    *   **Invalid Date Handling**: If the resultant day of the month is invalid (e.g., Feb 30th), the task must be scheduled for the **1st day of the following month** (e.g., March 1st).
*   **Recurrence Integrity**: Snoozing a task **must not** impact or reset the task's recurrence schedule. Recurrence patterns only reset upon task completion.

## 3. UI/UX Rules
*   **Completed Tasks**: Once marked complete, a task moves to the general "Completed Tasks" list regardless of its original scheduled date.
*   **Scheduled View**: The "Scheduled" view must only display future-dated, non-completed tasks, ordered chronologically.

## 4. Migration Protocol
*   **Atomic Migrations**: Database schema changes (like adding columns) must be handled in a single version step (e.g., `16 -> 17`).
*   **Cleanup**: When removing old flags (e.g., `isPlannedForToday`), the migration must include the transition of existing data to the new schema and use a temporary table to drop/rename columns in SQLite if necessary.

## 5. Testing
*   **Edge Case Coverage**: Scheduling logic must be validated against edge cases:
    *   Leap years (e.g., Feb 29th).
    *   End-of-month rollovers (e.g., 30th/31st of months with 31 days moving into shorter months).
    *   Year rollovers (e.g., December to January).
