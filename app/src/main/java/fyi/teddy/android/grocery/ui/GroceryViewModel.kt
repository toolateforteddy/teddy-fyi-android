package fyi.teddy.android.grocery.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.data.SyncLog
import fyi.teddy.android.data.SyncLogDao
import fyi.teddy.android.data.UserSyncMetadataDao
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.domain.MoveGroceryItemDownUseCase
import fyi.teddy.android.grocery.domain.MoveGroceryItemUpUseCase
import fyi.teddy.android.grocery.domain.ai.GroceryCategorizer
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.network.SyncWorker
import fyi.teddy.android.util.StringUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList", "VariableNaming")
class GroceryViewModel(
    internal val repository: GroceryRepository,
    val userId: String,
    internal val application: Application,
    workManager: WorkManager? = null,
    internal val userSyncMetadataDao: UserSyncMetadataDao = AppDatabase.getDatabase(application).userSyncMetadataDao(),
    internal val syncLogDao: SyncLogDao = AppDatabase.getDatabase(application).syncLogDao(),
    private val prefs: SharedPreferences = application.getSharedPreferences("grocery_prefs", Context.MODE_PRIVATE)
) : ViewModel() {

    private val wm: WorkManager? = workManager ?: try { WorkManager.getInstance(application) } catch (_: Exception) { null }

    internal val _currentPhase = MutableStateFlow(GroceryPhase.NEED)
    internal val _selectedStoreIds = MutableStateFlow(setOf<String>())
    internal val _planningStoreContextId = MutableStateFlow<String?>(null)
    internal val _shoppingStoreId = MutableStateFlow(
        try {
            prefs.getString("last_shopping_store_id", null)
        } catch (_: ClassCastException) {
            prefs.edit { remove("last_shopping_store_id") }
            null
        }
    )
    internal val _isEditMode = MutableStateFlow(false)
    internal val _showRecommendedDialog = MutableStateFlow(false)
    internal val _newItemName = MutableStateFlow("")
    internal val _newItemQuantity = MutableStateFlow("1")
    internal val _newItemUnit = MutableStateFlow<String?>(null)
    internal val _newItemInput = MutableStateFlow("")
    internal val _selectedCategoryId = MutableStateFlow<String?>(null)
    internal val _recentlyCheckedIds = MutableStateFlow(setOf<String>())
    internal val _selectedListId = MutableStateFlow(
        try {
            prefs.getString("selected_list_id", null)
        } catch (_: ClassCastException) {
            prefs.edit { remove("selected_list_id") }
            null
        }
    )
    internal val _activeInviteCode = MutableStateFlow<String?>(null)
    internal val _snackbarMessage = MutableStateFlow<GrocerySnackbarMessage?>(null)
    internal val _isCategorizing = MutableStateFlow(false)
    internal val _dismissedRecommendationIds = MutableStateFlow<Set<String>>(emptySet())

    internal val categorizer = GroceryCategorizer(application)

    internal val _hasDefaultItems = combine(
        repository.getItemsWithoutList(userId),
        repository.getAllStores(userId).map { all -> all.any { it.listId == null && !it.isDeleted } },
        repository.getAllCategories(userId).map { all -> all.any { it.listId == null && !it.isDeleted } }
    ) { orphans, storesOrphaned, catsOrphaned ->
        orphans.isNotEmpty() || storesOrphaned || catsOrphaned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _unsyncedCount = repository.getUnsyncedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _lastSyncStatus = syncLogDao.getLatestLog()
        .map { log: SyncLog? -> log?.status }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSyncing = if (wm != null) {
        combine(
            wm.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME),
            wm.getWorkInfosForUniqueWorkFlow("PeriodicSyncWorker")
        ) { infos1, infos2 ->
            (infos1 + infos2).any { it.state == WorkInfo.State.RUNNING }
        }
    } else {
        flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isSyncEnqueued = if (wm != null) {
        combine(
            wm.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME),
            wm.getWorkInfosForUniqueWorkFlow("PeriodicSyncWorker")
        ) { infos1, infos2 ->
            (infos1 + infos2).any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
        }
    } else {
        flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val state: StateFlow<GroceryUiState> = combine(
        _currentPhase,
        _selectedStoreIds,
        _planningStoreContextId,
        _shoppingStoreId,
        _isEditMode,
        _showRecommendedDialog,
        _newItemName,
        _newItemQuantity,
        _newItemUnit,
        _newItemInput,
        _selectedCategoryId,
        _recentlyCheckedIds,
        _selectedListId,
        _activeInviteCode,
        _snackbarMessage,
        categorizer.isReady,
        _isCategorizing,
        _hasDefaultItems,
        _unsyncedCount,
        _lastSyncStatus,
        _isSyncing,
        _isSyncEnqueued,
        _dismissedRecommendationIds
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        GroceryUiState(
            currentPhase = args[0] as GroceryPhase,
            selectedStoreIds = args[1] as Set<String>,
            planningStoreContextId = args[2] as String?,
            shoppingStoreId = args[3] as String?,
            isEditMode = args[4] as Boolean,
            showRecommendedDialog = args[5] as Boolean,
            newItemName = args[6] as String,
            newItemQuantity = args[7] as String,
            newItemUnit = args[8] as String?,
            newItemInput = args[9] as String,
            selectedCategoryId = args[10] as String?,
            recentlyCheckedIds = args[11] as Set<String>,
            selectedListId = args[12] as String?,
            activeInviteCode = args[13] as String?,
            snackbarMessage = args[14] as GrocerySnackbarMessage?,
            isAiReady = args[15] as Boolean,
            isCategorizing = args[16] as Boolean,
            hasItemsInDefaultList = args[17] as Boolean,
            unsyncedCount = args[18] as Int,
            lastSyncStatus = args[19] as String?,
            isSyncing = args[20] as Boolean,
            isSyncEnqueued = args[21] as Boolean,
            dismissedRecommendationIds = args[22] as Set<String>
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GroceryUiState())



    internal fun setActiveInviteCode(code: String?) { _activeInviteCode.value = code }
    internal fun setSnackbarMessage(msg: GrocerySnackbarMessage?) { _snackbarMessage.value = msg }

    private val moveGroceryItemUpUseCase = MoveGroceryItemUpUseCase(repository)
    private val moveGroceryItemDownUseCase = MoveGroceryItemDownUseCase(repository)

    fun onEvent(event: GroceryUiEvent) {
        if (handleNavigationAndInputEvents(event)) return
        if (handleItemEvents(event)) return
        if (handleStoreEvents(event)) return
        if (handleCategoryEvents(event)) return
        handleListAndOtherEvents(event)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun handleNavigationAndInputEvents(event: GroceryUiEvent): Boolean {
        when (event) {
            is GroceryUiEvent.SetPhase -> setPhase(event.phase)
            is GroceryUiEvent.ToggleStoreSelection -> toggleStoreSelection(event.storeId)
            is GroceryUiEvent.SetPlanningStoreContext -> _planningStoreContextId.value = event.storeId
            is GroceryUiEvent.SetShoppingStoreId -> setShoppingStoreId(event.storeId)
            is GroceryUiEvent.SetEditMode -> setEditMode(event.enabled)
            is GroceryUiEvent.SetShowRecommendedDialog -> setShowRecommendedDialog(event.show)
            is GroceryUiEvent.SetNewItemName -> setNewItemName(event.name)
            is GroceryUiEvent.SetNewItemQuantity -> setNewItemQuantity(event.qty)
            is GroceryUiEvent.SetNewItemUnit -> setNewItemUnit(event.unit)
            is GroceryUiEvent.SetNewItemInput -> setNewItemInput(event.input)
            is GroceryUiEvent.SetSelectedCategoryId -> setSelectedCategoryId(event.categoryId)
            is GroceryUiEvent.SetSelectedListId -> setSelectedListId(event.listId)
            is GroceryUiEvent.InsertItemFromInput -> insertItemFromInput(event.input)
            else -> return false
        }
        return true
    }

    private fun handleItemEvents(event: GroceryUiEvent): Boolean {
        when (event) {
            is GroceryUiEvent.InsertItem -> insertItem(event.name, event.quantity, event.categoryId, event.unit)
            is GroceryUiEvent.UpdateItem -> updateItem(event.item)
            is GroceryUiEvent.DeleteItem -> deleteItem(event.item)
            is GroceryUiEvent.MoveItemUp -> viewModelScope.launch { moveGroceryItemUpUseCase(event.item, event.siblings) }
            is GroceryUiEvent.MoveItemDown -> viewModelScope.launch { moveGroceryItemDownUseCase(event.item, event.siblings) }
            is GroceryUiEvent.UpdateStoreInfo -> updateStoreInfo(event.info)
            is GroceryUiEvent.DeleteStoreInfo -> deleteStoreInfo(event.info)
            is GroceryUiEvent.ToggleBought -> toggleBought(event.item, event.isChecked)
            is GroceryUiEvent.MarkDoneForTrip -> markDoneForTrip()
            else -> return false
        }
        return true
    }

    private fun handleStoreEvents(event: GroceryUiEvent): Boolean {
        when (event) {
            is GroceryUiEvent.InsertStore -> insertStore(event.name)
            is GroceryUiEvent.DeleteStore -> deleteStore(event.store)
            is GroceryUiEvent.UpdateStore -> updateStore(event.store)
            is GroceryUiEvent.SwapStorePositions -> swapStorePositions(event.store1, event.store2)
            else -> return false
        }
        return true
    }

    private fun handleCategoryEvents(event: GroceryUiEvent): Boolean {
        when (event) {
            is GroceryUiEvent.InsertCategory -> insertCategory(event.name)
            is GroceryUiEvent.UpdateCategory -> updateCategory(event.category)
            is GroceryUiEvent.DeleteCategory -> deleteCategory(event.category)
            is GroceryUiEvent.SwapCategoryPositions -> swapCategoryPositions(event.cat1, event.cat2)
            else -> return false
        }
        return true
    }

    private fun handleListAndOtherEvents(event: GroceryUiEvent) {
        when (event) {
            is GroceryUiEvent.InsertList -> insertList(event.name)
            is GroceryUiEvent.DeleteList -> deleteList(event.list)
            is GroceryUiEvent.UpdateList -> updateList(event.list)
            is GroceryUiEvent.ReorderLists -> reorderLists(event.lists)
            is GroceryUiEvent.ShareList -> shareListWithUser(event.listId, event.userId)
            is GroceryUiEvent.CreateInvite -> createInvite(event.listId)
            is GroceryUiEvent.JoinList -> joinList(event.code)
            is GroceryUiEvent.DismissSnackbar -> {
                if (_snackbarMessage.value?.id == event.messageId) {
                    _snackbarMessage.value = null
                }
            }
            is GroceryUiEvent.RemoveListMember -> removeListMember(event.member)
            is GroceryUiEvent.AddRecommendedItems -> addRecommendedItems(event.selectedIds)
            is GroceryUiEvent.DismissRecommendation -> dismissRecommendation(event.itemId)
            else -> {}
        }
    }

    fun dismissRecommendation(itemId: String) {
        _dismissedRecommendationIds.update { it + itemId }
    }

    // Sources from repository
    @OptIn(ExperimentalCoroutinesApi::class)
    val items = _selectedListId.flatMapLatest { listId ->
        val flow = if (listId == null) {
            repository.getItemsWithoutList(userId)
        } else {
            repository.getItemsForList(listId)
        }
        flow.map { list -> list.filter { !it.isDeleted } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lists = repository.getAllLists(userId)
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stores = combine(repository.getAllStores(userId), _selectedListId) { allStores, listId ->
        allStores.filter { it.listId == listId && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = combine(repository.getAllCategories(userId), _selectedListId) { allCats, listId ->
        allCats.filter { it.listId == listId && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeInfos = repository.getAllStoreInfo(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedItems = repository.getRecommendedItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultListAndClaimOrphanedItems(userId)
            repository.claimEverything(userId)
            categorizer.initialize()

            // If we're on the "Default List" (null) but it's empty, and we have other lists, switch to the first one.
            combine(lists, _hasDefaultItems) { availableLists, hasDefault ->
                if (_selectedListId.value == null && !hasDefault && availableLists.isNotEmpty()) {
                    setSelectedListId(availableLists.first().id)
                }
            }.first()
        }
    }

    // Sanitize and format name inputs uniformly
    fun formatName(input: String): String {
        return StringUtils.formatTitle(input)
    }

    // Setters
    fun setPhase(phase: GroceryPhase) {
        _currentPhase.value = phase
        if (phase != GroceryPhase.SHOPPING) {
            _isEditMode.value = false
        }
    }

    fun toggleStoreSelection(storeId: String) {
        _selectedStoreIds.update { current ->
            if (current.contains(storeId)) current - storeId else current + storeId
        }
    }

    fun setShoppingStoreId(storeId: String?) {
        _shoppingStoreId.value = storeId
        if (storeId != null) {
            prefs.edit { putString("last_shopping_store_id", storeId) }
        } else {
            prefs.edit { remove("last_shopping_store_id") }
        }
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setShowRecommendedDialog(show: Boolean) {
        _showRecommendedDialog.value = show
    }

    fun setNewItemName(name: String) {
        _newItemName.value = name
    }

    fun setNewItemQuantity(qty: String) {
        _newItemQuantity.value = qty
    }

    fun setNewItemUnit(unit: String?) {
        _newItemUnit.value = unit
    }

    fun setNewItemInput(input: String) {
        _newItemInput.value = input
    }

    fun setSelectedCategoryId(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    // Core state flow combining all tables for categories and custom views
    val baseFilteredItems: StateFlow<List<GroceryItem>> = combine(
        items,
        storeInfos,
        stores,
        _currentPhase,
        _selectedStoreIds,
        _shoppingStoreId
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val itemsList = args[0] as List<GroceryItem>
        @Suppress("UNCHECKED_CAST")
        val infos = args[1] as List<GroceryItemStoreInfo>
        @Suppress("UNCHECKED_CAST")
        val allStores = args[2] as List<Store>
        val phase = args[3] as GroceryPhase
        @Suppress("UNCHECKED_CAST")
        val selectedStores = args[4] as Set<String>
        val shoppingStore = args[5] as String?

        val activeItems = itemsList.filter { it.isActive }
        when (phase) {
            GroceryPhase.NEED -> activeItems
            GroceryPhase.PLANNING -> {
                if (selectedStores.isEmpty()) {
                    // Show everything in planning by default
                    activeItems
                } else if (selectedStores.contains("-1")) {
                    // "Unassigned" filter: items that have NO store mappings at all
                    activeItems.filter { item ->
                        val itemInfos = infos.filter { it.groceryItemId == item.id }
                        itemInfos.isEmpty()
                    }
                } else {
                    // Filtered view: items explicitly marked as available at ANY of the selected stores
                    // OR items that have no mapping for that store yet (Default to store's support flag)
                    activeItems.filter { item ->
                        val itemInfos = infos.filter { it.groceryItemId == item.id }
                        selectedStores.any { storeId ->
                            val info = itemInfos.find { it.storeId == storeId }
                            val store = allStores.find { it.id == storeId }
                            info?.isAvailable ?: store?.isDefaultSupported ?: true
                        }
                    }
                }
            }
            GroceryPhase.SHOPPING -> {
                if (shoppingStore == null) emptyList()
                else {
                    // High-velocity mode: Show items unless explicitly marked as unavailable
                    val store = allStores.find { it.id == shoppingStore }
                    activeItems.filter { item ->
                        val info = infos.find { it.groceryItemId == item.id && it.storeId == shoppingStore }
                        info?.isAvailable ?: store?.isDefaultSupported ?: true
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered items that belong in the standard categories (excluding items already checked and moved)
    val standardCategoryItems: StateFlow<List<GroceryItem>> = combine(
        baseFilteredItems,
        _currentPhase,
        _recentlyCheckedIds
    ) { baseItems, phase, recentlyChecked ->
        if (phase == GroceryPhase.SHOPPING) {
            baseItems.filter { !it.isBought || recentlyChecked.contains(it.id) }
        } else {
            baseItems
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Checked/completed items that have finished their 2-second delay and reside in "In Cart" category
    val inCartItems: StateFlow<List<GroceryItem>> = combine(
        baseFilteredItems,
        _currentPhase,
        _recentlyCheckedIds
    ) { baseItems, phase, recentlyChecked ->
        if (phase == GroceryPhase.SHOPPING) {
            baseItems.filter { it.isBought && !recentlyChecked.contains(it.id) }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database mutators wrapped in view model scopes
    fun insertItemFromInput(input: String) {
        if (input.isBlank()) return
        val (name, quantity, unit) = parseNaturalLanguage(input)
        
        viewModelScope.launch {
            var catId = _selectedCategoryId.value
            
            // Auto-categorize if no category is selected and AI is ready
            if (catId == null && categorizer.isReady.value) {
                _isCategorizing.value = true
                val catNames = categories.value.map { it.name }
                val suggestedName = categorizer.categorize(name, catNames)
                if (suggestedName != null) {
                    catId = categories.value.find { it.name == suggestedName }?.id
                }
                _isCategorizing.value = false
            }
            
            insertItem(name, quantity, catId, unit)
        }
        _newItemInput.value = ""
    }

    @Suppress("ReturnCount")
    private fun parseNaturalLanguage(input: String): Triple<String, String?, String?> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Triple("", null, null)

        val numberWords = mapOf(
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10"
        )
        
        val commonUnits = setOf(
            "lb", "lbs", "oz", "gram", "g", "kg", "kilogram", "liter", "l", "ml", 
            "cup", "cups", "bottle", "bottles", "can", "cans", "box", "boxes", 
            "pack", "packs", "bag", "bags", "bunch", "bunches", "dozen", "dz", 
            "gallon", "gal", "qt", "quart", "pint", "pt"
        )

        val words = trimmed.split("\\s+".toRegex())
        val firstWord = words[0].lowercase()
        
        val quantity: String?
        val remainingWords: List<String>

        if (firstWord.all { it.isDigit() }) {
            quantity = firstWord
            remainingWords = words.drop(1)
        } else if (numberWords.containsKey(firstWord)) {
            quantity = numberWords[firstWord]!!
            remainingWords = words.drop(1)
        } else {
            // Not a number or number-word, whole input is the name
            return Triple(trimmed, null, null)
        }

        if (remainingWords.isEmpty()) {
            return Triple(trimmed, quantity, null)
        }

        // Check for units
        val firstRemaining = remainingWords[0].lowercase()
        
        // Pattern: "Quantity [Unit] of [Name]"
        if (remainingWords.size > 1 && remainingWords[1].lowercase() == "of") {
            val unit = remainingWords[0]
            val name = remainingWords.drop(2).joinToString(" ")
            if (name.isNotBlank()) {
                return Triple(name, quantity, unit)
            }
        }
        
        // Pattern: "Quantity [KnownUnit] [Name]"
        if (commonUnits.contains(firstRemaining) && remainingWords.size > 1) {
            val unit = remainingWords[0]
            val name = remainingWords.drop(1).joinToString(" ")
            return Triple(name, quantity, unit)
        }

        // Default: everything else after quantity is the name
        return Triple(remainingWords.joinToString(" "), quantity, null)
    }

    fun insertItem(name: String, quantity: String?, categoryId: String?, unit: String? = null) {
        if (name.isNotBlank()) {
            val capitalizedName = formatName(name)
            viewModelScope.launch {
                val existing = items.value.find { 
                    it.name.equals(capitalizedName, ignoreCase = true) 
                }
                
                if (existing != null) {
                    repository.updateItem(existing.copy(
                        isActive = true, 
                        quantity = quantity ?: existing.quantity, 
                        categoryId = categoryId ?: existing.categoryId,
                        isBought = false,
                        unit = unit ?: existing.unit
                    ))
                } else {
                    val item = GroceryItem(
                        name = capitalizedName,
                        quantity = quantity ?: "1",
                        categoryId = categoryId,
                        userId = userId,
                        isActive = true,
                        listId = _selectedListId.value,
                        unit = unit
                    )
                    repository.insertItem(item)

                    // Auto-map to stores that are supported by default
                    stores.value.filter { it.isDefaultSupported }.forEach { store ->
                        repository.insertStoreInfo(
                            GroceryItemStoreInfo(
                                groceryItemId = item.id,
                                storeId = store.id,
                                userId = userId,
                                isAvailable = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun setSelectedListId(listId: String?) {
        _selectedListId.value = listId
        if (listId != null) {
            prefs.edit { putString("selected_list_id", listId) }
        } else {
            prefs.edit { remove("selected_list_id") }
        }
    }
}
