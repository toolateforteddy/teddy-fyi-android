package fyi.teddy.android.grocery.ui

import android.app.Application
import android.content.Context
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
    val userId: String,
    private val application: Application
) : ViewModel() {

    private val prefs = application.getSharedPreferences("grocery_prefs", Context.MODE_PRIVATE)

    // Internal mutable state flows for UDF compliance
    private val _currentPhase = MutableStateFlow(GroceryPhase.NEED)
    private val _selectedStoreIds = MutableStateFlow(setOf<Int>())
    private val _shoppingStoreId = MutableStateFlow<Int?>(
        prefs.getInt("last_shopping_store_id", -1).takeIf { it != -1 }
    )
    private val _isEditMode = MutableStateFlow(false)
    private val _showRecommendedDialog = MutableStateFlow(false)
    private val _newItemName = MutableStateFlow("")
    private val _newItemQuantity = MutableStateFlow("1")
    private val _newItemUnit = MutableStateFlow<String?>(null)
    private val _newItemInput = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    private val _recentlyCheckedIds = MutableStateFlow(setOf<Int>())
    private val _selectedListId = MutableStateFlow<String?>(null)

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
        _newItemInput,
        _selectedCategoryId,
        _recentlyCheckedIds,
        _selectedListId
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        GroceryUiState(
            currentPhase = args[0] as GroceryPhase,
            selectedStoreIds = args[1] as Set<Int>,
            shoppingStoreId = args[2] as Int?,
            isEditMode = args[3] as Boolean,
            showRecommendedDialog = args[4] as Boolean,
            newItemName = args[5] as String,
            newItemQuantity = args[6] as String,
            newItemUnit = args[7] as String?,
            newItemInput = args[8] as String,
            selectedCategoryId = args[9] as Int?,
            recentlyCheckedIds = args[10] as Set<Int>,
            selectedListId = args[11] as String?
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
            is GroceryUiEvent.SetNewItemInput -> setNewItemInput(event.input)
            is GroceryUiEvent.SetSelectedCategoryId -> setSelectedCategoryId(event.categoryId)
            is GroceryUiEvent.SetSelectedListId -> setSelectedListId(event.listId)
            is GroceryUiEvent.InsertItemFromInput -> insertItemFromInput(event.input)
            
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
            is GroceryUiEvent.DeleteStoreInfo -> deleteStoreInfo(event.info)
            is GroceryUiEvent.ToggleBought -> toggleBought(event.item, event.isChecked)
            is GroceryUiEvent.MarkDoneForTrip -> markDoneForTrip()
            
            // Store Mutators
            is GroceryUiEvent.InsertStore -> insertStore(event.name)
            is GroceryUiEvent.DeleteStore -> deleteStore(event.store)
            is GroceryUiEvent.UpdateStore -> updateStore(event.store)
            is GroceryUiEvent.SwapStorePositions -> swapStorePositions(event.store1, event.store2)
            
            // Category Mutators
            is GroceryUiEvent.InsertCategory -> insertCategory(event.name)
            is GroceryUiEvent.UpdateCategory -> updateCategory(event.category)
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
        if (storeId != null) {
            prefs.edit().putInt("last_shopping_store_id", storeId).apply()
        } else {
            prefs.edit().remove("last_shopping_store_id").apply()
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

    fun setSelectedCategoryId(categoryId: Int?) {
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
        val itemsList = args[0] as List<GroceryItem>
        val infos = args[1] as List<GroceryItemStoreInfo>
        val allStores = args[2] as List<Store>
        val phase = args[3] as GroceryPhase
        val selectedStores = args[4] as Set<Int>
        val shoppingStore = args[5] as Int?

        val activeItems = itemsList.filter { it.isActive }
        when (phase) {
            GroceryPhase.NEED -> activeItems
            GroceryPhase.PLANNING -> {
                if (selectedStores.isEmpty()) {
                    // Show everything in planning by default
                    activeItems
                } else if (selectedStores.contains(-1)) {
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
        insertItem(name, quantity, _selectedCategoryId.value, unit)
        _newItemInput.value = ""
    }

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

    fun insertItem(name: String, quantity: String?, categoryId: Int?, unit: String? = null) {
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
                    repository.insertItem(item).toInt()
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

    fun updateStoreInfo(info: GroceryItemStoreInfo) {
        viewModelScope.launch { repository.insertStoreInfo(info.copy(userId = userId)) }
    }

    fun deleteStoreInfo(info: GroceryItemStoreInfo) {
        viewModelScope.launch { repository.deleteStoreInfo(info) }
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
            setShoppingStoreId(null)
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

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
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
