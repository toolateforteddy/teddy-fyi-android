package fyi.teddy.android.network

import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryList

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
                NetworkClient.syncJson.encodeToJsonElement(GroceryItemDto.serializer(), item.toDto())
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
                NetworkClient.syncJson.encodeToJsonElement(GroceryListDto.serializer(), list.toDto())
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
                NetworkClient.syncJson.encodeToJsonElement(GroceryListMemberDto.serializer(), member.toDto())
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
                NetworkClient.syncJson.encodeToJsonElement(StoreDto.serializer(), store.toDto())
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
                NetworkClient.syncJson.encodeToJsonElement(CategoryDto.serializer(), category.toDto())
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
                NetworkClient.syncJson.encodeToJsonElement(GroceryItemStoreInfoDto.serializer(), info.toDto())
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
                    groceryDao.upsertList(localGroceryList.copy(syncState = "SYNCED"))
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
                    groceryDao.upsertListMember(localMember.copy(syncState = "SYNCED"))
                }
            }
        }

        // 3. Transition successfully uploaded stores back to sync_state = SYNCED
        unsyncedStores.forEach { localStore ->
            if (localStore.isDeleted) {
                if (successIds.contains(localStore.id)) {
                    groceryDao.hardDeleteStore(localStore.id)
                }
            } else {
                if (successIds.contains(localStore.id)) {
                    groceryDao.upsertStore(localStore.copy(syncState = "SYNCED"))
                }
            }
        }

        // 4. Transition successfully uploaded categories back to sync_state = SYNCED
        unsyncedCategories.forEach { localCategory ->
            if (localCategory.isDeleted) {
                if (successIds.contains(localCategory.id)) {
                    groceryDao.hardDeleteCategory(localCategory.id)
                }
            } else {
                if (successIds.contains(localCategory.id)) {
                    groceryDao.upsertCategory(localCategory.copy(syncState = "SYNCED"))
                }
            }
        }

        // 5. Transition successfully uploaded grocery items back to sync_state = SYNCED
        unsyncedGroceryItems.forEach { localGroceryItem ->
            if (localGroceryItem.isDeleted) {
                if (successIds.contains(localGroceryItem.id)) {
                    groceryDao.hardDeleteItem(localGroceryItem.id)
                }
            } else {
                if (successIds.contains(localGroceryItem.id)) {
                    groceryDao.upsertItem(localGroceryItem.copy(syncState = "SYNCED"))
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
                    groceryDao.upsertStoreInfo(localInfo.copy(syncState = "SYNCED"))
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
                        NetworkClient.syncJson.decodeFromJsonElement(GroceryListDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode GroceryListDto: ${e.message}", e)
                        null
                    }
                }
                if (listDto != null) {
                    groceryDao.upsertList(listDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
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
                        NetworkClient.syncJson.decodeFromJsonElement(GroceryListMemberDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode GroceryListMemberDto: ${e.message}", e)
                        null
                    }
                }
                if (memberDto != null) {
                    ensureListExists(groceryDao, memberDto.listId)
                    groceryDao.upsertListMember(memberDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
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
                        NetworkClient.syncJson.decodeFromJsonElement(StoreDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode StoreDto: ${e.message}", e)
                        null
                    }
                }
                if (storeDto != null) {
                    ensureListExists(groceryDao, storeDto.listId)
                    groceryDao.upsertStore(storeDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
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
                        NetworkClient.syncJson.decodeFromJsonElement(CategoryDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode CategoryDto: ${e.message}", e)
                        null
                    }
                }
                if (categoryDto != null) {
                    ensureListExists(groceryDao, categoryDto.listId)
                    groceryDao.upsertCategory(categoryDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
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
                        NetworkClient.syncJson.decodeFromJsonElement(GroceryItemDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode GroceryItemDto: ${e.message}", e)
                        null
                    }
                }
                if (groceryDto != null) {
                    ensureListExists(groceryDao, groceryDto.listId)
                    groceryDao.upsertItem(groceryDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
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
                        NetworkClient.syncJson.decodeFromJsonElement(GroceryItemStoreInfoDto.serializer(), it)
                    } catch (e: Exception) {
                        android.util.Log.e("GrocerySyncManager", "Failed to decode GroceryItemStoreInfoDto: ${e.message}", e)
                        null
                    }
                }
                if (infoDto != null) {
                    groceryDao.upsertStoreInfo(infoDto.toEntity().copy(syncState = "SYNCED", version = changeDelta.version))
                }
            }
        }
    }
}
