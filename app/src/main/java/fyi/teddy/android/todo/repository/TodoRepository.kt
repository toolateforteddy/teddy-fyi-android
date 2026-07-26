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
@Suppress("TooGenericExceptionCaught")
class TodoRepository(
    internal val todoDao: TodoDao,
    private val context: Context? = null
) {

    internal fun scheduleSync() {
        context?.let { SyncWorker.enqueue(it) }
    }

    /**
     * Retrieves all [TodoList]s for a specific user, sorted by position.
     */
    fun getAllLists(userId: String): Flow<List<TodoList>> = todoDao.getAllLists(userId)

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
        
        if (item.parentId == null && item.scheduledDate != null && item.userId != null) {
            todoDao.scheduleIncompleteUnscheduledChildren(item.id, item.userId, item.scheduledDate)
        }

        if (item.parentId == null && item.userId != null) {
            todoDao.updateChildrenListId(item.id, item.userId, item.listId)
        }

        scheduleSync()
    }

    /**
     * Deletes a [TodoItem], strictly adhering to the local mutation lifecycle rules.
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
     * Calls the remote endpoint to intelligently suggest an icon for the given item.
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "assignIcon exception", e)
            null
        }
    }
}
