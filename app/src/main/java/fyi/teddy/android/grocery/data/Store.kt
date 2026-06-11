package fyi.teddy.android.grocery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val position: Int = 0,
    val isDefaultSupported: Boolean = true,
    val userId: String? = null,

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING_INSERT",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
