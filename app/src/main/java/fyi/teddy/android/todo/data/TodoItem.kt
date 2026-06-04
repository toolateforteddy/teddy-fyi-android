package fyi.teddy.android.todo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [
        Index(value = ["listId"])
    ]
)
data class TodoItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val scheduledDate: String? = null,
    val recurrenceIntervalDays: Int? = null,
    val scheduledAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val parentId: String? = null,
    val isDaily: Boolean = false,
    val dueDate: Long? = null,
    val description: String? = null,
    val listId: String? = null,
    val priority: Int = 0,

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "SYNCED",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
)
