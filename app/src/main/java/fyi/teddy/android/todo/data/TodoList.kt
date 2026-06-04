package fyi.teddy.android.todo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#000000",
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "SYNCED",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
)
