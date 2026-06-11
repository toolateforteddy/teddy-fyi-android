package fyi.teddy.android.network

import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class OperationType {
    @SerialName("INSERT") INSERT,
    @SerialName("UPDATE") UPDATE,
    @SerialName("DELETE") DELETE
}

@Serializable
data class TodoChangeDelta(
    val id: String,
    @SerialName("type")
    val operation_type: OperationType,
    val version: Int,
    val data: JsonElement? = null
)

@Serializable
data class GroceryChangeDelta(
    val id: Int,
    @SerialName("type")
    val operation_type: OperationType,
    val version: Int,
    val data: JsonElement? = null
)

@Serializable
data class SyncRequest(
    val last_synced_at: String?,
    val client_id: String,
    val todo_changes: List<TodoChangeDelta>,
    val grocery_changes: List<GroceryChangeDelta>
)

@Serializable
data class SyncResponse(
    val success_ids: List<String>,
    val remote_todo_changes: List<TodoChangeDelta>,
    val remote_grocery_changes: List<GroceryChangeDelta>,
    val server_timestamp: String
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
    val id: Int,
    val name: String,
    val quantity: String,
    @SerialName("isBought") val isBought: Boolean,
    @SerialName("createdAt") val createdAt: Long,
    val position: Int,
    @SerialName("categoryId") val categoryId: Int?,
    @SerialName("timesBought") val timesBought: Int,
    @SerialName("userId") val userId: String?,
    @SerialName("isActive") val isActive: Boolean,
    @SerialName("listId") val listId: String?,
    val unit: String?,
    val notes: String?,
    val sync_state: String,
    val version: Int,
    val is_deleted: Boolean
)

@Serializable
data class GroceryListDto(
    val id: String,
    val name: String,
    @SerialName("ownerId") val ownerId: String?,
    @SerialName("createdAt") val createdAt: Long,
    val sync_state: String,
    val version: Int,
    val is_deleted: Boolean
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

fun GroceryItem.toDto(): GroceryItemDto {
    return GroceryItemDto(
        id = id,
        name = name,
        quantity = quantity,
        isBought = isBought,
        createdAt = createdAt,
        position = position,
        categoryId = categoryId,
        timesBought = timesBought,
        userId = userId,
        isActive = isActive,
        listId = listId,
        unit = unit,
        notes = notes,
        sync_state = syncState,
        version = version,
        is_deleted = isDeleted
    )
}

fun GroceryItemDto.toEntity(): GroceryItem {
    return GroceryItem(
        id = id,
        name = name,
        quantity = quantity,
        isBought = isBought,
        createdAt = createdAt,
        position = position,
        categoryId = categoryId,
        timesBought = timesBought,
        userId = userId,
        isActive = isActive,
        listId = listId,
        unit = unit,
        notes = notes,
        syncState = sync_state,
        version = version,
        isDeleted = is_deleted
    )
}

fun GroceryList.toDto(): GroceryListDto {
    return GroceryListDto(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        sync_state = syncState,
        version = version,
        is_deleted = isDeleted
    )
}

fun GroceryListDto.toEntity(): GroceryList {
    return GroceryList(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        syncState = sync_state,
        version = version,
        isDeleted = is_deleted
    )
}
