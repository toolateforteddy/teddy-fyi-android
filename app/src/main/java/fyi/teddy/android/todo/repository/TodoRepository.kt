package fyi.teddy.android.todo.repository

import android.content.Context
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.todo.util.TaskSchedulerUtils
import fyi.teddy.android.network.NetworkClient
import fyi.teddy.android.network.SyncWorker
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AssignIconResponse(
    @SerialName("emoji_or_asset_token") val icon: String,
)

/**
 * Repository layer for managing [TodoItem] data operations.
 * Acts as the clean API boundary between the domain view models and Room database DAOs,
 * facilitating future local-first synchronization engine integrations.
 */
class TodoRepository(
    private val todoDao: TodoDao,
    private val context: Context? = null
) {

    private fun scheduleSync() {
        context?.let { SyncWorker.enqueueDebounced(it) }
    }

    /**
     * Retrieves all [TodoItem]s for a specific user, sorted by position and creation date.
     */
    fun getAllItems(userId: String): Flow<List<TodoItem>> = todoDao.getAllItems(userId)

    /**
     * Retrieves all [TodoItem]s active or scheduled for today, utilizing modernized date formats.
     */
    fun getTodayItems(userId: String, today: String = TaskSchedulerUtils.getTodayDateString()): Flow<List<TodoItem>> {
        return todoDao.getTodayItems(userId, today)
    }

    /**
     * Retrieves all future scheduled [TodoItem]s starting after today's date.
     */
    fun getScheduledItems(userId: String, today: String = TaskSchedulerUtils.getTodayDateString()): Flow<List<TodoItem>> {
        return todoDao.getScheduledItems(userId, today)
    }

    /**
     * Inserts a new [TodoItem] at the next position sequence for the user.
     */
    suspend fun insertItem(item: TodoItem) {
        todoDao.insertWithNextPosition(item)
        scheduleSync()
    }

    /**
     * Updates an existing [TodoItem]'s details or completion state, ensuring the
     * local mutation lifecycle state machine is strictly followed.
     */
    suspend fun updateItem(item: TodoItem) {
        val nextSyncState = if (item.syncState == "SYNCED") "PENDING_UPDATE" else item.syncState
        todoDao.updateItem(item.copy(syncState = nextSyncState))
        
        // If this is a parent task being scheduled, apply the same scheduling to children
        if (item.parentId == null && item.scheduledDate != null && item.userId != null) {
            todoDao.scheduleIncompleteUnscheduledChildren(item.id, item.userId, item.scheduledDate)
        }

        scheduleSync()
    }

    /**
     * Deletes a [TodoItem], strictly adhering to the local mutation lifecycle rules.
     * If never synced (PENDING_INSERT), hard-deletes locally immediately.
     * If already synced (SYNCED or PENDING_UPDATE), flags as soft-deleted and PENDING_DELETE.
     */
    suspend fun deleteItem(item: TodoItem) {
        if (item.syncState == "PENDING_INSERT") {
            todoDao.deleteItem(item)
        } else {
            todoDao.updateItem(item.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }

    /**
     * Deletes all [TodoItem]s belonging to a specific user.
     */
    suspend fun deleteAll(userId: String) {
        todoDao.deleteAll(userId)
        scheduleSync()
    }

    /**
     * Resets any non-daily planned items for the user.
     */
    suspend fun resetPlannedItems(userId: String, today: String = TaskSchedulerUtils.getTodayDateString()) {
        todoDao.resetPlannedItems(userId, today)
        scheduleSync()
    }

    /**
     * Automatically claims unowned items for the currently logged-in user.
     */
    suspend fun claimUnownedItems(userId: String) {
        todoDao.claimUnownedItems(userId)
        scheduleSync()
    }

    /**
     * Resets state of recurring daily items back to uncompleted for today's date.
     */
    suspend fun resetDailyItems(userId: String) {
        val today = TaskSchedulerUtils.getTodayDateString()
        todoDao.resetDailyItems(userId, today)
        scheduleSync()
    }

    /**
     * Swaps display positions of two todo items to facilitate drag/drop or ordering.
     */
    suspend fun swapPositions(item1: TodoItem, item2: TodoItem) {
        todoDao.swapPositions(item1, item2)
        scheduleSync()
    }

    /**
     * Calls the remote endpoint to intelligently suggest an icon for the given item
     * based on its title, and updates the local item if successful.
     */
    suspend fun assignIcon(item: TodoItem, idToken: String): String? {
        return try {
            val response = NetworkClient.client.post("https://api-rust.teddy.fyi/api/assign-icon") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("todo_title" to item.title))
            }
            android.util.Log.d("TodoRepository", "assignIcon response status: ${response.status}")
            if (response.status.isSuccess()) {
                val iconResponse = response.body<AssignIconResponse>()
                val iconName = iconResponse.icon
                android.util.Log.d("TodoRepository", "assignIcon iconName: $iconName")
                if (iconName.isNotBlank() && (iconName != "null")) {
                    updateItem(item.copy(icon = iconName))
                    iconName
                } else null
            } else {
                val errorBody = try { response.body<String>() } catch (_: Exception) { "could not read error body" }
                android.util.Log.e("TodoRepository", "assignIcon failed: $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "assignIcon exception", e)
            null
        }
    }

    /**
     * Retrieves all todo lists for a specific user.
     */
    fun getAllLists(userId: String): Flow<List<TodoList>> = todoDao.getAllLists(userId)

    /**
     * Inserts a new todo list.
     */
    suspend fun insertList(list: TodoList) {
        todoDao.insertList(list)
        scheduleSync()
    }

    /**
     * Updates an existing todo list, following local mutation lifecycle state machine rules.
     */
    suspend fun updateList(list: TodoList) {
        val nextSyncState = if (list.syncState == "SYNCED") "PENDING_UPDATE" else list.syncState
        todoDao.updateList(list.copy(syncState = nextSyncState))
        scheduleSync()
    }

    /**
     * Deletes a todo list, following local mutation lifecycle state machine rules.
     * If never synced (PENDING_INSERT), hard-deletes and detaches lists.
     * If already synced (SYNCED or PENDING_UPDATE), soft-deletes and PENDING_DELETE.
     */
    suspend fun deleteList(list: TodoList) {
        if (list.syncState == "PENDING_INSERT") {
            todoDao.deleteListAndNullifyItems(list)
        } else {
            todoDao.nullifyListIdForItems(list.id)
            todoDao.updateList(list.copy(syncState = "PENDING_DELETE", isDeleted = true))
        }
        scheduleSync()
    }
}
