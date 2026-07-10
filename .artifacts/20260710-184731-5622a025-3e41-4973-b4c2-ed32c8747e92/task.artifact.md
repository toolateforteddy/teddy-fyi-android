# Task: Preserve Last Sync Time Across Logout

- [x] Research and Planning
    - [x] Identify where `last_synced_at` is stored
    - [x] Identify where it is cleared on logout
    - [x] Create implementation plan
- [x] Implementation
    - [x] Create `UserSyncMetadata` Room entity and DAO
    - [x] Update `AppDatabase` with migration to version 34
    - [x] Update `SyncWorker` to use per-user database metadata
    - [x] Update `DebugScreen` to display per-user metadata
    - [x] Update `GroceryViewModel` to clear user metadata on list join
- [x] Verification
    - [x] Create unit test for `UserSyncMetadataDao`
    - [x] Manually verify per-user persistence across logout/login
