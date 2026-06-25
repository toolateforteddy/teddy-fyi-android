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
                list.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(TodoListDto.serializer(), list.toDto())
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
                    todoDao.upsertItem(localItem.copy(syncState = "SYNCED"))
                }
            }
        }

        // Transition successfully uploaded lists back to sync_state = SYNCED
        unsyncedLists.forEach { localList ->
            if (localList.isDeleted) {
                if (successIds.contains(localList.id)) {
                    todoDao.hardDeleteList(localList.id)
                }
            } else {
                if (successIds.contains(localList.id)) {
                    todoDao.upsertList(localList.copy(syncState = "SYNCED"))
                }
            }
        }

        // Upsert incoming remote_todo_changes into local Room DB
        remoteChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                todoDao.hardDeleteItem(changeDelta.id)
            } else {
                val itemDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(TodoItemDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (itemDto != null) {
                    todoDao.upsertItem(itemDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // Upsert incoming remote_todo_list_changes into local Room DB
        remoteListChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                todoDao.hardDeleteList(changeDelta.id)
            } else {
                val listDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(TodoListDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (listDto != null) {
                    todoDao.upsertList(listDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }
    }
}
