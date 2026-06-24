package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryList
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
                operationType = operationType,
                version = item.version,
                data = data
            )
        }
    }

    suspend fun collectLocalListChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryListChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedGroceryLists = if (isFirstSync) {
            groceryDao.getAllListsOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedLists()
        }

        return unsyncedGroceryLists.map { list ->
            val operationType = when {
                list.isDeleted -> OperationType.DELETE
                list.syncState == "PENDING_INSERT" -> OperationType.INSERT
                list.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(GroceryListDto.serializer(), list.toDto())
            }
            GroceryListChangeDelta(
                id = list.id,
                operationType = operationType,
                version = list.version,
                data = data
            )
        }
    }

    suspend fun collectLocalListMemberChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryListMemberChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedListMembers = if (isFirstSync) {
            groceryDao.getAllListMembersOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedListMembers()
        }

        return unsyncedListMembers.map { member ->
            val operationType = when {
                member.isDeleted -> OperationType.DELETE
                member.syncState == "PENDING_INSERT" -> OperationType.INSERT
                member.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(GroceryListMemberDto.serializer(), member.toDto())
            }
            GroceryListMemberChangeDelta(
                id = member.id,
                operationType = operationType,
                version = member.version,
                data = data
            )
        }
    }

    suspend fun collectLocalStoreChanges(db: AppDatabase, isFirstSync: Boolean): List<StoreChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedStores = if (isFirstSync) {
            groceryDao.getAllStoresOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedStores()
        }

        return unsyncedStores.map { store ->
            val operationType = when {
                store.isDeleted -> OperationType.DELETE
                store.syncState == "PENDING_INSERT" -> OperationType.INSERT
                store.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(StoreDto.serializer(), store.toDto())
            }
            StoreChangeDelta(
                id = store.id,
                operationType = operationType,
                version = store.version,
                data = data
            )
        }
    }

    suspend fun collectLocalCategoryChanges(db: AppDatabase, isFirstSync: Boolean): List<CategoryChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedCategories = if (isFirstSync) {
            groceryDao.getAllCategoriesOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedCategories()
        }

        return unsyncedCategories.map { category ->
            val operationType = when {
                category.isDeleted -> OperationType.DELETE
                category.syncState == "PENDING_INSERT" -> OperationType.INSERT
                category.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(CategoryDto.serializer(), category.toDto())
            }
            CategoryChangeDelta(
                id = category.id,
                operationType = operationType,
                version = category.version,
                data = data
            )
        }
    }

    suspend fun collectLocalStoreInfoChanges(db: AppDatabase, isFirstSync: Boolean): List<GroceryItemStoreInfoChangeDelta> {
        val groceryDao = db.groceryDao()
        val unsyncedStoreInfos = if (isFirstSync) {
            groceryDao.getAllStoreInfosOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedStoreInfos()
        }

        return unsyncedStoreInfos.map { info ->
            val operationType = when {
                info.isDeleted -> OperationType.DELETE
                info.syncState == "PENDING_INSERT" -> OperationType.INSERT
                info.syncState == "PENDING_UPDATE" -> OperationType.UPDATE
                else -> OperationType.UPDATE
            }
            val data = if (operationType == OperationType.DELETE) null else {
                Json.encodeToJsonElement(GroceryItemStoreInfoDto.serializer(), info.toDto())
            }
            GroceryItemStoreInfoChangeDelta(
                id = "${info.groceryItemId}_${info.storeId}",
                groceryItemId = info.groceryItemId,
                storeId = info.storeId,
                operationType = operationType,
                version = info.version,
                data = data
            )
        }
    }

    private suspend fun ensureListExists(dao: GroceryDao, listId: String?) {
        if (listId == null) return
        val existing = dao.getListByIdOneShot(listId)
        if (existing == null) {
            dao.insertList(
                GroceryList(
                    id = listId,
                    name = "Syncing List...",
                    syncState = "SYNCED",
                    version = 0
                )
            )
        }
    }

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
        val unsyncedListMembers = if (isFirstSync) {
            groceryDao.getAllListMembersOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedListMembers()
        }
        val unsyncedStores = if (isFirstSync) {
            groceryDao.getAllStoresOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedStores()
        }
        val unsyncedCategories = if (isFirstSync) {
            groceryDao.getAllCategoriesOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedCategories()
        }
        val unsyncedStoreInfos = if (isFirstSync) {
            groceryDao.getAllStoreInfosOneShot().map { it.copy(syncState = "PENDING_INSERT") }
        } else {
            groceryDao.getUnsyncedStoreInfos()
        }

        // 1. Transition successfully uploaded grocery lists back to sync_state = SYNCED
        unsyncedGroceryLists.forEach { localGroceryList ->
            if (localGroceryList.isDeleted) {
                if (successIds.contains(localGroceryList.id)) {
                    groceryDao.hardDeleteList(localGroceryList.id)
                }
            } else {
                if (successIds.contains(localGroceryList.id)) {
                    groceryDao.insertList(localGroceryList.copy(syncState = "SYNCED"))
                }
            }
        }

        // 2. Transition successfully uploaded grocery list members back to sync_state = SYNCED
        unsyncedListMembers.forEach { localMember ->
            if (localMember.isDeleted) {
                if (successIds.contains(localMember.id)) {
                    groceryDao.hardDeleteListMember(localMember.id)
                }
            } else {
                if (successIds.contains(localMember.id)) {
                    groceryDao.insertListMember(localMember.copy(syncState = "SYNCED"))
                }
            }
        }

        // 3. Transition successfully uploaded stores back to sync_state = SYNCED
        unsyncedStores.forEach { localStore ->
            val stringId = localStore.id.toString()
            if (localStore.isDeleted) {
                if (successIds.contains(stringId)) {
                    groceryDao.hardDeleteStore(localStore.id)
                }
            } else {
                if (successIds.contains(stringId)) {
                    groceryDao.insertStore(localStore.copy(syncState = "SYNCED"))
                }
            }
        }

        // 4. Transition successfully uploaded categories back to sync_state = SYNCED
        unsyncedCategories.forEach { localCategory ->
            val stringId = localCategory.id.toString()
            if (localCategory.isDeleted) {
                if (successIds.contains(stringId)) {
                    groceryDao.hardDeleteCategory(localCategory.id)
                }
            } else {
                if (successIds.contains(stringId)) {
                    groceryDao.insertCategory(localCategory.copy(syncState = "SYNCED"))
                }
            }
        }

        // 5. Transition successfully uploaded grocery items back to sync_state = SYNCED
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

        // 6. Transition successfully uploaded store infos back to sync_state = SYNCED
        unsyncedStoreInfos.forEach { localInfo ->
            val compositeId = "${localInfo.groceryItemId}_${localInfo.storeId}"
            if (localInfo.isDeleted) {
                if (successIds.contains(compositeId)) {
                    groceryDao.hardDeleteStoreInfo(localInfo.groceryItemId, localInfo.storeId)
                }
            } else {
                if (successIds.contains(compositeId)) {
                    groceryDao.insertStoreInfo(localInfo.copy(syncState = "SYNCED"))
                }
            }
        }

        // 1. Upsert incoming remote_grocery_list_changes into local Room DB (Parent of most)
        remoteListChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteList(changeDelta.id)
            } else {
                val listDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(GroceryListDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (listDto != null) {
                    groceryDao.insertList(listDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // 2. Upsert incoming remote_grocery_list_member_changes into local Room DB (Depends on List)
        remoteListMemberChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteListMember(changeDelta.id)
            } else {
                val memberDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(GroceryListMemberDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (memberDto != null) {
                    ensureListExists(groceryDao, memberDto.listId)
                    groceryDao.insertListMember(memberDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // 3. Upsert incoming remote_store_changes into local Room DB (Depends on List)
        remoteStoreChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteStore(changeDelta.id)
            } else {
                val storeDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(StoreDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (storeDto != null) {
                    ensureListExists(groceryDao, storeDto.listId)
                    groceryDao.insertStore(storeDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // 4. Upsert incoming remote_category_changes into local Room DB (Depends on List)
        remoteCategoryChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteCategory(changeDelta.id)
            } else {
                val categoryDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(CategoryDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (categoryDto != null) {
                    ensureListExists(groceryDao, categoryDto.listId)
                    groceryDao.insertCategory(categoryDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // 5. Upsert incoming remote_grocery_changes into local Room DB (Depends on List and Category)
        remoteChanges.forEach { changeDelta ->
            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteItem(changeDelta.id)
            } else {
                val groceryDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(GroceryItemDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (groceryDto != null) {
                    ensureListExists(groceryDao, groceryDto.listId)
                    groceryDao.insertItem(groceryDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }

        // 6. Upsert incoming remote_grocery_item_store_info_changes into local Room DB (Depends on Item and Store)
        remoteStoreInfoChanges.forEach { changeDelta ->
            val groceryItemId = changeDelta.groceryItemId
            val storeId = changeDelta.storeId

            if (changeDelta.operationType == OperationType.DELETE) {
                groceryDao.hardDeleteStoreInfo(groceryItemId, storeId)
            } else {
                val infoDto = changeDelta.data?.let {
                    try {
                        Json.decodeFromJsonElement(GroceryItemStoreInfoDto.serializer(), it)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (infoDto != null) {
                    groceryDao.insertStoreInfo(infoDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }
    }
}
