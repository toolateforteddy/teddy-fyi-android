# Project Agents & Architecture

This document outlines the architectural agents, patterns, and communication protocols used in the Teddy FYI Android project.

## Overview
The application is a modular Android client built with Kotlin, Jetpack Compose, and Room. It leverages Google Authentication to securely interact with a backend API.

## Core Architectural Agents

### 1. UI Layer (Compose)
*   **Purpose**: Manages user interaction and state presentation.
*   **Pattern**: Uses ViewModel-based state management with Jetpack Compose `StateFlow` to observe data changes reactively.
*   **Responsibility**: Collects data from Domain/Data layers and maps it to UI states.

### 2. ViewModel Layer
*   **Purpose**: Acts as the bridge between the UI and the data layer.
*   **Responsibility**: Handles business logic, UI state preservation, and event handling. Uses `viewModelScope` for structured concurrency.

### 3. Data Layer (Repository Pattern)
*   **Purpose**: Abstracts the data source (Local vs. Remote).
*   **Local Persistence**: Utilizes **Room Database** for offline-first capabilities.
*   **Remote API**: Communicates with the backend using **Retrofit** or **Ktor** (to be confirmed).
*   **Sync Logic**: Responsible for reconciling local database state with remote API changes.

### 4. Auth Agent (Google Identity)
*   **Purpose**: Handles user authentication.
*   **Implementation**: Uses `androidx.credentials` with `googleid` provider.
*   **Lifecycle**: Manages the auth token lifecycle, caching, and token refresh/validation before API requests.

## Communication Protocol
*   **API Interaction**: RESTful endpoints with JSON payloads.
*   **Auth Header**: All authenticated requests must include the `Authorization: Bearer <token>` header, retrieved from the Auth Agent.
*   **Security**: Sensitive data is cached securely in `SharedPreferences` (or EncryptedSharedPreferences) and validated against the Auth Agent on launch.

## Concurrency Model
*   **Kotlin Coroutines**: All asynchronous operations (network calls, database access) are performed using Coroutines.
*   **Flows**: Used for reactive data streaming from the database to the UI.

## Development Workflow
1.  **Feature Definition**: New requirements are tracked in module-specific `*_FEATURES.md` files.
2.  **Implementation**: Features are implemented following the Repository-ViewModel-UI flow.
3.  **Testing & Validation**: A feature, bug fix, or architectural migration is only considered complete once the corresponding unit and/or UI test suites are updated to reflect the new behavior and all tests are passing.
4.  **Sync**: Local-only features are implemented first, with Remote API integration added as a secondary "Sync" step.

## Future Architectural Evolution
To ensure the app scales effectively, the following architectural goals are prioritized:
*   **DI Transition**: Migrate to Hilt for automatic dependency management to improve testability and decouple components.
*   **ViewModel Refactor**: Enforce Unidirectional Data Flow (UDF) where all UI state is managed within ViewModels.
*   **Networking Modernization**: Migrate from legacy `HttpURLConnection` to Ktor for efficient, type-safe API communication.
*   **Modularization**: Transition to a feature-based multi-module Gradle project structure to improve build times and maintainability.
*   **Static Analysis**: Integrate Detekt or Ktlint to maintain high code quality standards.

## Database Integrity Protocol
*   **Schema Changes**: Any modification to an `@Entity` (e.g., adding/modifying columns) MUST be accompanied by an immediate update to the `AppDatabase` version and the addition of an `AutoMigration` (or manual `Migration` if needed) to ensure the local database schema remains in sync with the codebase.
*   **Verification**: Always inspect `AppDatabase.kt` and the `exportSchema` JSON files whenever an entity class is modified.


# AI Context: Local-First Synchronization Engine (Phase 2)

## Project Overview
This project is an Android app combining Grocery Management and To-Do/Task tracking. It uses a Local-First architecture. The app is moving from Phase 1 (Local-only) to Phase 2 (Cloud Sync with a Rust Backend).

## Core Architecture & State Machine
- **Local DB:** Room (SQLite)
- **Primary Keys:** Every syncable row MUST use a client-side generated UUID (`String`).
- **Sync Worker:** Android `WorkManager` with exponential backoff.
- **Sync Strategy:** Single endpoint transaction (`POST /api/sync`). It handles bidirectional sync (uploads local changes and downloads remote changes in one atomic round-trip).

## Local Schema Enhancements (Required for Phase 2)
Every syncable Room Entity must include these tracking columns:
1. `sync_state`: Enum (`SYNCED`, `PENDING_INSERT`, `PENDING_UPDATE`, `PENDING_DELETE`)
2. `version`: Int (Increments on server validation, client tracks it for conflict resolution)
3. `is_deleted`: Boolean (Soft-delete flag for local offline deletions)

### Local Mutation Lifecycle Rules:
- **Insert:** Set UUID, `sync_state = PENDING_INSERT`, `version = 1`.
- **Update:** If `SYNCED`, flip to `PENDING_UPDATE`. If `PENDING_INSERT`, remain `PENDING_INSERT`.
- **Delete:** If `PENDING_INSERT`, hard-delete locally. If `SYNCED` or `PENDING_UPDATE`, set `is_deleted = true` and `sync_state = PENDING_DELETE`.

## Phase 4 Strategy (Keep in mind for Modularization)
Do not optimize for separate apps yet, but structure code modularly. Data domains (Todo vs. Grocery) must have **completely isolated Room tables and entities** because their features diverge heavily:
- **Todo:** Fields for due dates, recurrence rules, and subtasks. Private to the user.
- **Grocery:** Fields for store IDs, purchase history, and checking states. Shared household scope.

## Expectations for Gemini Agent
When generating code, modifying Room DAOs, or writing WorkManager payloads:
1. Ensure all network payloads are structured as batch deltas (not single REST mutations).
2. Maintain strict separation between UI layers and the background sync engine.
3. Adhere to the `sync_state` lifecycle state machine on every database write.