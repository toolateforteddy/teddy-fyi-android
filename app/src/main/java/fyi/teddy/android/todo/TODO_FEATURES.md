# Todo App Features

This document tracks the features of the Teddy FYI Todo manager.
The Developer will add feature requests to the Requested Features section, and then, upon Prompting,
will look at the Requested Features section, implement each feature, then move that item to the
Current Features List.


## Current Features
- [x] **Local Persistence**: Uses Room database to store tasks on the device.
- [x] **Add Tasks**: Simple text input to create new todo items.
- [x] **Complete Tasks**: Checkbox to toggle completion status (with strikethrough UI).
- [x] **Delete Tasks**: Trash icon to permanently remove items.
- [x] **Reactive UI**: List updates automatically using Kotlin Flow.
- [x] **Insertion Improvement**: The keyboard "Done/Enter" key now submits the task.
- [x] **Clear All**: Added a trash icon in the header to clear all tasks (supports both Real and Debug modes).
- [x] **Edit Mode**: A pencil icon in the nav bar toggles "Edit Mode". The delete trash can icons on individual items are only visible when Edit Mode is active.
- [x] **Clear All Confirmation**: It is too easy to clear all items. Let's make it harder in two ways. First, don't show the trash can except in edit mode. Second, add a confirmation modal before actually clearing the data with explanation of what is about to happen, and a way to abort or confirm that this is desired.
- [x] **Completed Task Management**: Completed tasks should be removed from the screen after a 2 second delay to appreciate the joy of crossing something off the list. Subtasks should just be moved to the bottom of the subtask list upon completion. There should be a way to access the list of all completed tasks.
- [x] **Confetti Upon Completion**: When you check off an item, during the 2 seconds before the item clears, throw out a small burst of confetti across the screen.
- [x] **Item ordering**: In edit mode, items can be reordered using up/down arrows. There is also an overflow menu on each item with "Move to Top" and "Move to Bottom" options.
- [x] **Today Planning**: Added "Planning Today" and "Today's Tasks" modes. Checking an item in Planning mode marks it for today. At midnight, any uncompleted today-plan items are automatically reset.
- [x] **Mode Management**: Instead of the icon in the top to move through the three modes, add a mode switcher at the bottom so you can easily see which mode you are in, the selected state, and switch to either of the other two modes quickly.
- [x] **Back Arrow Visual Noise**: Removed the back arrow from the navigation bar.
- [x] **Recurring Tasks**: Some items, such as mowing the lawn, should be scheduled once a week. Create a way to create a recurrence schedule. The timer to decide when to schedule the task again should only start after the task is marked complete. For example, if mow the lawn shows up on my list on Monday, but I don't do it until Thursday, it should show back up on my list the following Thursday, not the following Monday.
- [x] **Edit Mode UX**: When in task edit mode, the box to add a new task is hidden to focus on editing existing tasks.
- [x] **Auth Integration**: Todo items are now linked to the authenticated user's ID. Existing unowned items are automatically assigned to the user upon login.
- [x] **Edit Task Name**: In the overflow menu in edit mode, add an option to edit the title of the task.
- [x] **Nested Tasks**: I should be able to create subtasks inside a single task. When viewing the task list, I should be able to toggle between showing nested tasks and not, by tapping on an arrow expand icon. When collapsed, the task should display X/Y, X being the number of completed subtasks, and Y being the total number of subtasks. If a subtask is added to the today list, then when viewing the today list, the main task should be shown, but only the one selected nested task underneath it. If a task with subtasks is added to the today list, then all subtasks should be added to the today list.
- [x] **Daily Tasks**: I want to be able to mark some tasks as recurring daily. It doesn't matter that I completed the task the previous day, it should still be on my todo list for today, auto added at 8am local time. For example, I should empty the dishwasher every day.
- [x] **Due Dates**: Set reminders and deadlines. Automatically add it to the today list whenever due date is within 2 days. But put it at the bottom, and add the due date in red text.
- [x] **Planning Mode Added Tasks**: When adding a task and in Planning Mode the task should already be checked as being planned for today. Just to save a second of time from adding and then having to check the button.


## Requested Features


## Planned Features
- [ ] **Cloud Sync**: Synchronize local Room database with the Rust backend.
- [ ] **Offline Support**: Gracefully handle data sync when connectivity returns.
- [ ] **Categories/Tags**: Organize tasks by type.
- [ ] **Prioritization**: Mark tasks as high priority with a star, forcing them to the top of all views.
- [ ] **Search & Global History**: A search bar to find tasks by title across active and completed lists.
- [ ] **Push Notifications**: Reminders for approaching due dates and a morning summary of "Today's Tasks".
- [ ] **Productivity Insights**: A stats dashboard showing total tasks completed, streaks for daily items, and weekly progress charts.
- [ ] **Rich Task Details**: Add multi-line notes and clickable URLs to individual tasks.
- [ ] **Push to tomorrow**: In Today Mode, add field to the edit mode overflow menu to "schedule for tomorrow". This will remove it from today's planned list, but at the end of the day after removing all the incomplete tasks from today, this task will be put back onto the today schedule so that it starts on the schedule.

## Bugs
- [x] **Dark Mode**: The items are displayed in black text on a black background. And the nav bar is white.
- [x] **Empty Title on Real Mode**: Fixed a race condition where the task title was being reset to empty before the async database save operation could read it.
- [x] **Move to Top/Bottom**: The topmost item shouldn't have the option to move to top. The bottommost item shouldn't have the option to move to bottom.
- [x] **Reorder items once only**: Fixed logic that failed when two items had the same position (common after migration).
- [x] **New Items Hidden**: Fixed a race condition in the DAO query where items added after the initial screen load were filtered out until a manual refresh. This bug came back.
- [x] **Nested Task Indentation**: Fixed indentation for top-level tasks and subtasks. Reduced expanding arrow size and margins. The indentation for subtasks is too subtle. Add a dash before each checkbox on subtasks.
- [x] **Adding Subtasks**: Subtask title input is now auto-focused with the keyboard open. The dialog remains open after adding an item to allow for multiple subtasks to be added in sequence.
- [x] **Right Caret instead of up caret**: For parent tasks, instead of an ^ for the collapsed state, use a >.
- [x] **Caret Nav**: Move the caret over to the right, so that the checkboxes can be closer to the left side without having to leave space for the caret.
- [x] **Subtasks in Today Mode**: If a subtask has been selected to be done today, it should show up on the today screen. And on that screen, it should show up nested under its parent task, even if the parent task hasn't been selected to be done today. The subtasks that have not been selected for today, should not show up.
- [x] **Planned Tasks in Backlog**: If a task has been selected to be done today, let's hide it from the "Backlog" tab to reduce the clutter.
- [x] **Rename Normal to Backlog**: Normal is a terrible name for anything. Let's call it the backlog.
- [x] **Subtasks in Today**: Right now, it is showing all tasks that have subtasks inside of Today, even if none of those subtasks have tasks scheduled to be tackled today. Only parent tasks that have scheduled subtasks should be rendered in Today view. Also, collapsing subtasks in Today view doesn't work at all. Nothing happens.
- [x] **Subtasks in Today Cont.**: Now, all parent tasks are being displayed in Today view, even if none of their subtasks have been scheduled for today. Also, tasks scheduled for today that are NOT parents or subtasks are missing from the page.