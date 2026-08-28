package fyi.teddy.android.network

import android.util.Log
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.GroceryDao
import kotlinx.serialization.json.JsonElement

data class RemoteGroceryChanges(
    val items: List<GroceryChangeDelta> = emptyList(),
    val stores: List<StoreChangeDelta> = emptyList(),
    val categories: List<CategoryChangeDelta> = emptyList(),
    val lists: List<GroceryListChangeDelta> = emptyList(),
    val members: List<GroceryListMemberChangeDelta> = emptyList(),
    val storeInfos: List<GroceryItemStoreInfoChangeDelta> = emptyList()
)

@Suppress("TooManyFunctions")
object GrocerySyncManager {

    private const val TAG = "GrocerySyncManager"

    private fun determineOpType(isDeleted: Boolean, syncState: String): OperationType {
        return when {
            isDeleted -> OperationType.DELETE
            syncState == "PENDING_INSERT" -> OperationType.INSERT
            else -> OperationType.UPDATE
        }
    }

    suspend fun collectLocalChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryChangeDelta> {
        val dao = db.groceryDao()
        val items = if (isFirstSync) dao.getAllItemsOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedItems()
        return items.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryChangeDelta(
                id = item.id,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE || item.syncState == "NEED_UPDATE") null 
                       else NetworkClient.syncJson.encodeToJsonElement(GroceryItemDto.serializer(), item.toDto())
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
                data = if (op == OperationType.DELETE || item.syncState == "NEED_UPDATE") null 
                       else NetworkClient.syncJson.encodeToJsonElement(GroceryListDto.serializer(), item.toDto())
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
                data = if (op == OperationType.DELETE || item.syncState == "NEED_UPDATE") null 
                       else NetworkClient.syncJson.encodeToJsonElement(GroceryListMemberDto.serializer(), item.toDto())
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
                data = if (op == OperationType.DELETE || item.syncState == "NEED_UPDATE") null 
                       else NetworkClient.syncJson.encodeToJsonElement(StoreDto.serializer(), item.toDto())
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
        val storeInfos = if (isFirstSync) dao.getAllStoreInfosOneShot().map { it.copy(syncState = "PENDING_INSERT") } else dao.getUnsyncedStoreInfos()
        return storeInfos.map { item ->
            val op = determineOpType(item.isDeleted, item.syncState)
            GroceryItemStoreInfoChangeDelta(
                id = item.id,
                groceryItemId = item.groceryItemId,
                storeId = item.storeId,
                operationType = op,
                version = item.version,
                data = if (op == OperationType.DELETE || item.syncState == "NEED_UPDATE") null
                       else NetworkClient.syncJson.encodeToJsonElement(GroceryItemStoreInfoDto.serializer(), item.toDto())
            )
        }
    }

