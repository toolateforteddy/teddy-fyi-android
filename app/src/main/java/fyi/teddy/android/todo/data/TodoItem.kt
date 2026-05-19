package fyi.teddy.android.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val isPlannedForToday: Boolean = false,
    val recurrenceIntervalDays: Int? = null,
    val scheduledAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val parentId: Int? = null,
    val isDaily: Boolean = false,
    val dueDate: Long? = null
)
