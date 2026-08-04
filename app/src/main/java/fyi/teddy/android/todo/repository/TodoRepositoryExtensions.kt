@file:Suppress("unused")
package fyi.teddy.android.todo.repository

import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.todo.util.TaskSchedulerUtils

suspend fun TodoRepository.insertList(list: TodoList) {
    todoDao.insertList(list)
    scheduleSync()
}

suspend fun TodoRepository.updateList(list: TodoList) {
    val nextSyncState = if (list.syncState == "SYNCED") "PENDING_UPDATE" else list.syncState
    todoDao.updateList(list.copy(syncState = nextSyncState))
    scheduleSync()
}

suspend fun TodoRepository.updateListPositions(lists: List<TodoList>) {
    todoDao.updateListPositions(lists)
}

suspend fun TodoRepository.deleteList(list: TodoList) {
    if (list.syncState == "PENDING_INSERT") {
        todoDao.deleteListAndNullifyItems(list)
    } else {
        todoDao.nullifyListIdForItems(list.id)
        todoDao.updateList(list.copy(syncState = "PENDING_DELETE", isDeleted = true))
    }
    scheduleSync()
}

suspend fun TodoRepository.resetPlannedItems(
    userId: String,
    today: String = TaskSchedulerUtils.getTodayDateString()
) {
    todoDao.resetPlannedItems(userId, today)
    scheduleSync()
}

suspend fun TodoRepository.claimUnownedItems(userId: String) {
    todoDao.claimUnownedItems(userId)
    scheduleSync()
}

suspend fun TodoRepository.resetDailyItems(userId: String) {
    val today = TaskSchedulerUtils.getTodayDateString()
    todoDao.resetDailyItems(userId, today)
    scheduleSync()
}

suspend fun TodoRepository.swapPositions(item1: TodoItem, item2: TodoItem) {
    todoDao.swapPositions(item1, item2)
    scheduleSync()
}