    @Suppress("LongParameterList")
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
        processSuccessfulUploads(dao, successIds, isFirstSync)
        applyRemoteChanges(
            dao,
            RemoteGroceryChanges(
                items = remoteChanges,
                stores = remoteStoreChanges,
                categories = remoteCategoryChanges,
                lists = remoteListChanges,
                members = remoteListMemberChanges,
                storeInfos = remoteStoreInfoChanges
            )
        )
    }

    private suspend fun applyRemoteChanges(dao: GroceryDao, remote: RemoteGroceryChanges) {
        applyRemoteLists(dao, remote.lists)
        applyRemoteMembers(dao, remote.members)
        applyRemoteStores(dao, remote.stores)
        applyRemoteCategories(dao, remote.categories)
        applyRemoteItems(dao, remote.items)
        applyRemoteStoreInfos(dao, remote.storeInfos)
    }

    private suspend fun applyRemoteLists(dao: GroceryDao, remoteLists: List<GroceryListChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteLists.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteList(change.id)
                deleted++
            } else {
                decodeDto(change.data, GroceryListDto.serializer())?.let { dto ->
                    // See TodoSyncManager.processRemoteLists: preserve local ordering when
                    // the server does not report a position.
                    val localPosition = dao.getListByIdOneShot(dto.id)?.position ?: 0
                    dao.upsertList(
                        dto.toEntity(fallbackPosition = localPosition)
                            .copy(syncState = "SYNCED", version = change.version)
                    )
                    upserted++
                }
            }
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote grocery lists: upserted=$upserted, deleted=$deleted")
    }

    private suspend fun applyRemoteMembers(dao: GroceryDao, remoteMembers: List<GroceryListMemberChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteMembers.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteListMember(change.id)
                deleted++
                return@forEach
            }
            val dto = decodeDto(change.data, GroceryListMemberDto.serializer()) ?: return@forEach
            if (!listExists(dao, dto.listId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring GroceryMember ${change.id} because Parent List ${dto.listId} is missing.")
                return@forEach
            }
            dao.upsertListMember(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
            upserted++
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote grocery members: upserted=$upserted, deleted=$deleted")
    }

    private suspend fun applyRemoteStores(dao: GroceryDao, remoteStores: List<StoreChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteStores.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteStore(change.id)
                deleted++
                return@forEach
            }
            val dto = decodeDto(change.data, StoreDto.serializer()) ?: return@forEach
            if (!listExists(dao, dto.listId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring Store ${change.id} because Parent List ${dto.listId} is missing.")
                return@forEach
            }
            dao.upsertStore(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
            upserted++
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote stores: upserted=$upserted, deleted=$deleted")
    }

    private suspend fun applyRemoteCategories(dao: GroceryDao, remoteCategories: List<CategoryChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteCategories.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteCategory(change.id)
                deleted++
                return@forEach
            }
            val dto = decodeDto(change.data, CategoryDto.serializer()) ?: return@forEach
            if (!listExists(dao, dto.listId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring Category ${change.id} because Parent List ${dto.listId} is missing.")
                return@forEach
            }
            dao.upsertCategory(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
            upserted++
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote categories: upserted=$upserted, deleted=$deleted")
    }

    private suspend fun applyRemoteItems(dao: GroceryDao, remoteItems: List<GroceryChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteItems.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteItem(change.id)
                deleted++
                return@forEach
            }
            val dto = decodeDto(change.data, GroceryItemDto.serializer()) ?: return@forEach
            if (!listExists(dao, dto.listId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring GroceryItem ${change.id} because Parent List ${dto.listId} is missing.")
                return@forEach
            }
            dao.upsertItem(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
            upserted++
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote grocery items: upserted=$upserted, deleted=$deleted")
    }

    private suspend fun applyRemoteStoreInfos(dao: GroceryDao, remoteStoreInfos: List<GroceryItemStoreInfoChangeDelta>) {
        var upserted = 0
        var deleted = 0
        remoteStoreInfos.forEach { change ->
            if (change.operationType == OperationType.DELETE) {
                dao.hardDeleteStoreInfo(change.groceryItemId, change.storeId)
                deleted++
                return@forEach
            }
            val dto = decodeDto(change.data, GroceryItemStoreInfoDto.serializer()) ?: return@forEach
            if (!itemExists(dao, change.groceryItemId) || !storeExists(dao, change.storeId)) {
                Log.e(TAG, "INCONSISTENCY: Ignoring StoreInfo because dependencies are missing.")
                return@forEach
            }
            dao.upsertStoreInfo(dto.toEntity().copy(syncState = "SYNCED", version = change.version))
            upserted++
        }
        if (upserted > 0 || deleted > 0) Log.d(TAG, "Applied remote store infos: upserted=$upserted, deleted=$deleted")
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

    private suspend fun listExists(dao: GroceryDao, listId: String?): Boolean {
        if (listId == null) return true
        return dao.getListByIdOneShot(listId) != null
    }

    private suspend fun itemExists(dao: GroceryDao, itemId: String): Boolean {
        return dao.getItemByIdOneShot(itemId) != null
    }

    private suspend fun storeExists(dao: GroceryDao, storeId: String): Boolean {
        return dao.getStoreByIdOneShot(storeId) != null
    }
}
