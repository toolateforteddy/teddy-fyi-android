package fyi.teddy.android.grocery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.domain.MoveGroceryItemDownUseCase
import fyi.teddy.android.grocery.domain.MoveGroceryItemUpUseCase
import fyi.teddy.android.grocery.repository.GroceryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class GroceryViewModel(
    private val repository: GroceryRepository,
    val userId: String
) : ViewModel() {

    // Internal mutable state flows for UDF compliance
    private val _currentPhase = MutableStateFlow(GroceryPhase.NEED)
    val currentPhase: StateFlow<GroceryPhase> = _currentPhase.asStateFlow()

    private val _selectedStoreIds = MutableStateFlow(setOf<Int>())
    val selectedStoreIds: StateFlow<Set<Int>> = _selectedStoreIds.asStateFlow()

    private val _shoppingStoreId = MutableStateFlow<Int?>(null)
    val shoppingStoreId: StateFlow<Int?> = _shoppingStoreId.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _showRecommendedDialog = MutableStateFlow(false)
    val showRecommendedDialog: StateFlow<Boolean> = _showRecommendedDialog.asStateFlow()

    private val _newItemName = MutableStateFlow("")
    val newItemName: StateFlow<String> = _newItemName.asStateFlow()

    private val _newItemQuantity = MutableStateFlow("1")
    val newItemQuantity: StateFlow<String> = _newItemQuantity.asStateFlow()

    private val _newItemUnit = MutableStateFlow<String?>(null)
    val newItemUnit: StateFlow<String?> = _newItemUnit.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _recentlyCheckedIds = MutableStateFlow(setOf<Int>())
    val recentlyCheckedIds: StateFlow<Set<Int>> = _recentlyCheckedIds.asStateFlow()

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId: StateFlow<String?> = _selectedListId.asStateFlow()

    // Combined state for modern UDF support
    val state: StateFlow<GroceryUiState> = combine(
        _currentPhase,
        _selectedStoreIds,
        _shoppingStoreId,
        _isEditMode,
        _showRecommendedDialog,
        _newItemName,
        _newItemQuantity,
        _newItemUnit,
        _selectedCategoryId,
        _recentlyCheckedIds,
        _selectedListId
    ) { args ->
        GroceryUiState(
            currentPhase = args[0] as GroceryPhase,
            selectedStoreIds = args[1] as Set<Int>,
            shoppingStoreId = args[2] as Int?,
            isEditMode = args[3] as Boolean,
            showRecommendedDialog = args[4] as Boolean,
            newItemName = args[5] as String,
            newItemQuantity = args[6] as String,
            newItemUnit = args[7] as String?,
            selectedCategoryId = args[8] as Int?,
            recentlyCheckedIds = args[9] as Set<Int>,
            selectedListId = args[10] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GroceryUiState())

    // Instantiate use cases
    private val moveGroceryItemUpUseCase = MoveGroceryItemUpUseCase(repository)
    private val moveGroceryItemDownUseCase = MoveGroceryItemDownUseCase(repository)

    fun onEvent(event: GroceryUiEvent) {
        when (event) {
            is GroceryUiEvent.SetPhase -> setPhase(event.phase)
            is GroceryUiEvent.ToggleStoreSelection -> toggleStoreSelection(event.storeId)
            is GroceryUiEvent.SetShoppingStoreId -> setShoppingStoreId(event.storeId)
            is GroceryUiEvent.SetEditMode -> setEditMode(event.enabled)
            is GroceryUiEvent.SetShowRecommendedDialog -> setShowRecommendedDialog(event.show)
            is GroceryUiEvent.SetNewItemName -> setNewItemName(event.name)
            is GroceryUiEvent.SetNewItemQuantity -> setNewItemQuantity(event.qty)
            is GroceryUiEvent.SetNewItemUnit -> setNewItemUnit(event.unit)
            is GroceryUiEvent.SetSelectedCategoryId -> setSelectedCategoryId(event.categoryId)
            is GroceryUiEvent.SetSelectedListId -> setSelectedListId(event.listId)
            
            // Item Mutators
            is GroceryUiEvent.InsertItem -> insertItem(event.name, event.quantity, event.categoryId, event.unit)
            is GroceryUiEvent.UpdateItem -> updateItem(event.item)
            is GroceryUiEvent.DeleteItem -> deleteItem(event.item)
            is GroceryUiEvent.MoveItemUp -> {
                viewModelScope.launch { moveGroceryItemUpUseCase(event.item, event.siblings) }
            }
            is GroceryUiEvent.MoveItemDown -> {
                viewModelScope.launch { moveGroceryItemDownUseCase(event.item, event.siblings) }
            }
            is GroceryUiEvent.UpdateStoreInfo -> updateStoreInfo(event.info)
            is GroceryUiEvent.ToggleBought -> toggleBought(event.item, event.isChecked)
            is GroceryUiEvent.MarkDoneForTrip -> markDoneForTrip()
            
            // Store Mutators
            is GroceryUiEvent.InsertStore -> insertStore(event.name)
            is GroceryUiEvent.DeleteStore -> deleteStore(event.store)
            is GroceryUiEvent.UpdateStore -> updateStore(event.store)
            is GroceryUiEvent.SwapStorePositions -> swapStorePositions(event.store1, event.store2)
            
            // Category Mutators
            is GroceryUiEvent.InsertCategory -> insertCategory(event.name)
            is GroceryUiEvent.DeleteCategory -> deleteCategory(event.category)
            is GroceryUiEvent.SwapCategoryPositions -> swapCategoryPositions(event.cat1, event.cat2)
            
            // List Mutators
            is GroceryUiEvent.InsertList -> insertList(event.name)
            is GroceryUiEvent.DeleteList -> deleteList(event.list)
            is GroceryUiEvent.UpdateList -> updateList(event.list)
            is GroceryUiEvent.ShareList -> shareListWithUser(event.listId, event.userId)
            is GroceryUiEvent.RemoveListMember -> removeListMember(event.member)
            is GroceryUiEvent.AddRecommendedItems -> addRecommendedItems(event.selectedIds)
        }
    }

    // Sources from repository
    @OptIn(ExperimentalCoroutinesApi::class)
    val items = _selectedListId.flatMapLatest { listId ->
        if (listId == null) {
            repository.getItemsWithoutList(userId)
        } else {
            repository.getItemsForList(listId)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lists = repository.getAllLists(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stores = repository.getAllStores(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.getAllCategories(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeInfos = repository.getAllStoreInfo(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedItems = repository.getRecommendedItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.claimEverything(userId)
        }
    }

    // Sanitize and format name inputs uniformly
    fun formatName(input: String): String {
        return input.trim().split("\\s+".toRegex())
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
    }

    // Setters
    fun setPhase(phase: GroceryPhase) {
        _currentPhase.value = phase
        if (phase != GroceryPhase.SHOPPING) {
            _isEditMode.value = false
        }
    }

    fun toggleStoreSelection(storeId: Int) {
        _selectedStoreIds.update { current ->
            if (current.contains(storeId)) current - storeId else current + storeId
        }
    }

    fun setShoppingStoreId(storeId: Int?) {
        _shoppingStoreId.value = storeId
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

    fun setSelectedCategoryId(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    // Core state flow combining all tables for categories and custom views
    val baseFilteredItems: StateFlow<List<GroceryItem>> = combine(
        items,
        storeInfos,
        _currentPhase,
        _selectedStoreIds,
        _shoppingStoreId
    ) { all, infos, phase, selectedStores, shoppingStore ->
        val activeItems = all.filter { it.isActive }
        when (phase) {
            GroceryPhase.NEED -> activeItems
            GroceryPhase.PLANNING -> {
                if (selectedStores.isEmpty()) activeItems
                else {
                    activeItems.filter { item ->
                        val itemInfos = infos.filter { it.groceryItemId == item.id }
                        selectedStores.any { storeId ->
                            val info = itemInfos.find { it.storeId == storeId }
                            info?.isAvailable ?: true
                        }
                    }
                }
            }
            GroceryPhase.SHOPPING -> {
                if (shoppingStore == null) emptyList()
                else {
                    activeItems.filter { item ->
                        val info = infos.find { it.groceryItemId == item.id && it.storeId == shoppingStore }
                        info?.isAvailable ?: true
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
    fun insertItem(name: String, quantity: String, categoryId: Int?, unit: String? = null) {
        if (name.isNotBlank()) {
            val capitalizedName = formatName(name)
            viewModelScope.launch {
                val item = GroceryItem(
                    name = capitalizedName,
                    quantity = quantity,
                    categoryId = categoryId,
                    userId = userId,
                    isActive = true,
                    listId = _selectedListId.value,
                    unit = unit
                )
                val existingInactive = items.value.find { 
                    it.name.equals(capitalizedName, ignoreCase = true) && !it.isActive 
                }
                val itemId = if (existingInactive != null) {
                    repository.updateItem(existingInactive.copy(
                        isActive = true, 
                        quantity = quantity, 
                        categoryId = categoryId,
                        isBought = false,
                        unit = unit
                    ))
                    existingInactive.id
                } else {
                    repository.insertItem(item).toInt()
                }

                stores.value.forEach { store ->
                    if (!store.isDefaultSupported) {
                        repository.insertStoreInfo(
                            GroceryItemStoreInfo(
                                groceryItemId = itemId,
                                storeId = store.id,
                                isAvailable = false,
                                userId = userId
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateItem(item: GroceryItem) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun swapItemPositions(item1: GroceryItem, item2: GroceryItem) {
        viewModelScope.launch { repository.swapItemPositions(item1, item2) }
    }

    fun updateStoreInfo(info: GroceryItemStoreInfo) {
        viewModelScope.launch { repository.insertStoreInfo(info.copy(userId = userId)) }
    }

    fun toggleBought(item: GroceryItem, isChecked: Boolean) {
        val updatedItem = item.copy(isBought = isChecked)
        
        if (isChecked && _currentPhase.value == GroceryPhase.SHOPPING) {
            // Immediately mark as checked, and add to recentlyCheckedIds to start 2-second transition
            _recentlyCheckedIds.update { it + item.id }
            updateItem(updatedItem)
            
            viewModelScope.launch {
                delay(2000)
                _recentlyCheckedIds.update { it - item.id }
            }
        } else {
            _recentlyCheckedIds.update { it - item.id }
            updateItem(updatedItem)
        }
    }

    fun markDoneForTrip() {
        viewModelScope.launch {
            repository.markDoneForTrip(userId)
        }
    }

    // Store operations
    fun insertStore(name: String) {
        if (name.isNotBlank()) {
            val capitalized = formatName(name)
            viewModelScope.launch {
                repository.insertStore(Store(name = capitalized, userId = userId))
            }
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch { repository.deleteStore(store) }
    }

    fun updateStore(store: Store) {
        viewModelScope.launch { repository.updateStore(store) }
    }

    fun swapStorePositions(store1: Store, store2: Store) {
        viewModelScope.launch { repository.swapStorePositions(store1, store2) }
    }

    // Category operations
    fun insertCategory(name: String) {
        if (name.isNotBlank()) {
            val capitalized = formatName(name)
            viewModelScope.launch {
                repository.insertCategory(Category(name = capitalized, userId = userId))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun swapCategoryPositions(cat1: Category, cat2: Category) {
        viewModelScope.launch { repository.swapCategoryPositions(cat1, cat2) }
    }

    fun addRecommendedItems(selectedItemIds: List<Int>) {
        viewModelScope.launch {
            recommendedItems.value.filter { selectedItemIds.contains(it.id) }.forEach { item ->
                repository.updateItem(item.copy(isBought = false, isActive = true))
            }
        }
    }

    // List and Collaboration operations
    fun setSelectedListId(listId: String?) {
        _selectedListId.value = listId
    }

    fun insertList(name: String) {
        if (name.isNotBlank()) {
            val capitalized = formatName(name)
            viewModelScope.launch {
                repository.insertList(
                    GroceryList(
                        name = capitalized,
                        ownerId = userId
                    )
                )
            }
        }
    }

    fun deleteList(list: GroceryList) {
        viewModelScope.launch {
            repository.deleteList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
            }
        }
    }

    fun updateList(list: GroceryList) {
        viewModelScope.launch {
            repository.updateList(list)
        }
    }

    fun shareListWithUser(listId: String, memberUserId: String) {
        if (memberUserId.isNotBlank()) {
            viewModelScope.launch {
                repository.insertListMember(
                    GroceryListMember(
                        listId = listId,
                        userId = memberUserId
                    )
                )
            }
        }
    }

    fun removeListMember(member: GroceryListMember) {
        viewModelScope.launch {
            repository.deleteListMember(member)
        }
    }

    fun getListMembers(listId: String) = repository.getListMembers(listId)
}
