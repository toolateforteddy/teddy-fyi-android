# Walkthrough - Fix Room KSP "unexpected jvm signature V"

Resolved the KSP compilation error `java.lang.IllegalStateException: unexpected jvm signature V` which occurred during the build process.

## Changes Made

### Room DAOs Optimization
Modified all `@Dao` classes to avoid signatures that trigger issues in the Room KSP processor when used with Kotlin 2.x:

1.  **Removed Default Parameters**: Room DAOs do not support default parameters in Kotlin.
    - Updated [SyncLogDao](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/data/SyncLog.kt) to remove the default limit in `getRecentLogs`.
2.  **Eliminated `Unit` Return Types**: Changed `suspend` functions returning `Unit` (which maps to `void`/`V` in bytecode) to return `Int` or `Long`.
    - Updated [GroceryDao](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/grocery/data/GroceryDao.kt), [TodoDao](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/todo/data/TodoDao.kt), [SyncLogDao](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/data/SyncLog.kt), and [UserSyncMetadataDao](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/data/UserSyncMetadata.kt).
3.  **Removed Nullable Primitives**: Changed `Long?` return types to `Long` with `COALESCE` in queries to avoid JVM signature ambiguity.
    - Updated `SyncLogDao#getLastSuccessTimestamp`.

### Dependency Updates
- Upgraded **Room** from `2.6.1` to `2.8.4` to ensure compatibility with Kotlin 2.2 and the latest KSP features.
- Synchronized **KSP** version to `2.2.10-2.0.2` to match the project's Kotlin version.

## Verification Results

### Automated Tests
- Successfully ran `./gradlew :app:kspDebugKotlin`. The build now completes without the `unexpected jvm signature V` error.
- Verified that existing code using these DAOs (like `SyncWorker` and `DebugScreen`) remains compatible with the signature changes.

---
**Build Status: SUCCESS**
