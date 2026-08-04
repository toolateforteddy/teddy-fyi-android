package fyi.teddy.android.grocery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "grocery_lists")
data class GroceryList(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val position: Int = 0,
    val ownerId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING_INSERT",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
