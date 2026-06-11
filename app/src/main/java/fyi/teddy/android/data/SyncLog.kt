package fyi.teddy.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "FAILURE", "RETRY"
    val durationMillis: Long,
    val errorMessage: String? = null,
    val todoChangesSent: Int = 0,
    val groceryChangesSent: Int = 0,
    val todoChangesReceived: Int = 0,
    val groceryChangesReceived: Int = 0
)

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLog): Long

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<SyncLog>>

    @Query("DELETE FROM sync_logs WHERE timestamp < :cutoff")
    suspend fun pruneLogs(cutoff: Long)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAll()
}
