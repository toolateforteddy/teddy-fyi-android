package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import kotlinx.serialization.json.Json

object GrocerySyncManager {

    suspend fun collectLocalChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedGroceryItems = if (isFirstSync) {
            groceryDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedItems()
        }

        return unsyncedGroceryItems.map { item ->
            val operationType = when {
                item.isDeleted -> OperationType.DELETE
                item.syncState == "PENDING_INSERT" -> OperationType.INSERT
                item.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(GroceryItemDto.serializer(), item.toDto())
            }
            GroceryChangeDelta(
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
        remoteChanges: List<GroceryChangeDelta>,
        isFirstSync: Boolean
    ) {
        val groceryDao = db.groceryDao()
        val unsyncedGroceryItems = if (isFirstSync) {
            groceryDao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedItems()
        }
        val unsyncedGroceryLists = if (isFirstSync) {
            groceryDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedLists()
        }

        // Transition successfully uploaded grocery items back to sync_state = SYNCED
        unsyncedGroceryItems.forEach { localGroceryItem ->
            val stringId = localGroceryItem.id.toString()
            if (localGroceryItem.isDeleted) {
                if (successIds.contains(stringId)) {
                    groceryDao.hardDeleteItem(localGroceryItem.id)
                }
            } else {
                if (successIds.contains(stringId)) {
                    groceryDao.insertItem(localGroceryItem.copy(syncState = "SYNCED"))
                }
            }
        }

        // For grocery lists, mark them as SYNCED locally on successful sync response
        unsyncedGroceryLists.forEach { localGroceryList ->
            if (localGroceryList.isDeleted) {
                groceryDao.hardDeleteList(localGroceryList.id)
            } else {
                groceryDao.insertList(localGroceryList.copy(syncState = "SYNCED"))
            }
        }

        // Upsert incoming remote_grocery_changes into local Room DB
        remoteChanges.forEach { changeDelta ->
            if (changeDelta.operation_type == OperationType.DELETE) {
                groceryDao.hardDeleteItem(changeDelta.id)
            } else {
                val groceryDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(GroceryItemDto.serializer(), it)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (groceryDto != null) {
                    groceryDao.insertItem(groceryDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }
    }
}
