package fyi.teddy.android.network

import android.util.Log
import fyi.teddy.android.grocery.data.GroceryDao

internal suspend fun processSuccessfulUploads(dao: GroceryDao, successIds: List<String>, isFirstSync: Boolean) {
    Log.d("GrocerySyncManager", "processSuccessfulUploads: successIds count = ${successIds.size}")
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
        val compositeId = "${local.groceryItemId}-${local.storeId}"
        if (successIds.contains(local.id) || successIds.contains(compositeId)) {
            if (local.isDeleted) dao.hardDeleteStoreInfo(local.groceryItemId, local.storeId)
            else dao.upsertStoreInfo(local.copy(syncState = "SYNCED"))
        } else {
            Log.d("GrocerySyncManager", "processSuccessfulStoreInfos: id ${local.id} (composite: $compositeId) not found in successIds ($successIds)")
        }
    }
}
