package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.Store
import android.util.Log
import kotlinx.serialization.json.JsonElement

object GrocerySyncManager {

    private const val TAG = "GrocerySyncManager"

    // --- COLLECTION OF LOCAL CHANGES ---

    suspend fun collectLocalChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedItems()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(GroceryItemDto.serializer(), item.toDto())
            )
        }
    }

    suspend fun collectLocalListChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryListChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedLists()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryListChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(GroceryListDto.serializer(), item.toDto())
            )
        }
    }

    suspend fun collectLocalListMemberChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryListMemberChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllListMembersOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedListMembers()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryListMemberChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(GroceryListMemberDto.serializer(), item.toDto())
            )
        }
    }

    suspend fun collectLocalStoreChanges(db: AppDatabase, isFirstSync: Boolean): List<StoreChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllStoresOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedStores()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            StoreChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(StoreDto.serializer(), item.toDto())
            )
        }
    }

    suspend fun collectLocalCategoryChanges(db: AppDatabase, isFirstSync: Boolean): List<CategoryChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllCategoriesOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedCategories()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            CategoryChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(CategoryDto.serializer(), item.toDto())
            )
        }
    }

    suspend fun collectLocalStoreInfoChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryItemStoreInfoChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllStoreInfosOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedStoreInfos()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryItemStoreInfoChangeDelta(
                id = "${item.groceryItemId}_${item.storeId}",
                groceryItemId = item.groceryItemId,
                storeId = item.storeId,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE) null else NetworkClient.syncJson.encodeToJsonElement(GroceryItemStoreInfoDto.serializer(), item.toDto())
            )
        }
    }

    // --- SYNC SUCCESS HANDLER ---

    suspend fun handleSyncSuccess(
        db: AppDatabase,
        successIds: List<String>,
        remoteChanges: List<GroceryChangeDelta>,
        remoteStoreChanges: List<StoreChangeDelta>,
        remoteCategoryChanges: List<CategoryChangeDelta>,
        remoteListChanges: List<GroceryListChangeDelta>,
        remoteListMemberChanges: List<GroceryListMemberChangeDelta>,
        remoteStoreInfoChanges: List<GroceryItemStoreInfoChangeDelta>,
        isFirstSync: Boolean
    ) {
        val dao = db.groceryDao()

        // 1. Process local changes that were successfully uploaded
        processSuccessfulUploads(dao, successIds, isFirstSync)

        // 2. Apply changes received from the server
        applyRemoteChanges(
            dao,
            remoteChanges,
            remoteStoreChanges,
            remoteCategoryChanges,
            remoteListChanges,
            remoteListMemberChanges,
            remoteStoreInfoChanges
        )
    }

    private suspend fun processSuccessfulUploads(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        // Order matches dependency hierarchy where possible
        processSuccessfulLists(dao, successIds, isFirstSync)
        processSuccessfulListMembers(dao, successIds, isFirstSync)
        processSuccessfulStores(dao, successIds, isFirstSync)
        processSuccessfulCategories(dao, successIds, isFirstSync)
        processSuccessfulItems(dao, successIds, isFirstSync)
        processSuccessfulStoreInfos(dao, successIds, isFirstSync)
    }

    private suspend fun processSuccessfulLists(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllListsOneShot() else dao.getUnsyncedLists()
        items.forEach { local ->
            if (successIds.contains(local.id)) {
                if (local.isDeleted) dao.hardDeleteList(local.id)
                else dao.upsertList(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun processSuccessfulListMembers(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllListMembersOneShot() else dao.getUnsyncedListMembers()
        items.forEach { local ->
            if (successIds.contains(local.id)) {
                if (local.isDeleted) dao.hardDeleteListMember(local.id)
                else dao.upsertListMember(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun processSuccessfulStores(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllStoresOneShot() else dao.getUnsyncedStores()
        items.forEach { local ->
            if (successIds.contains(local.id)) {
                if (local.isDeleted) dao.hardDeleteStore(local.id)
                else dao.upsertStore(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun processSuccessfulCategories(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllCategoriesOneShot() else dao.getUnsyncedCategories()
        items.forEach { local ->
            if (successIds.contains(local.id)) {
                if (local.isDeleted) dao.hardDeleteCategory(local.id)
                else dao.upsertCategory(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun processSuccessfulItems(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllItemsOneShot() else dao.getUnsyncedItems()
        items.forEach { local ->
            if (successIds.contains(local.id)) {
                if (local.isDeleted) dao.hardDeleteItem(local.id)
                else dao.upsertItem(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun processSuccessfulStoreInfos(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
        val items = if (isFirstSync) dao.getAllStoreInfosOneShot() else dao.getUnsyncedStoreInfos()
        items.forEach { local ->
            val compositeId = "${local.groceryItemId}_${local.storeId}"
            if (successIds.contains(compositeId)) {
                if (local.isDeleted) dao.hardDeleteStoreInfo(local.groceryItemId, local.storeId)
                else dao.upsertStoreInfo(local.copy(syncState = "SYNCED"))
            }
        }
    }

    private suspend fun applyRemoteChanges(
        dao: GroceryDao,
        remoteItems: List<GroceryChangeDelta>,
        remoteStores: List<StoreChangeDelta>,
        remoteCategories: List<CategoryChangeDelta>,
        remoteLists: List<GroceryListChangeDelta>,
        remoteMembers: List<GroceryListMemberChangeDelta>,
        remoteStoreInfos: List<GroceryItemStoreInfoChangeDelta>
    ) {
        // Order matches dependency hierarchy: Lists -> (Members, Stores, Categories) -> Items -> StoreInfos
        
        remoteLists.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteList(change.id)
            } else {
                decodeDto(change.data, GroceryListDto.serializer())?.let { dto ->
                    dao.upsertList(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }

        remoteMembers.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteListMember(change.id)
            } else {
                decodeDto(change.data, GroceryListMemberDto.serializer())?.let { dto ->
                    ensureListExists(dao, dto.listId)
                    dao.upsertListMember(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }

        remoteStores.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteStore(change.id)
            } else {
                decodeDto(change.data, StoreDto.serializer())?.let { dto ->
                    ensureListExists(dao, dto.listId)
                    dao.upsertStore(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }

        remoteCategories.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteCategory(change.id)
            } else {
                decodeDto(change.data, CategoryDto.serializer())?.let { dto ->
                    ensureListExists(dao, dto.listId)
                    dao.upsertCategory(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }

        remoteItems.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteItem(change.id)
            } else {
                decodeDto(change.data, GroceryItemDto.serializer())?.let { dto ->
                    ensureListExists(dao, dto.listId)
                    dao.upsertItem(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }

        remoteStoreInfos.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteStoreInfo(change.groceryItemId, change.storeId)
            } else {
                decodeDto(change.data, GroceryItemStoreInfoDto.serializer())?.let { dto ->
                    ensureItemExists(dao, change.groceryItemId)
                    ensureStoreExists(dao, change.storeId)
                    dao.upsertStoreInfo(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
                }
            }
        }
    }

    // --- HELPERS ---

    private fun determineOpType(isDeleted: Boolean, syncState: String): OperationType {
        return when {
            isDeleted -> OperationType.DELETE
            syncState == "PENDING_INSERT" -> OperationType.INSERT
            else -> OperationType.UPDATE
        }
    }

    private fun <T> decodeDto(data: JsonElement?, serializer: kotlinx.serialization.KSerializer<T>): T? {
        return data?.let {
            try {
                NetworkClient.syncJson.decodeFromJsonElement(serializer, it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode DTO: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun ensureListExists(dao: GroceryDao, listId: String?) {
        if (listId == null) return
        val existing = dao.getListByIdOneShot(listId)
        if (existing == null) {
            Log.d(TAG, "ensureListExists: Creating placeholder list for $listId - why was it missing from remote changes?")
            dao.upsertList(GroceryList(id = listId, name = "Syncing List...", syncState = "SYNCED", version = 0))
        }
    }

    private suspend fun ensureItemExists(dao: GroceryDao, itemId: String) {
        val existing = dao.getItemByIdOneShot(itemId)
        if (existing == null) {
            Log.d(TAG, "ensureItemExists: Creating placeholder item for $itemId - why was it missing from remote changes?")
            dao.upsertItem(GroceryItem(id = itemId, name = "Syncing Item...", syncState = "SYNCED", version = 0))
        }
    }

    private suspend fun ensureStoreExists(dao: GroceryDao, storeId: String) {
        val existing = dao.getStoreByIdOneShot(storeId)
        if (existing == null) {
            Log.d(TAG, "ensureStoreExists: Creating placeholder store for $storeId - why was it missing from remote changes?")
            dao.upsertStore(Store(id = storeId, name = "Syncing Store...", syncState = "SYNCED", version = 0))
        }
    }
}
