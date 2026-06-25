@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class, kotlinx.serialization.InternalSerializationApi::class)
package fyi.teddy.android.network

import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
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
data class TodoListChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class TodoChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class GroceryListChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class GroceryListMemberChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class StoreChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class CategoryChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class GroceryChangeDelta(
    val id: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class GroceryItemStoreInfoChangeDelta(
    val id: String,
    @SerialName("grocery_item_id") val groceryItemId: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("type")
    val operationType: OperationType,
    val version: Int,
    val data: JsonElement? = null,
)

@Serializable
data class SyncRequest(
    @SerialName("last_synced_at") val lastSyncedAt: String?,
    @SerialName("client_id") val clientId: String,
    @SerialName("todo_list_changes") val todoListChanges: List<TodoListChangeDelta> = emptyList(),
    @SerialName("todo_changes") val todoChanges: List<TodoChangeDelta> = emptyList(),
    @SerialName("grocery_list_changes") val groceryListChanges: List<GroceryListChangeDelta> = emptyList(),
    @SerialName("grocery_list_member_changes") val groceryListMemberChanges: List<GroceryListMemberChangeDelta> = emptyList(),
    @SerialName("store_changes") val storeChanges: List<StoreChangeDelta> = emptyList(),
    @SerialName("category_changes") val categoryChanges: List<CategoryChangeDelta> = emptyList(),
    @SerialName("grocery_changes") val groceryChanges: List<GroceryChangeDelta> = emptyList(),
    @SerialName("grocery_item_store_info_changes") val groceryItemStoreInfoChanges: List<GroceryItemStoreInfoChangeDelta> = emptyList(),
)

@Serializable
data class SyncResponse(
    @SerialName("success_ids") val successIds: List<String>,
    @SerialName("remote_todo_list_changes") val remoteTodoListChanges: List<TodoListChangeDelta> = emptyList(),
    @SerialName("remote_todo_changes") val remoteTodoChanges: List<TodoChangeDelta> = emptyList(),
    @SerialName("remote_grocery_list_changes") val remoteGroceryListChanges: List<GroceryListChangeDelta> = emptyList(),
    @SerialName("remote_grocery_list_member_changes") val remoteGroceryListMemberChanges: List<GroceryListMemberChangeDelta> = emptyList(),
    @SerialName("remote_store_changes") val remoteStoreChanges: List<StoreChangeDelta> = emptyList(),
    @SerialName("remote_category_changes") val remoteCategoryChanges: List<CategoryChangeDelta> = emptyList(),
    @SerialName("remote_grocery_changes") val remoteGroceryChanges: List<GroceryChangeDelta> = emptyList(),
    @SerialName("remote_grocery_item_store_info_changes") val remoteGroceryItemStoreInfoChanges: List<GroceryItemStoreInfoChangeDelta> = emptyList(),
    @SerialName("server_timestamp") val serverTimestamp: String
)

@Serializable
data class TodoItemDto(
    val id: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    val position: Int,
    @SerialName("scheduled_date") val scheduledDate: String?,
    @SerialName("recurrence_rule") val recurrenceRule: String?,
    @SerialName("scheduled_at") val scheduledAt: Long,
    @SerialName("user_id") val userId: String?,
    @SerialName("parent_id") val parentId: String?,
    @SerialName("is_daily") val isDaily: Boolean,
    @SerialName("due_date") val dueDate: Long?,
    val description: String?,
    @SerialName("list_id") val listId: String?,
    val priority: Int,
    val icon: String?,
    @SerialName("last_scheduled_date") val lastScheduledDate: String?,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class TodoListDto(
    val id: String,
    val name: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("user_id") val userId: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class GroceryItemDto(
    val id: String,
    val name: String,
    val quantity: String,
    @SerialName("is_bought") val isBought: Boolean,
    @SerialName("created_at") val createdAt: Long,
    val position: Int,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("times_bought") val timesBought: Int,
    @SerialName("user_id") val userId: String?,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("list_id") val listId: String?,
    val unit: String?,
    val notes: String?,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class GroceryListDto(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val position: Int,
    @SerialName("is_default_supported") val isDefaultSupported: Boolean,
    @SerialName("user_id") val userId: String?,
    @SerialName("list_id") val listId: String?,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val position: Int,
    @SerialName("user_id") val userId: String?,
    @SerialName("list_id") val listId: String?,
    val icon: String?,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class GroceryListMemberDto(
    val id: String,
    @SerialName("list_id") val listId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: Long,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class GroceryItemStoreInfoDto(
    @SerialName("grocery_item_id") val groceryItemId: String,
    @SerialName("store_id") val storeId: String,
    val price: Double?,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("user_id") val userId: String?,
    @SerialName("sync_state") val syncState: String = "SYNCED",
    val version: Int = 1,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    val id: String?, // Ignored
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
        icon = icon,
        lastScheduledDate = lastScheduledDate,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
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
        icon = icon,
        lastScheduledDate = lastScheduledDate,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun TodoList.toDto(): TodoListDto {
    return TodoListDto(
        id = id,
        name = name,
        colorHex = colorHex,
        userId = userId,
        createdAt = createdAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun TodoListDto.toEntity(): TodoList {
    return TodoList(
        id = id,
        name = name,
        colorHex = colorHex,
        userId = userId,
        createdAt = createdAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
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
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
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
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryList.toDto(): GroceryListDto {
    return GroceryListDto(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListDto.toEntity(): GroceryList {
    return GroceryList(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun Store.toDto(): StoreDto {
    return StoreDto(
        id = id,
        name = name,
        position = position,
        isDefaultSupported = isDefaultSupported,
        userId = userId,
        listId = listId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun StoreDto.toEntity(): Store {
    return Store(
        id = id,
        name = name,
        position = position,
        isDefaultSupported = isDefaultSupported,
        userId = userId,
        listId = listId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun Category.toDto(): CategoryDto {
    return CategoryDto(
        id = id,
        name = name,
        position = position,
        userId = userId,
        listId = listId,
        icon = icon,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun CategoryDto.toEntity(): Category {
    return Category(
        id = id,
        name = name,
        position = position,
        userId = userId,
        listId = listId,
        icon = icon,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListMember.toDto(): GroceryListMemberDto {
    return GroceryListMemberDto(
        id = id,
        listId = listId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryListMemberDto.toEntity(): GroceryListMember {
    return GroceryListMember(
        id = id,
        listId = listId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}

fun GroceryItemStoreInfo.toDto(): GroceryItemStoreInfoDto {
    return GroceryItemStoreInfoDto(
        groceryItemId = groceryItemId,
        storeId = storeId,
        price = price,
        isAvailable = isAvailable,
        userId = userId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted,
        id=null,
    )
}

fun GroceryItemStoreInfoDto.toEntity(): GroceryItemStoreInfo {
    return GroceryItemStoreInfo(
        groceryItemId = groceryItemId,
        storeId = storeId,
        price = price,
        isAvailable = isAvailable,
        userId = userId,
        syncState = syncState,
        version = version,
        isDeleted = isDeleted
    )
}
