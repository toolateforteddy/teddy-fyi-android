package fyi.teddy.android.network

import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val last_synced_at: String?,
    val todo_changes: TodoChangesDto,
    val grocery_changes: GroceryChangesDto
)

@Serializable
data class TodoChangesDto(
    val items: List<TodoItemDto>,
    val lists: List<TodoListDto>
)

@Serializable
data class GroceryChangesDto(
    val items: List<GroceryItemDto> = emptyList(),
    val lists: List<GroceryListDto> = emptyList()
)

@Serializable
data class SyncResponse(
    val server_time: String,
    val remote_changes: RemoteChangesDto
)

@Serializable
data class RemoteChangesDto(
    val todo_changes: TodoChangesDto,
    val grocery_changes: GroceryChangesDto
)

@Serializable
data class TodoItemDto(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val position: Int,
    val scheduledDate: String?,
    val recurrenceRule: String?,
    val scheduledAt: Long,
    val userId: String?,
    val parentId: String?,
    val isDaily: Boolean,
    val dueDate: Long?,
    val description: String?,
    val listId: String?,
    val priority: Int,
    val sync_state: String,
    val version: Int,
    val is_deleted: Boolean
)

@Serializable
data class TodoListDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val userId: String?,
    val createdAt: Long,
    val sync_state: String,
    val version: Int,
    val is_deleted: Boolean
)

@Serializable
data class GroceryItemDto(
    val id: String,
    val name: String,
    val quantity: String,
    val isBought: Boolean,
    val createdAt: Long,
    val position: Int,
    val categoryId: Int?,
    val timesBought: Int,
    val userId: String?,
    val isActive: Boolean,
    val listId: String?,
    val unit: String?,
    val notes: String?,
    val sync_state: String = "SYNCED",
    val version: Int = 1,
    val is_deleted: Boolean = false
)

@Serializable
data class GroceryListDto(
    val id: String,
    val name: String,
    val ownerId: String?,
    val createdAt: Long,
    val sync_state: String = "SYNCED",
    val version: Int = 1,
    val is_deleted: Boolean = false
)

// Helper mapping extensions

fun TodoItem.toDto(): TodoItemDto {
    return TodoItemDto(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAt = createdAt,
        position = position,
        scheduledDate = scheduledDate,
        recurrenceRule = recurrenceRule,
        scheduledAt = scheduledAt,
        userId = userId,
        parentId = parentId,
        isDaily = isDaily,
        dueDate = dueDate,
        description = description,
        listId = listId,
        priority = priority,
        sync_state = syncState,
        version = version,
        is_deleted = isDeleted
    )
}

fun TodoItemDto.toEntity(): TodoItem {
    return TodoItem(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAt = createdAt,
        position = position,
        scheduledDate = scheduledDate,
        recurrenceRule = recurrenceRule,
        scheduledAt = scheduledAt,
        userId = userId,
        parentId = parentId,
        isDaily = isDaily,
        dueDate = dueDate,
        description = description,
        listId = listId,
        priority = priority,
        syncState = sync_state,
        version = version,
        isDeleted = is_deleted
    )
}

fun TodoList.toDto(): TodoListDto {
    return TodoListDto(
        id = id,
        name = name,
        colorHex = colorHex,
        userId = userId,
        createdAt = createdAt,
        sync_state = syncState,
        version = version,
        is_deleted = isDeleted
    )
}

fun TodoListDto.toEntity(): TodoList {
    return TodoList(
        id = id,
        name = name,
        colorHex = colorHex,
        userId = userId,
        createdAt = createdAt,
        syncState = sync_state,
        version = version,
        isDeleted = is_deleted
    )
}
