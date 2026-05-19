# Teddy FYI Android App

The **Teddy FYI Android App** is a versatile personal organization suite featuring integrated Todo and Grocery management tools, designed for local productivity with cloud-readiness.

## Core Functionality
- **Dual-Mode Organization**: The app provides dedicated modules for both Task Management (Todo) and Grocery Shopping.
- **Authentication & User Profiles**: 
    - Supports Google Sign-In with persistent session caching.
    - Displays user profile information upon successful login.
    - Includes a development-only "Skip Auth" mode for testing.
- **System Monitoring**: Includes a dashboard-integrated health check for `teddy.fyi` services to ensure connectivity.

---

## Todo Manager
The Todo module focuses on granular task tracking and productivity:
- **Task Organization**: Supports subtasks, due dates, daily recurring tasks, and custom recurrence schedules.
- **Planning Features**: Includes "Today's Tasks" mode, prioritizing tasks with upcoming deadlines, and automatic resetting of daily tasks.
- **UX & Delights**: Features a "Confetti" animation upon task completion, automatic removal of completed items, and intuitive management through edit modes and sorting options.

## Grocery List Manager
The Grocery module streamlines shopping and item gathering:
- **Smart Shopping**: Features "Need," "Planning," and "Shopping" modes to optimize the shopping experience based on store availability.
- **Store & Category Management**: Allows grouping items by store/aisle, setting store defaults, and tracking item prices across different stores to identify the best value.
- **Intelligent Additions**: Uses item autocomplete and a "Recommended" feature to quickly re-add frequently purchased items.

---

## Technical Highlights
- **Reactive UI**: Built with Jetpack Compose and Kotlin Flow, ensuring the UI updates automatically as database content changes.
- **Local Persistence**: Both modules utilize the Room database for robust offline performance.
- **Cloud-Ready Architecture**: While currently built for local offline use, the infrastructure is prepared for upcoming backend synchronization.

## Future Roadmap
Planned enhancements include full Cloud Sync with a Rust-based backend, receipt scanning, shared lists for collaboration, and advanced productivity analytics.
