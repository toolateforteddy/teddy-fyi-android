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
3.  **Sync**: Local-only features are implemented first, with Remote API integration added as a secondary "Sync" step.
