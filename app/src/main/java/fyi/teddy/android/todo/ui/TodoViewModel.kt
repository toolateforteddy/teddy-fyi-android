package fyi.teddy.android.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.todo.domain.MoveItemDownUseCase
import fyi.teddy.android.todo.domain.MoveItemToBottomUseCase
import fyi.teddy.android.todo.domain.MoveItemToTopUseCase
import fyi.teddy.android.todo.domain.MoveItemUpUseCase
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.repository.claimUnownedItems
import fyi.teddy.android.todo.repository.deleteList
import fyi.teddy.android.todo.repository.insertList
import fyi.teddy.android.todo.repository.updateList
import fyi.teddy.android.todo.repository.updateListPositions
import fyi.teddy.android.todo.util.TodoResetScheduler
import fyi.teddy.android.util.StringUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

private const val FLOW_TIMEOUT_MS = 5000L
private const val CONFETTI_DELAY_MS = 2000L

@Suppress("TooManyFunctions")
class TodoViewModel(
    application: Application,
    internal val repository: TodoRepository,
    private val userId: String,
    initialMode: String? = null
) : AndroidViewModel(application) {

    private val _currentMode = MutableStateFlow(
        if (initialMode != null) {
            try {
                TodoMode.valueOf(initialMode)
            } catch (_: Exception) {
                TodoMode.BACKLOG
            }
        } else {
            TodoMode.BACKLOG
        }
    )
    val currentMode: StateFlow<TodoMode> = _currentMode.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _showCompletedOnly = MutableStateFlow(false)
    val showCompletedOnly: StateFlow<Boolean> = _showCompletedOnly.asStateFlow()

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId: StateFlow<String?> = _selectedListId.asStateFlow()

    val allLists = repository.getAllLists(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val todayDateFlow = MutableStateFlow(LocalDate.now().toString())

    val allItems = repository.getAllItems(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayItems = todayDateFlow.flatMapLatest { today ->
        repository.getTodayItems(userId, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scheduledItems = todayDateFlow.flatMapLatest { today ->
        repository.getScheduledItems(userId, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptyList())

    data class TodoListUiModel(
        val list: TodoList,
        val incompleteCount: Int = 0
    )

    val displayedLists: StateFlow<List<TodoListUiModel>> = combine(
        allLists,
        todayItems,
        _currentMode,
        _isEditMode,
        todayDateFlow
    ) { lists, todayItemsForCount, mode, isEditMode, todayString ->
        val incompleteToday = todayItemsForCount.filter { !it.isCompleted }
        val allParents = incompleteToday.filter { it.parentId == null }
        val allChildren = incompleteToday.filter { it.parentId != null }.groupBy { it.parentId }

        val todayCountByList = mutableMapOf<String, Int>()

        if (mode == TodoMode.TODAY && !isEditMode) {
            allParents.forEach { parent ->
                val isParentScheduledToday = parent.scheduledDate == todayString
                val hasChildrenScheduledToday = allChildren[parent.id]?.any { it.scheduledDate == todayString } == true

                if (isParentScheduledToday || hasChildrenScheduledToday) {
                    parent.listId?.let { lid ->
                        todayCountByList[lid] = (todayCountByList[lid] ?: 0) + 1
                    }

                    allChildren[parent.id]?.forEach { child ->
                        if (child.scheduledDate == todayString || (isParentScheduledToday && child.scheduledDate == null)) {
                            child.listId?.let { lid ->
                                todayCountByList[lid] = (todayCountByList[lid] ?: 0) + 1
                            }
                        }
                    }
                }
            }

            lists.map { list ->
                TodoListUiModel(list, todayCountByList[list.id] ?: 0)
            }.filter { it.incompleteCount > 0 }
                .sortedByDescending { it.incompleteCount }
        } else {
            incompleteToday.forEach { item ->
                item.listId?.let { lid ->
                    todayCountByList[lid] = (todayCountByList[lid] ?: 0) + 1
                }
            }
            lists.map { list ->
                TodoListUiModel(list, todayCountByList[list.id] ?: 0)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptyList())

    private val _recentlyCompletedIds = MutableStateFlow(setOf<String>())
    val recentlyCompletedIds: StateFlow<Set<String>> = _recentlyCompletedIds.asStateFlow()

    private val _selectedPlanningDate = MutableStateFlow(LocalDate.now().toString())
    val selectedPlanningDate: StateFlow<String> = _selectedPlanningDate.asStateFlow()

    fun setSelectedPlanningDate(date: String) {
        _selectedPlanningDate.value = date
    }

    private val _confettiTrigger = MutableSharedFlow<Unit>(replay = 0)
    val confettiTrigger: SharedFlow<Unit> = _confettiTrigger.asSharedFlow()

    internal val moveItemUpUseCase = MoveItemUpUseCase(repository)
    internal val moveItemDownUseCase = MoveItemDownUseCase(repository)
    internal val moveItemToTopUseCase = MoveItemToTopUseCase(repository)
    internal val moveItemToBottomUseCase = MoveItemToBottomUseCase(repository)

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    data class TodoFilterSettings(
        val mode: TodoMode,
        val showCompleted: Boolean,
        val recentlyCompleted: Set<String>,
        val selectedListId: String?,
        val isSearchActive: Boolean,
        val searchQuery: String
    )

    private val searchFlow = combine(_isSearchActive, _searchQuery) { active, query ->
        active to query
    }

    private val settingsFlow = combine(
        _currentMode,
        _showCompletedOnly,
        _recentlyCompletedIds,
        _selectedListId,
        searchFlow
    ) { mode, showCompleted, recentlyCompleted, selectedListId, searchPair ->
        val (isSearchActive, searchQuery) = searchPair
        TodoFilterSettings(mode, showCompleted, recentlyCompleted, selectedListId, isSearchActive, searchQuery)
    }

    val groupedItems: StateFlow<List<Pair<TodoItem, List<TodoItem>>>> = combine(
        allItems,
        todayItems,
        scheduledItems,
        settingsFlow,
        todayDateFlow
    ) { all, today, scheduled, settings, todayString ->
        val mode = settings.mode
        val showCompleted = settings.showCompleted
        val recentlyCompleted = settings.recentlyCompleted
        val selectedListId = settings.selectedListId

        val listFilteredAll = all.filter { (if (selectedListId != null) it.listId == selectedListId else true) && !it.isDeleted }
        val listFilteredToday = today.filter { (if (selectedListId != null) it.listId == selectedListId else true) && !it.isDeleted }
        val listFilteredScheduled = scheduled.filter { (if (selectedListId != null) it.listId == selectedListId else true) && !it.isDeleted }

        if (settings.isSearchActive && settings.searchQuery.trim().isNotBlank()) {
            val query = settings.searchQuery.trim().lowercase()
            val allParents = listFilteredAll.filter { it.parentId == null }
            val allChildren = listFilteredAll.filter { it.parentId != null }.groupBy { it.parentId }

            val tier1 = mutableListOf<Pair<TodoItem, List<TodoItem>>>()
            val tier2 = mutableListOf<Pair<TodoItem, List<TodoItem>>>()
            val tier3 = mutableListOf<Pair<TodoItem, List<TodoItem>>>()
            val tier4 = mutableListOf<Pair<TodoItem, List<TodoItem>>>()

            allParents.forEach { parent ->
                val children = allChildren[parent.id] ?: emptyList()
                val parentTitleMatches = parent.title.lowercase().contains(query)
                val subtaskTitleMatches = children.any { it.title.lowercase().contains(query) }
                val parentDescMatches = parent.description?.lowercase()?.contains(query) == true
                val subtaskDescMatches = children.any { it.description?.lowercase()?.contains(query) == true }
                val descriptionMatches = parentDescMatches || subtaskDescMatches

                if (!parent.isCompleted) {
                    if (parentTitleMatches) {
                        tier1.add(parent to children)
                    } else if (subtaskTitleMatches) {
                        tier2.add(parent to children)
                    } else if (descriptionMatches) {
                        tier3.add(parent to children)
                    }
                } else {
                    if (parentTitleMatches || subtaskTitleMatches || descriptionMatches) {
                        tier4.add(parent to children)
                    }
                }
            }

            return@combine tier1 + tier2 + tier3 + tier4
        }

        val baseItems = when (mode) {
            TodoMode.TODAY -> listFilteredToday
            TodoMode.BACKLOG -> listFilteredAll.filter { it.scheduledDate == null }
            TodoMode.PLANNING -> listFilteredAll
            TodoMode.SCHEDULED -> listFilteredScheduled
        }

        val filteredItems = baseItems.filter { item ->
            if (showCompleted) item.isCompleted
            else !item.isCompleted || recentlyCompleted.contains(item.id)
        }.let { items ->
            if (showCompleted) items.sortedByDescending { it.scheduledAt }
            else items.sortedWith(
                compareByDescending<TodoItem> { it.lastScheduledDate != null }
                    .thenByDescending { it.priority }
                    .thenBy { it.position }
                    .thenByDescending { it.createdAt }
            )
        }

        val allParents = filteredItems.filter { it.parentId == null }
        val allChildren = filteredItems.filter { it.parentId != null }.groupBy { it.parentId }

        if (mode == TodoMode.TODAY || mode == TodoMode.SCHEDULED) {
            val result = if (mode == TodoMode.SCHEDULED) {
                allParents.map { it to (allChildren[it.id] ?: emptyList()) }
            } else {
                allParents.filter { parent ->
                    parent.scheduledDate == todayString ||
                            allChildren[parent.id]?.any { it.scheduledDate == todayString } == true
                }.map { parent ->
                    val childrenForToday = allChildren[parent.id]?.filter { child ->
                        child.scheduledDate == todayString || (parent.scheduledDate == todayString && child.scheduledDate == null)
                    } ?: emptyList()
                    parent to childrenForToday
                }
            }
            result
        } else {
            allParents.map { it to (allChildren[it.id] ?: emptyList()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            repository.claimUnownedItems(userId)
        }

        viewModelScope.launch {
            val resetScheduler = TodoResetScheduler(application, repository)
            resetScheduler.checkAndResetDailyTasks(userId)
            val today = LocalDate.now().toString()
            todayDateFlow.value = today

            val todayItemsList = repository.getTodayItems(userId, today).first()
            if (todayItemsList.isNotEmpty() && _currentMode.value == TodoMode.BACKLOG && initialMode == null) {
                _currentMode.value = TodoMode.TODAY
            }
        }
    }

    fun setMode(mode: TodoMode) {
        _currentMode.value = mode
        todayDateFlow.value = LocalDate.now().toString()
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setShowCompletedOnly(show: Boolean) {
        _showCompletedOnly.value = show
    }

    fun toggleComplete(item: TodoItem, isChecked: Boolean) {
        if (isChecked && !_showCompletedOnly.value) {
            _recentlyCompletedIds.update { it + item.id }
            viewModelScope.launch {
                _confettiTrigger.emit(Unit)
                if (item.recurrenceRule != null) {
                    val nextTime = fyi.teddy.android.todo.util.TaskSchedulerUtils.calculateNextRecurrenceTime(
                        System.currentTimeMillis(),
                        item.recurrenceRule
                    )
                    updateItem(item.copy(
                        isCompleted = false,
                        scheduledAt = nextTime,
                        scheduledDate = null
                    ))
                } else {
                    updateItem(item.copy(isCompleted = true))
                }
                delay(CONFETTI_DELAY_MS.milliseconds)
                _recentlyCompletedIds.update { it - item.id }
            }
        } else {
            _recentlyCompletedIds.update { it - item.id }
            updateItem(item.copy(isCompleted = isChecked))
        }
    }

    fun insertItem(title: String, userId: String, parentId: String?, scheduledDate: String?) {
        if (title.isNotBlank()) {
            val capitalizedTitle = formatTitle(title)
            viewModelScope.launch {
                repository.insertItem(TodoItem(
                    title = capitalizedTitle,
                    userId = userId,
                    parentId = parentId,
                    scheduledDate = scheduledDate,
                    listId = _selectedListId.value
                ))
            }
        }
    }

    private fun formatTitle(title: String): String {
        return StringUtils.formatTitle(title)
    }

    fun selectList(listId: String?) {
        _selectedListId.value = listId
    }

    fun insertList(name: String, colorHex: String = "#000000") {
        if (name.isNotBlank()) {
            val capitalizedName = formatTitle(name)
            viewModelScope.launch {
                repository.insertList(
                    TodoList(
                        name = capitalizedName,
                        colorHex = colorHex,
                        userId = userId
                    )
                )
            }
        }
    }

    fun updateList(list: TodoList) {
        val formattedList = list.copy(name = formatTitle(list.name))
        viewModelScope.launch {
            repository.updateList(formattedList)
        }
    }

    fun reorderLists(lists: List<TodoList>) {
        viewModelScope.launch {
            repository.updateListPositions(lists)
        }
    }

    fun deleteList(list: TodoList) {
        viewModelScope.launch {
            repository.deleteList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
            }
        }
    }

    fun insertItem(item: TodoItem) {
        val formattedItem = item.copy(title = formatTitle(item.title))
        viewModelScope.launch { repository.insertItem(formattedItem) }
    }

    fun updateItem(item: TodoItem) {
        val formattedItem = item.copy(title = formatTitle(item.title))
        viewModelScope.launch { repository.updateItem(formattedItem) }
    }

    fun deleteItem(item: TodoItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun deleteAll(userId: String) {
        viewModelScope.launch { repository.deleteAll(userId) }
    }
}
