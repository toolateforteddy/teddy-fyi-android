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
*   **Remote API**: Communicates with the backend using **Ktor** (OkHttp engine) with `kotlinx.serialization`. Every URL lives in `network/ApiRoutes.kt`.
*   **Sync Logic**: Responsible for reconciling local database state with remote API changes.

### 4. Auth Agent (Google Identity)
*   **Purpose**: Handles user authentication.
*   **Implementation**: Uses `androidx.credentials` with `googleid` provider.
*   **Lifecycle**: Manages the auth token lifecycle, caching, and token refresh/validation before API requests.

## Communication Protocol
*   **API Interaction**: RESTful endpoints with JSON payloads.
*   **Auth Header**: All authenticated requests must include the `Authorization: Bearer <token>` header, retrieved from the Auth Agent.
*   **Security**: Tokens are persisted in DataStore Preferences, encrypted with Google Tink (`AES256_GCM`, keystore-backed), via `EncryptedDataStore`/`TokenStorage`. They are validated against the Auth Agent on launch.

## Concurrency Model
*   **Kotlin Coroutines**: All asynchronous operations (network calls, database access) are performed using Coroutines.
*   **Flows**: Used for reactive data streaming from the database to the UI.

## Development Workflow
1.  **Feature Definition**: New requirements are tracked in module-specific `*_FEATURES.md` files.
2.  **Implementation**: Features are implemented following the Repository-ViewModel-UI flow.
3.  **Testing & Validation**: A feature, bug fix, or architectural migration is only considered complete once the corresponding unit and/or UI test suites are updated to reflect the new behavior and all tests are passing.
4.  **Sync**: Local-only features are implemented first, with Remote API integration added as a secondary "Sync" step.

## Architectural Evolution

Already shipped (do not propose these as new work):
*   **Networking**: Ktor replaced `HttpURLConnection`.
*   **Cloud Sync**: Phase 2 bidirectional sync is live against the Rust backend.
*   **Static Analysis**: Detekt runs in CI against `app/detekt-baseline.xml`; new violations fail the build.

Still open:
*   **DI Transition**: Migrate to Hilt for automatic dependency management to improve testability and decouple components. Wiring is currently manual (`ViewModelProvider.Factory` plus object singletons).
*   **ViewModel Refactor**: Enforce Unidirectional Data Flow (UDF) where all UI state is managed within ViewModels.
*   **Modularization**: Transition to a feature-based multi-module Gradle project structure to improve build times and maintainability.

## Database Integrity Protocol
*   **Schema Changes**: Any modification to an `@Entity` (e.g., adding/modifying columns) MUST be accompanied by an immediate update to the `AppDatabase` version and the addition of an `AutoMigration` (or manual `Migration` if needed) to ensure the local database schema remains in sync with the codebase.
*   **Verification**: Always inspect `AppDatabase.kt` and the `exportSchema` JSON files whenever an entity class is modified.


## Further Reading
*   [CODEBASE_MAP.md](CODEBASE_MAP.md) — package layout, the sync engine, known gotchas. Start here.
*   [AI_CONTEXT.md](AI_CONTEXT.md) — the sync state machine and mutation lifecycle rules.
*   [AI_SCHEDULING_RULES.md](AI_SCHEDULING_RULES.md) — snooze/recurrence semantics.
*   [ROOM_SYNC_SPECIFICATION.md](ROOM_SYNC_SPECIFICATION.md), [AUTH_SPECIFICATION.md](AUTH_SPECIFICATION.md) — wire formats.
