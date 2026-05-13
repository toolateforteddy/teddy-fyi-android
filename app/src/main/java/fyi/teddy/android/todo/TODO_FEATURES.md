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

## Requested Features

## Planned Features
- [ ] **Auth Integration**: Link todo items to the authenticated user's ID.
- [ ] **Cloud Sync**: Synchronize local Room database with the Rust backend.
- [ ] **Offline Support**: Gracefully handle data sync when connectivity returns.
- [ ] **Categories/Tags**: Organize tasks by type.
- [ ] **Due Dates**: Set reminders and deadlines.

## Bugs
- [ ] **Dark Mode**: The items are displayed in black text on a black background. And the nav bar is white.