package fyi.teddy.android.data

import androidx.room.*

/**
 * Stores local-only metadata for synchronization on a per-user basis.
 * This table is NOT synced to the server.
 */
@Entity(tableName = "user_sync_metadata")
data class UserSyncMetadata(
    @PrimaryKey
    val userId: String,
    val lastSyncedAt: String?
)

@Dao
interface UserSyncMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: UserSyncMetadata)

    @Query("SELECT lastSyncedAt FROM user_sync_metadata WHERE userId = :userId")
    suspend fun getLastSyncedAt(userId: String): String?

    @Query("DELETE FROM user_sync_metadata WHERE userId = :userId")
    suspend fun clear(userId: String)

    @Query("DELETE FROM user_sync_metadata")
    suspend fun clearAll()
}
