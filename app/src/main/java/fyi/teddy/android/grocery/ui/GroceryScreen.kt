package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.grocery.ui.components.AddListDialog
import fyi.teddy.android.grocery.ui.components.RenameListDialog
import fyi.teddy.android.grocery.ui.components.JoinListDialog
import fyi.teddy.android.grocery.ui.components.NeedPhaseContent
import fyi.teddy.android.grocery.ui.components.PlanningPhaseContent
import fyi.teddy.android.grocery.ui.components.RecommendedItemsDialog
import fyi.teddy.android.grocery.ui.components.ShareListDialog
import fyi.teddy.android.grocery.ui.components.ShoppingPhaseContent
import java.util.*

enum class GroceryPhase {
    NEED, PLANNING, SHOPPING;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroceryScreen(
    userId: String, 
    onBack: () -> Unit, 
    onManageConfig: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GroceryViewModel = viewModel(
        factory = GroceryViewModelFactory(context.applicationContext as android.app.Application, userId),
    )
    
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.message)
            viewModel.onEvent(GroceryUiEvent.DismissSnackbar(msg.id))
        }
    }
    
    val items by viewModel.items.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val storeInfos by viewModel.storeInfos.collectAsState()
    val recommendedItems by viewModel.recommendedItems.collectAsState()

    val standardCategoryItems by viewModel.standardCategoryItems.collectAsState()
    val inCartItems by viewModel.inCartItems.collectAsState()
    
    val lists by viewModel.lists.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    var showAddItemSheet by remember { mutableStateOf(false) }
    
    var showListSelectorMenu by remember { mutableStateOf(value = false) }
    var showAddListDialog by remember { mutableStateOf(value = false) }
    var showRenameListDialog by remember { mutableStateOf(value = false) }
    var showJoinListDialog by remember { mutableStateOf(value = false) }
    var showShareListDialog by remember { mutableStateOf(value = false) }
    
    val nameFocusRequester = remember { FocusRequester() }

    val uniqueNames = remember(items) {
        items.asSequence().map { it.name }.distinct().sorted().toList()
    }
    
    val suggestions = remember(state.newItemInput, uniqueNames) {
        if (state.newItemInput.length < 2) emptyList()
        else uniqueNames.filter {
            it.contains(state.newItemInput, ignoreCase = true) && !it.equals(
                state.newItemInput,
                ignoreCase = true
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isError = state.snackbarMessage?.isError == true
                Snackbar(
                    containerColor = if (isError) Color(0xFFD32F2F) else Color(0xFF388E3C),
                    contentColor = Color.White,
                    snackbarData = data
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    val activeList = lists.find { it.id == state.selectedListId }
                    val activeListName = activeList?.name ?: "Default List"

                    if (state.isEditMode) Text("Grocery: ${state.currentPhase.displayName}")
                    else Text("Grocery: ${state.currentPhase.displayName}: $activeListName")
                        },
                actions = {
                    if (state.currentPhase == GroceryPhase.NEED) {
                        val syncIconColor = when (state.lastSyncStatus) {
                            "FAILURE", "RETRY" -> Color.Red
                            else -> if (state.unsyncedCount > 0) Color.Yellow else Color.White
                        }
                        
                        val transition = rememberInfiniteTransition(label = "syncRotation")
                        val rotation by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        IconButton(onClick = {
                            if (state.lastSyncStatus == "FAILURE" || state.lastSyncStatus == "RETRY") {
                                onNavigateToDebug()
                            } else {
                                fyi.teddy.android.network.SyncWorker.enqueue(context)
                            }
                        }) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync Data",
                                tint = syncIconColor,
                                modifier = if (state.isSyncing) Modifier.rotate(rotation) else Modifier
                            )
                        }
                    }
                    if (state.isEditMode) {
                        IconButton(onClick = onManageConfig) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    if (state.currentPhase != GroceryPhase.SHOPPING) {
                        IconButton(onClick = { viewModel.onEvent(GroceryUiEvent.SetEditMode(!state.isEditMode)) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (state.isEditMode) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }

                    if (state.currentPhase == GroceryPhase.SHOPPING) {
                        var showConfirmTripDone by remember { mutableStateOf(false) }
                        IconButton(onClick = { showConfirmTripDone = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Trip Complete")
                        }
                        if (showConfirmTripDone) {
                            AlertDialog(
                                onDismissRequest = { showConfirmTripDone = false },
                                title = { Text("Complete Trip?") },
                                text = { Text("Are you sure you want to mark all In Cart items as done and move them to history?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.onEvent(GroceryUiEvent.MarkDoneForTrip)
                                        showConfirmTripDone = false
                                    }) { Text("Confirm") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmTripDone = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (state.currentPhase == GroceryPhase.NEED) {
                FloatingActionButton(
                    onClick = { showAddItemSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Black) {
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.NEED,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.NEED)) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Need") },
                    label = { Text("Need") }
                )
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.PLANNING,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.PLANNING)) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Planning") },
                    label = { Text("Planning") }
                )
                NavigationBarItem(
                    selected = state.currentPhase == GroceryPhase.SHOPPING,
                    onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(GroceryPhase.SHOPPING)) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping") },
                    label = { Text("Shopping") }
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // List / Space selector Row for Shared Lists
                val activeList = lists.find { it.id == state.selectedListId }
                val activeListName = activeList?.name ?: "Default List"

                if (state.isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { showListSelectorMenu = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Lists",
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeListName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch List",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showListSelectorMenu,
                                onDismissRequest = { showListSelectorMenu = false }
                            ) {
                                if (state.hasItemsInDefaultList) {
                                    DropdownMenuItem(
                                        text = { Text("Default List") },
                                        onClick = {
                                            viewModel.onEvent(GroceryUiEvent.SetSelectedListId(null))
                                            showListSelectorMenu = false
                                        }
                                    )
                                }
                                lists.forEach { list ->
                                    DropdownMenuItem(
                                        text = { Text(list.name) },
                                        onClick = {
                                            viewModel.onEvent(GroceryUiEvent.SetSelectedListId(list.id))
                                            showListSelectorMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showJoinListDialog = true }) {
                                    Icon(
                                        Icons.Default.GroupAdd,
                                        contentDescription = "Join List",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { showAddListDialog = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "New List",
                                        tint = Color.White
                                    )
                                }
                                if (state.selectedListId != null) {
                                    IconButton(onClick = { showRenameListDialog = true }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Rename List",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(onClick = { showShareListDialog = true }) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share List",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.onEvent(GroceryUiEvent.DeleteList(activeList!!))
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete List",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }

                    }
                }

                // Add List Dialog
                if (showAddListDialog) {
                    AddListDialog(
                        onDismiss = { showAddListDialog = false },
                        onConfirm = { name ->
                            viewModel.onEvent(GroceryUiEvent.InsertList(name))
                            showAddListDialog = false
                        }
                    )
                }

                // Rename List Dialog
                if (showRenameListDialog && activeList != null) {
                    RenameListDialog(
                        initialName = activeList.name,
                        onDismiss = { showRenameListDialog = false },
                        onConfirm = { newName ->
                            viewModel.onEvent(GroceryUiEvent.UpdateList(activeList.copy(name = newName)))
                            showRenameListDialog = false
                        }
                    )
                }

                // Join List Dialog
                if (showJoinListDialog) {
                    JoinListDialog(
                        onDismiss = { showJoinListDialog = false },
                        onJoin = { code ->
                            viewModel.onEvent(GroceryUiEvent.JoinList(code))
                        }
                    )
                }

                // Share List Dialog
                if (showShareListDialog && (state.selectedListId != null)) {
                    ShareListDialog(
                        listName = activeListName,
                        membersFlow = remember(state.selectedListId) { viewModel.getListMembers(state.selectedListId!!) },
                        activeInviteCode = state.activeInviteCode,
                        onDismiss = { showShareListDialog = false },
                        onCreateInvite = {
                            viewModel.onEvent(GroceryUiEvent.CreateInvite(state.selectedListId!!))
                        },
                        onRemoveMember = { member ->
                            viewModel.onEvent(GroceryUiEvent.RemoveListMember(member))
                        }
                    )
                }

//                Spacer(modifier = Modifier.height(8.dp))

                when (state.currentPhase) {
                    GroceryPhase.NEED -> {
                        NeedPhaseContent(
                            items = standardCategoryItems,
                            categories = categories,
                            stores = stores,
                            storeInfos = storeInfos,
                            onEvent = viewModel::onEvent
                        )
                    }
                    GroceryPhase.PLANNING -> {
                        PlanningPhaseContent(
                            state = state,
                            items = items.filter { it.isActive },
                            stores = stores,
                            storeInfos = storeInfos,
                            recommendedItems = recommendedItems,
                            categories = categories,
                            onEvent = viewModel::onEvent
                        )
                    }
                    GroceryPhase.SHOPPING -> {
                        ShoppingPhaseContent(
                            state = state,
                            items = standardCategoryItems,
                            inCartItems = inCartItems,
                            stores = stores,
                            categories = categories,
                            onEvent = viewModel::onEvent
                        )
                    }
                }
            }
        }

        if (showAddItemSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddItemSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1A1A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Add New Item",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TextField(
                        value = state.newItemInput,
                        onValueChange = { viewModel.onEvent(GroceryUiEvent.SetNewItemInput(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester),
                        placeholder = { Text("e.g. 2 bunches of Bananas", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.onEvent(GroceryUiEvent.InsertItemFromInput(state.newItemInput))
                                showAddItemSheet = false
                            }
                        )
                    )

                    if (suggestions.isNotEmpty()) {
                        Text(
                            "Suggestions",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestions) { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.onEvent(GroceryUiEvent.SetNewItemInput(suggestion))
                                    },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
                    }
                    
                    if (state.isAiReady) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFBB86FC),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "AI Smart Categorization Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBB86FC)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.onEvent(GroceryUiEvent.InsertItemFromInput(state.newItemInput))
                            showAddItemSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Text("Add to List")
                    }
                }
            }
        }
        
        if (state.showRecommendedDialog) {
            RecommendedItemsDialog(
                recommendedItems = recommendedItems,
                activeItems = items,
                onDismiss = { viewModel.onEvent(GroceryUiEvent.SetShowRecommendedDialog(false)) },
                onAddItems = { selectedIds ->
                    viewModel.onEvent(GroceryUiEvent.AddRecommendedItems(selectedIds))
                }
            )
        }
    }
}

