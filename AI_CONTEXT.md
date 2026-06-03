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