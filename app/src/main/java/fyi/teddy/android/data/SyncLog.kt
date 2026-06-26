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
    
    val todoListsSent: Int = 0,
    val todoItemsSent: Int = 0,
    val groceryListsSent: Int = 0,
    val groceryMembersSent: Int = 0,
    val storesSent: Int = 0,
    val categoriesSent: Int = 0,
    val groceryItemsSent: Int = 0,
    val storeInfosSent: Int = 0,

    val todoListsReceived: Int = 0,
    val todoItemsReceived: Int = 0,
    val groceryListsReceived: Int = 0,
    val groceryMembersReceived: Int = 0,
    val storesReceived: Int = 0,
    val categoriesReceived: Int = 0,
    val groceryItemsReceived: Int = 0,
    val storeInfosReceived: Int = 0
)

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLog): Long

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<SyncLog>>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<SyncLog?>

    @Query("DELETE FROM sync_logs WHERE timestamp < :cutoff")
    suspend fun pruneLogs(cutoff: Long)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAll()
}
