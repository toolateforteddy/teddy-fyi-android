# Per-User Sync Metadata Persistence

The application currently uses a global `last_synced_at` timestamp in `SharedPreferences`, which is lost on logout because `UserSession.clear()` wipes the preferences. To support multi-user scenarios and ensure that each user maintains their own sync "high-watermark" across logout/login cycles, we are migrating this metadata to a dedicated local Room table.

## Proposed Changes

### Data Component

#### [NEW] [UserSyncMetadata.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/data/UserSyncMetadata.kt)
- Create a new Entity `UserSyncMetadata` with `userId` as the primary key and `lastSyncedAt` as a nullable String.
- Create `UserSyncMetadataDao` with methods to `upsert`, `getLastSyncedAt`, `clear(userId)`, and `clearAll()`.

#### [AppDatabase.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/data/AppDatabase.kt)
- Add `UserSyncMetadata` to the `@Database` entities list.
- Bump version to `34`.
- Add `MIGRATION_33_34` to create the `user_sync_metadata` table.

---

### Sync Engine

#### [SyncWorker.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/network/SyncWorker.kt)
- Update `doWork()` to fetch `lastSyncedAt` from `db.userSyncMetadataDao()` using the current `session.userId`.
- Add a migration step in `doWork()` to transition existing `last_synced_at` from `SharedPreferences` to the new database table if the database entry is missing.
- Update `doWork()` to save the new `serverTimestamp` to `db.userSyncMetadataDao()` upon successful sync.
- Update `enqueueIfNecessary()` to use the new database table for checking if a sync is required.

---

### UI & Auth Components

#### [DebugScreen.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/ui/screens/DebugScreen.kt)
- Update `reloadSyncMetadata()` to fetch the timestamp from `db.userSyncMetadataDao()` for the currently logged-in user.
- Update the "RESET METADATA" button to clear the specific user's metadata in the database instead of the global preference.

#### [GroceryViewModel.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/grocery/ui/GroceryViewModel.kt)
- Update `joinList()` to clear the user's sync metadata in the database when joining a new list, ensuring a fresh sync to pull new list metadata.

#### [UserSession.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/auth/UserSession.kt)
- The `clear()` method no longer needs to be modified to preserve `last_synced_at` because the database persists across logouts naturally, and the metadata is now stored there.

## Verification Plan

### Manual Verification
1. **Initial Sync**:
   - Log in as User A.
   - Perform a sync.
   - Verify `last_synced_at` is displayed in the Debug Screen.
2. **Logout & Login Same User**:
   - Log out User A.
   - Log back in as User A.
   - Verify `last_synced_at` is still present and correct (not null).
3. **Logout & Login Different User**:
   - Log out User A.
   - Log in as User B.
   - Verify `last_synced_at` is null (or specific to User B if they had a previous session).
4. **Database Inspection**:
   - Use `adb shell` to inspect the database:
     ```powershell
     adb shell run-as fyi.teddy.android sqlite3 /data/data/fyi.teddy.android/databases/app_database "SELECT * FROM user_sync_metadata;"
     ```

### Automated Tests
- I will create a unit test `UserSyncMetadataTest.kt` to verify the DAO operations.
- I will add a test case to `SyncWorkerTest.kt` (if possible/exists) or verify via logs that the migration from SharedPreferences to Room occurs correctly.
