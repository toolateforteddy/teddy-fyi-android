package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoDao
import android.util.Log
import kotlinx.serialization.json.JsonElement

object TodoSyncManager {

    private const val TAG = "TodoSyncManager"
    
    suspend fun collectLocalChanges(db: AppDatabase, isFirstSync: Boolean): List<TodoChangeDelta> {
        val todoDao = db.todoDao()
        val unsyncedItems = if (isFirstSync) {
            todoDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedItems()
        }

        return unsyncedItems.map { item ->
            val operationType = when {
                item.isDeleted -> OperationType.DELETE
                item.syncState == "PENDING_INSERT" -> OperationType.INSERT
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE || item.syncState == "NEED_UPDATE") null else {
                NetworkClient.syncJson.encodeToJsonElement(TodoItemDto.serializer(), item.toDto())
            }
            TodoChangeDelta(
                id = item.id,
                operationType = operationType,
                version = item.version,
                data = data
            )
        }
    }

    suspend fun collectLocalListChanges(db: AppDatabase, isFirstSync: Boolean): List<TodoListChangeDelta> {
        val todoDao = db.todoDao()
        val unsyncedLists = if (isFirstSync) {
            todoDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedLists()
        }

        return unsyncedLists.map { list ->
            val operationType = when {
                list.isDeleted -> OperationType.DELETE
                list.syncState == "PENDING_INSERT" -> OperationType.INSERT
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE || list.syncState == "NEED_UPDATE") null else {
                NetworkClient.syncJson.encodeToJsonElement(TodoListDto.serializer(), list.toDto())
            }
            TodoListChangeDelta(
                id = list.id,
                operationType = operationType,
                version = list.version,
                data = data
            )
        }
    }

    suspend fun handleSyncSuccess(
        db: AppDatabase,
        successIds: List<String>,
        remoteChanges: List<TodoChangeDelta>,
        remoteListChanges: List<TodoListChangeDelta>,
        isFirstSync: Boolean
    ) {
        val todoDao = db.todoDao()
        Log.d(TAG, "handleSyncSuccess: processing ${remoteListChanges.size} remote lists and ${remoteChanges.size} remote items")

        processUploadedItems(todoDao, successIds, isFirstSync)
        processUploadedLists(todoDao, successIds, isFirstSync)
        processRemoteLists(todoDao, remoteListChanges)
        processRemoteItems(todoDao, remoteChanges)
    }

    private suspend fun processUploadedItems(todoDao: TodoDao, successIds: List<String>, isFirstSync: Boolean) {
        val unsyncedItems = if (isFirstSync) {
            todoDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedItems()
        }
        var itemsUploaded = 0
        unsyncedItems.forEach { localItem ->
            if (successIds.contains(localItem.id)) {
                if (localItem.isDeleted) {
                    todoDao.hardDeleteItem(localItem.id)
                } else {
                    todoDao.upsertItem(localItem.copy(syncState = "SYNCED"))
                }
                itemsUploaded++
            }
        }
        if (itemsUploaded > 0) Log.d(TAG, "Marked $itemsUploaded local items as SYNCED")
    }

    private suspend fun processUploadedLists(todoDao: TodoDao, successIds: List<String>, isFirstSync: Boolean) {
        val unsyncedLists = if (isFirstSync) {
            todoDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedLists()
        }
        var listsUploaded = 0
        unsyncedLists.forEach { localList ->
            if (successIds.contains(localList.id)) {
                if (localList.isDeleted) {
                    todoDao.hardDeleteList(localList.id)
                } else {
                    todoDao.upsertList(localList.copy(syncState = "SYNCED"))
                }
                listsUploaded++
            }
        }
        if (listsUploaded > 0) Log.d(TAG, "Marked $listsUploaded local lists as SYNCED")
    }

    private suspend fun processRemoteLists(todoDao: TodoDao, remoteListChanges: List<TodoListChangeDelta>) {
        var remoteListsUpserted = 0
        var remoteListsDeleted = 0
        remoteListChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                todoDao.hardDeleteList(changeDelta.id)
                remoteListsDeleted++
            } else {
                decodeDto(changeDelta.data, TodoListDto.serializer())?.let { dto ->
                    todoDao.upsertList(dto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                    remoteListsUpserted++
                }
            }
        }
        if (remoteListsUpserted > 0 || remoteListsDeleted > 0) {
            Log.d(TAG, "Applied remote list changes: upserted=$remoteListsUpserted, deleted=$remoteListsDeleted")
        }
    }

    private suspend fun processRemoteItems(todoDao: TodoDao, remoteChanges: List<TodoChangeDelta>) {
        var remoteItemsUpserted = 0
        var remoteItemsDeleted = 0
        remoteChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                todoDao.hardDeleteItem(changeDelta.id)
                remoteItemsDeleted++
                return@forEach
            }
            val dto = decodeDto(changeDelta.data, TodoItemDto.serializer()) ?: return@forEach
            if (!listExists(todoDao, dto.listId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring TodoItem ${changeDelta.id} because Parent List ${dto.listId} is missing. Item: $dto")
                return@forEach
            }
            todoDao.upsertItem(dto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
            remoteItemsUpserted++
        }
        Log.d(TAG, "Applied remote item changes: upserted=$remoteItemsUpserted, deleted=$remoteItemsDeleted (out of ${remoteChanges.size})")
    }

    private fun <T> decodeDto(data: JsonElement?, serializer: kotlinx.serialization.KSerializer<T>): T? {
        return data?.let {
            try {
                NetworkClient.syncJson.decodeFromJsonElement(serializer, it)
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e(TAG, "Failed to decode DTO: ${e.message}", e)
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to decode DTO: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun listExists(dao: TodoDao, listId: String?): Boolean {
        if (listId == null) return true
        return dao.getListByIdOneShot(listId) != null
    }
}
