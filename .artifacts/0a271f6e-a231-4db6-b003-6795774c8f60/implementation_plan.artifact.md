# Fix: Swipe-to-Dismiss Stuck in Dismissed State

The user reports that the swipe-to-unschedule action "gets stuck with the red bar" on the screen after triggering. This is likely because the `dismissState.reset()` call in the `LaunchedEffect` is being interrupted or not completing correctly when the UI recomposes following the data update.

## Proposed Changes

### [Component: UI]

#### [MODIFY] [TodoItemRow.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/todo/ui/components/TodoItemRow.kt)
- Refactor `rememberSwipeToDismissBoxState` to use the `confirmValueChange` lambda.
- Move the action logic (`onIntent`) into `confirmValueChange`.
- Return `false` from `confirmValueChange` for all non-settled values. This forces the swipe to always snap back to the `Settled` state immediately after the action is triggered, preventing it from getting stuck in the dismissed state.
- Remove the `LaunchedEffect(dismissState.currentValue)` as it will no longer be necessary.
- Ensure `isConfirmed` is reset appropriately.

## Verification Plan

### Manual Verification
- Deploy the app.
- Go to the Today screen.
- Swipe left on a task and hold for the haptic feedback.
- Release and verify the task is unscheduled AND the row (if it remains) or the screen state resets cleanly without any stuck red bars.
- Test swipe-to-schedule (swipe right) on the Backlog screen as well.
