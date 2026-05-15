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
- [x] **Debug Mode**: A toggle in the nav bar allows entering a sandbox mode. In this mode, the list is initialized with a copy of the real list, but changes are in-memory only and do not affect the persistent database.
- [x] **Edit Mode**: A pencil icon in the nav bar toggles "Edit Mode". The delete trash can icons on individual items are only visible when Edit Mode is active.
- [x] **Clear All Confirmation**: It is too easy to clear all items. Let's make it harder in two ways. First, don't show the trash can except in edit mode. Second, add a confirmation modal before actually clearing the data with explanation of what is about to happen, and a way to abort or confirm that this is desired.
- [x] **Completed Task Management**: Completed tasks should be removed from the screen after a 2 second delay to appreciate the joy of crossing something off the list. Subtasks should just be moved to the bottom of the subtask list upon completion. There should be a way to access the list of all completed tasks.

## Requested Features

## Planned Features
- [ ] **Auth Integration**: Link todo items to the authenticated user's ID.
- [ ] **Cloud Sync**: Synchronize local Room database with the Rust backend.
- [ ] **Offline Support**: Gracefully handle data sync when connectivity returns.
- [ ] **Categories/Tags**: Organize tasks by type.
- [ ] **Due Dates**: Set reminders and deadlines.
- [ ] **Nested Tasks**: I should be able to create subtasks in side a single task. When viewing the task list, I should be able to toggle between showing nested tasks and not, by tapping on an arrow expand icon. When collapsed, the task should display X/Y, X being the number of completed subtasks, and Y being the total number of subtasks.
- [ ] **Daily Tasks**: I want to be able to mark some tasks as recurring daily. It doesn't matter that I completed the task the previous day, it should still be on my todo list for today, auto added at 8am local time. For example, I should empty the dishwasher every day.
- [ ] **Non-Daily Recurring Tasks**: Some items, such as mowing the lawn, should be scheduled once a week. Create a way to create a recurrence schedule. The timer to decide when to schedule the task again should only start after the task is marked complete. For example, if mow the lawn shows up on my list on Monday, but I don't do it until Thursday, it should show back up on my list the following Thursday, not the following Monday.

## Bugs
- [x] **Dark Mode**: The items are displayed in black text on a black background. And the nav bar is white.
- [x] **Empty Title on Real Mode**: Fixed a race condition where the task title was being reset to empty before the async database save operation could read it.
