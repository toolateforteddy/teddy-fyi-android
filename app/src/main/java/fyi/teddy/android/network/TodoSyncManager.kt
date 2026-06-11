package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import kotlinx.serialization.json.Json

object TodoSyncManager {
    
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
                item.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(TodoItemDto.serializer(), item.toDto())
            }
            TodoChangeDelta(
                id = item.id,
                operation_type = operationType,
                version = item.version,
                data = data
            )
        }
    }

    suspend fun handleSyncSuccess(
        db: AppDatabase,
        successIds: List<String>,
        remoteChanges: List<TodoChangeDelta>,
        isFirstSync: Boolean
    ) {
        val todoDao = db.todoDao()
        val unsyncedItems = if (isFirstSync) {
            todoDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedItems()
        }
        val unsyncedLists = if (isFirstSync) {
            todoDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            todoDao.getUnsyncedLists()
        }

        // Transition successfully uploaded items back to sync_state = SYNCED
        unsyncedItems.forEach { localItem ->
            if (localItem.isDeleted) {
                if (successIds.contains(localItem.id)) {
                    todoDao.hardDeleteItem(localItem.id)
                }
            } else {
                if (successIds.contains(localItem.id)) {
                    todoDao.insertItem(localItem.copy(syncState = "SYNCED"))
                }
            }
        }

        // For lists, since the server doesn't sync lists, we just mark them as SYNCED locally on successful sync response
        unsyncedLists.forEach { localList ->
            if (localList.isDeleted) {
                todoDao.hardDeleteList(localList.id)
            } else {
                todoDao.insertList(localList.copy(syncState = "SYNCED"))
            }
        }

        // Upsert incoming remote_todo_changes into local Room DB
        remoteChanges.forEach { changeDelta ->
            if (changeDelta.operation_type == OperationType.DELETE) {
                todoDao.hardDeleteItem(changeDelta.id)
            } else {
                val itemDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(TodoItemDto.serializer(), it)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (itemDto != null) {
                    todoDao.insertItem(itemDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }
    }
}
