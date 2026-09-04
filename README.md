# Teddy FYI Android App

The **Teddy FYI Android App** is a versatile personal organization suite featuring integrated Todo and Grocery management tools, built local-first with bidirectional cloud sync.

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
- **Shared Lists**: Households can share a grocery list through invite codes.

---

## Technical Highlights
- **Reactive UI**: Built with Jetpack Compose and Kotlin Flow, ensuring the UI updates automatically as database content changes.
- **Local-First Persistence**: Both modules use a shared Room database, so the app is fully usable offline.
- **Bidirectional Cloud Sync**: Every syncable row carries a client-generated UUID plus `sync_state` / `version` / `is_deleted`
  columns. A WorkManager job (`SyncWorker`) exchanges batched deltas with a Rust backend over a single
  `POST /api/sync` round-trip, with adaptive backoff and a metered-network guard.
- **Home-Screen Widgets**: Two Jetpack Glance widgets (tactical todo grid, grocery list) with custom canvas renderers.
- **On-Device AI (opt-in)**: MediaPipe-backed grocery categorization that no-ops unless a model is side-loaded.

## Getting Started
```bash
./gradlew assembleDebug           # both flavours; or: make build
./gradlew assembleGroceryDebug    # the grocery-only build; or: make build-grocery
./gradlew testFullDebugUnitTest   # or: make test
make install && make run       # adb install + launch on the first connected device
```
See [setup.md](setup.md) for SDK prerequisites and [CODEBASE_MAP.md](CODEBASE_MAP.md) for an
architecture tour.

## Future Roadmap
Planned enhancements include receipt scanning and advanced productivity analytics.
