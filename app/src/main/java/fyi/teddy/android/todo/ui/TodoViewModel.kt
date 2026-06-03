package fyi.teddy.android.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.util.TodoResetScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodoViewModel(
    application: Application,
    private val repository: TodoRepository,
    userId: String
) : AndroidViewModel(application) {

    // Internal state flows to enforce Unidirectional Data Flow (UDF)
    private val _currentMode = MutableStateFlow(TodoMode.BACKLOG)
    val currentMode: StateFlow<TodoMode> = _currentMode.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _showCompletedOnly = MutableStateFlow(false)
    val showCompletedOnly: StateFlow<Boolean> = _showCompletedOnly.asStateFlow()

    private val _recentlyCompletedIds = MutableStateFlow(setOf<Int>())
    val recentlyCompletedIds: StateFlow<Set<Int>> = _recentlyCompletedIds.asStateFlow()

    // Confetti trigger shared flow for UI effects
    private val _confettiTrigger = MutableSharedFlow<Unit>(replay = 0)
    val confettiTrigger: SharedFlow<Unit> = _confettiTrigger.asSharedFlow()

    // Cold/Hot source flows from repository
    val allItems = repository.getAllItems(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val todayItems = repository.getTodayItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val scheduledItems = repository.getScheduledItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Intermediate settings flow to combine multiple filter states
    private val settingsFlow = combine(
        _currentMode,
        _showCompletedOnly,
        _recentlyCompletedIds
    ) { mode, showCompleted, recentlyCompleted ->
        Triple(mode, showCompleted, recentlyCompleted)
    }

    /**
     * Unified grouped items state flow that automatically combines child-parent relationships,
     * filter modes, completion states, and active visual delays on the background threads.
     */
    val groupedItems: StateFlow<List<Pair<TodoItem, List<TodoItem>>>> = combine(
        allItems,
        todayItems,
        scheduledItems,
        settingsFlow
    ) { all, today, scheduled, settings ->
        val (mode, showCompleted, recentlyCompleted) = settings
        val todayString = LocalDate.now().toString()
        val baseItems = when(mode) {
            TodoMode.TODAY -> today
            TodoMode.BACKLOG -> all.filter { it.scheduledDate == null }
            TodoMode.TODAY_PLANNING -> all
            TodoMode.SCHEDULED -> scheduled
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
        }
    }

    fun setMode(mode: TodoMode) {
        _currentMode.value = mode
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
    fun insertItem(title: String, userId: String, parentId: Int?, scheduledDate: String?) {
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
                    scheduledDate = scheduledDate
                ))
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
