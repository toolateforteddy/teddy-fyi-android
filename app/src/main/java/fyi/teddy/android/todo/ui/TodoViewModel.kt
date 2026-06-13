package fyi.teddy.android.todo.ui

import android.app.Application
import android.widget.Toast
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
    private val userId: String,
    initialMode: String? = null
) : AndroidViewModel(application) {

    // Internal state flows to enforce Unidirectional Data Flow (UDF)
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

    private val moveItemUpUseCase = fyi.teddy.android.todo.domain.MoveItemUpUseCase(repository)
    private val moveItemDownUseCase = fyi.teddy.android.todo.domain.MoveItemDownUseCase(repository)
    private val moveItemToTopUseCase = fyi.teddy.android.todo.domain.MoveItemToTopUseCase(repository)
    private val moveItemToBottomUseCase = fyi.teddy.android.todo.domain.MoveItemToBottomUseCase(repository)

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
            else items.sortedWith(
                compareByDescending<TodoItem> { it.lastScheduledDate != null } // Rolled over items first in Backlog
                    .thenByDescending { it.priority }
                    .thenBy { it.position }
                    .thenByDescending { it.createdAt }
            )
        }

        val allParents = filteredItems.filter { it.parentId == null }
        val allChildren = filteredItems.filter { it.parentId != null }.groupBy { it.parentId }

        if (mode == TodoMode.TODAY || mode == TodoMode.SCHEDULED) {
            val result = if (mode == TodoMode.SCHEDULED) {
                // For scheduled, we keep them flat or grouped by date later in UI
                allParents.map { it to (allChildren[it.id] ?: emptyList()) }
            } else {
                allParents.filter { parent ->
                    parent.scheduledDate == (if (mode == TodoMode.TODAY) todayString else parent.scheduledDate) || 
                            allChildren[parent.id]?.any { it.scheduledDate == (if (mode == TodoMode.TODAY) todayString else it.scheduledDate) } == true
                }.map { parent ->
                    parent to (allChildren[parent.id] ?: emptyList())
                }
            }
            result
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

    fun moveItemUp(item: TodoItem) {
        viewModelScope.launch { moveItemUpUseCase(item) }
    }

    fun moveItemDown(item: TodoItem) {
        viewModelScope.launch { moveItemDownUseCase(item) }
    }

    fun moveItemToTop(item: TodoItem) {
        viewModelScope.launch { moveItemToTopUseCase(item) }
    }

    fun moveItemToBottom(item: TodoItem) {
        viewModelScope.launch { moveItemToBottomUseCase(item) }
    }

    fun assignIcon(item: TodoItem) {
        viewModelScope.launch {
            val session = fyi.teddy.android.auth.UserSession()
            session.load(getApplication())
            val token = session.idToken
            if (token != null) {
                val iconName = repository.assignIcon(item, token)
                if (iconName != null) {
                    Toast.makeText(getApplication(), "Assigned icon: $iconName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Failed to assign icon", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
