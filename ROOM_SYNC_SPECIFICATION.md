# Blueprint: Local-First Room Sync Engine

This document defines the mandatory architecture for Room Database schemas and the background Sync Worker. Any agent implementing new features must adhere to these state machine rules to ensure compatibility with the `teddy.fyi` backend.

## 1. Entity Standard Requirements
Every syncable table MUST include the following columns and configuration:

### Primary Key
- **Type**: `String` (UUID)
- **Generation**: Client-side generated (e.g., `java.util.UUID.randomUUID().toString()`) before insertion.

### Tracking Columns
| Column | Type | Purpose |
| :--- | :--- | :--- |
| `sync_state` | `String` / `Enum` | One of: `SYNCED`, `PENDING_INSERT`, `PENDING_UPDATE`, `PENDING_DELETE` |
| `version` | `Int` | Incremented by the server. Used for conflict resolution. |
| `is_deleted` | `Boolean` | Soft-delete flag. Default: `false`. |
| `last_modified` | `Long` | Local timestamp (Unix epoch) for UI sorting/logging. |

## 2. The Mutation State Machine
All database writes must follow these lifecycle rules to prevent data loss during sync:

### **Insert Action**
- Set `sync_state = PENDING_INSERT`.
- Set `version = 1`.
- Set `is_deleted = false`.

### **Update Action**
- **If** `sync_state == SYNCED`: Change to `PENDING_UPDATE`.
- **If** `sync_state == PENDING_INSERT`: Remain `PENDING_INSERT`.
- **If** `sync_state == PENDING_UPDATE`: Remain `PENDING_UPDATE`.

### **Delete Action**
- **If** `sync_state == PENDING_INSERT`: Perform a **Hard Delete** (remove from DB immediately).
- **If** `sync_state == SYNCED` or `PENDING_UPDATE`: Set `is_deleted = true` and `sync_state = PENDING_DELETE`.

## 3. DAO Pattern
DAOs must provide specific queries to support the Sync Worker:
- `getPendingSyncRecords()`: Returns all rows where `sync_state != SYNCED`.
- `upsertFromSync(list)`: A "Conflict Resolution" upsert that only updates local data if the incoming server `version` is higher than the local `version`, or if the local state is `SYNCED`.

## 4. Sync Worker Protocol (`WorkManager`)
The worker handles bidirectional synchronization in a single atomic transaction.

### **Workflow**
1. **Collect**: Query all tables for records where `sync_state != SYNCED`.
2. **Payload**: Construct a JSON object containing "deltas" (changes only).
3. **Transmit**: `POST /api/sync` (with `Authorization: Bearer <token>`).
4. **Reconcile**:
    - On `200 OK`: The server returns the "Final Truth" for the affected records.
    - Update local rows with server-provided `version` and set `sync_state = SYNCED`.
    - **Hard Delete** any local rows where `is_deleted == true` AND the server has acknowledged the deletion.

### **Retry Policy**
- Use **Exponential Backoff** (starting at 30 seconds).
- Constraints: `NetworkType.CONNECTED` is required.

## 5. Domain Isolation
Even when sharing a sync handler:
- Each data domain (e.g., Todo, Grocery, Finance) must have its own dedicated Room `Entity` and `Table`.
- Do not mix types in the sync payload; use polymorphic JSON handling or distinct keys per domain in the sync request.
