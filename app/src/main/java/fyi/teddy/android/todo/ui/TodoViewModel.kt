package fyi.teddy.android.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.util.TodoResetScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodoViewModel(
    application: Application,
    private val repository: TodoRepository,
    private val userId: String
) : AndroidViewModel(application) {

    // Internal state flows to enforce Unidirectional Data Flow (UDF)
    private val _currentMode = MutableStateFlow(TodoMode.BACKLOG)
    val currentMode: StateFlow<TodoMode> = _currentMode.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _showCompletedOnly = MutableStateFlow(false)
    val showCompletedOnly: StateFlow<Boolean> = _showCompletedOnly.asStateFlow()

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId: StateFlow<String?> = _selectedListId.asStateFlow()

    val allLists = repository.getAllLists(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _recentlyCompletedIds = MutableStateFlow(setOf<String>())
    val recentlyCompletedIds: StateFlow<Set<String>> = _recentlyCompletedIds.asStateFlow()

    private val _selectedPlanningDate = MutableStateFlow(LocalDate.now().toString())
    val selectedPlanningDate: StateFlow<String> = _selectedPlanningDate.asStateFlow()

    fun setSelectedPlanningDate(date: String) {
        _selectedPlanningDate.value = date
    }

    // Confetti trigger shared flow for UI effects
    private val _confettiTrigger = MutableSharedFlow<Unit>(replay = 0)
    val confettiTrigger: SharedFlow<Unit> = _confettiTrigger.asSharedFlow()

    private val todayDateFlow = MutableStateFlow(LocalDate.now().toString())

    // Cold/Hot source flows from repository
    val allItems = repository.getAllItems(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayItems = todayDateFlow.flatMapLatest { today ->
        repository.getTodayItems(userId, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scheduledItems = todayDateFlow.flatMapLatest { today ->
        repository.getScheduledItems(userId, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class TodoFilterSettings(
        val mode: TodoMode,
        val showCompleted: Boolean,
        val recentlyCompleted: Set<String>,
        val selectedListId: String?
    )

    // Intermediate settings flow to combine multiple filter states
    private val settingsFlow = combine(
        _currentMode,
        _showCompletedOnly,
        _recentlyCompletedIds,
        _selectedListId
    ) { mode, showCompleted, recentlyCompleted, selectedListId ->
        TodoFilterSettings(mode, showCompleted, recentlyCompleted, selectedListId)
    }

    /**
     * Unified grouped items state flow that automatically combines child-parent relationships,
     * filter modes, completion states, and active visual delays on the background threads.
     */
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
        
        val listFilteredAll = all.filter { if (selectedListId != null) it.listId == selectedListId else true }
        val listFilteredToday = today.filter { if (selectedListId != null) it.listId == selectedListId else true }
        val listFilteredScheduled = scheduled.filter { if (selectedListId != null) it.listId == selectedListId else true }

        val baseItems = when(mode) {
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
            else items
        }

        val allParents = filteredItems.filter { it.parentId == null }
        val allChildren = filteredItems.filter { it.parentId != null }.groupBy { it.parentId }

        if (mode == TodoMode.TODAY || mode == TodoMode.SCHEDULED) {
            allParents.filter { parent ->
                parent.scheduledDate == (if (mode == TodoMode.TODAY) todayString else parent.scheduledDate) || 
                        allChildren[parent.id]?.any { it.scheduledDate == (if (mode == TodoMode.TODAY) todayString else it.scheduledDate) } == true
            }.map { parent ->
                parent to (allChildren[parent.id] ?: emptyList())
            }
        } else {
            allParents.map { it to (allChildren[it.id] ?: emptyList()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            if (todayItemsList.isNotEmpty() && _currentMode.value == TodoMode.BACKLOG) {
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

    /**
     * Toggles the completion state of an item.
     * If the item is marked as completed, handles the 2-second visual delay for confetti,
     * recurrence calculation, and database writes safely in the ViewModel's scope.
     */
    fun toggleComplete(item: TodoItem, isChecked: Boolean) {
        if (isChecked && !_showCompletedOnly.value) {
            _recentlyCompletedIds.update { it + item.id }
            viewModelScope.launch {
                _confettiTrigger.emit(Unit)
                delay(2000)
                _recentlyCompletedIds.update { it - item.id }
                
                if (item.recurrenceIntervalDays != null) {
                    val nextTime = fyi.teddy.android.todo.util.TaskSchedulerUtils.calculateNextRecurrenceTime(
                        System.currentTimeMillis(),
                        item.recurrenceIntervalDays
                    )
                    updateItem(item.copy(
                        isCompleted = false,
                        scheduledAt = nextTime,
                        scheduledDate = null
                    ))
                } else {
                    updateItem(item.copy(isCompleted = true))
                }
            }
        } else {
            _recentlyCompletedIds.update { it - item.id }
            updateItem(item.copy(isCompleted = isChecked))
        }
    }

    /**
     * Formats, capitalizes, and inserts a new To-Do item.
     */
    fun insertItem(title: String, userId: String, parentId: String?, scheduledDate: String?) {
        if (title.isNotBlank()) {
            val words = title.split(" ")
            val capitalizedTitle = words.joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            }
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

    fun selectList(listId: String?) {
        _selectedListId.value = listId
    }

    fun insertList(name: String, colorHex: String = "#000000") {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.insertList(
                    fyi.teddy.android.todo.data.TodoList(
                        name = name,
                        colorHex = colorHex,
                        userId = userId
                    )
                )
            }
        }
    }

    fun updateList(list: fyi.teddy.android.todo.data.TodoList) {
        viewModelScope.launch {
            repository.updateList(list)
        }
    }

    fun deleteList(list: fyi.teddy.android.todo.data.TodoList) {
        viewModelScope.launch {
            repository.deleteList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
            }
        }
    }

    fun insertItem(item: TodoItem) {
        viewModelScope.launch { repository.insertItem(item) }
    }

    fun updateItem(item: TodoItem) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun deleteItem(item: TodoItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun deleteAll(userId: String) {
        viewModelScope.launch { repository.deleteAll(userId) }
    }

    fun swapPositions(item1: TodoItem, item2: TodoItem) {
        viewModelScope.launch { repository.swapPositions(item1, item2) }
    }

    fun moveParentToTop(item: TodoItem) {
        val parents = allItems.value.filter { it.parentId == null }
        val minPos = parents.minByOrNull { it.position }?.position ?: 0
        updateItem(item.copy(position = minPos - 1))
    }

    fun moveParentToBottom(item: TodoItem) {
        val parents = allItems.value.filter { it.parentId == null }
        val maxPos = parents.maxByOrNull { it.position }?.position ?: 0
        updateItem(item.copy(position = maxPos + 1))
    }

    fun moveChildToTop(item: TodoItem) {
        val parentId = item.parentId ?: return
        val children = allItems.value.filter { it.parentId == parentId }
        val minPos = children.minByOrNull { it.position }?.position ?: 0
        updateItem(item.copy(position = minPos - 1))
    }

    fun moveChildToBottom(item: TodoItem) {
        val parentId = item.parentId ?: return
        val children = allItems.value.filter { it.parentId == parentId }
        val maxPos = children.maxByOrNull { it.position }?.position ?: 0
        updateItem(item.copy(position = maxPos + 1))
    }
}
