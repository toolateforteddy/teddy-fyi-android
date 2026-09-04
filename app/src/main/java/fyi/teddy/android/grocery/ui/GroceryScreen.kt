package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.grocery.ui.components.AddItemForm
import fyi.teddy.android.grocery.ui.components.AddListDialog
import fyi.teddy.android.grocery.ui.components.GrocerySpaceOption
import fyi.teddy.android.grocery.ui.components.GrocerySpaceRailSection
import fyi.teddy.android.grocery.ui.components.GrocerySpaceSwitcherTitle
import fyi.teddy.android.grocery.ui.components.GroceryRailEntry
import fyi.teddy.android.grocery.ui.components.grocerySpaceOptions
import fyi.teddy.android.grocery.ui.components.nameFor
import fyi.teddy.android.grocery.ui.components.RenameListDialog
import fyi.teddy.android.grocery.ui.components.JoinListDialog
import fyi.teddy.android.grocery.ui.components.DOCKED_ADD_PANE_WIDTH
import fyi.teddy.android.grocery.ui.components.DockedAddItemPane
import fyi.teddy.android.grocery.ui.components.NeedPhaseContent
import fyi.teddy.android.grocery.ui.components.PlanningPhaseContent
import fyi.teddy.android.grocery.ui.components.RecommendedItemsDialog
import fyi.teddy.android.grocery.ui.components.ReorderGrocerySpacesDialog
import fyi.teddy.android.grocery.ui.components.ShareListDialog
import fyi.teddy.android.grocery.ui.components.ShoppingPhaseContent
import fyi.teddy.android.grocery.ui.components.shouldDockAddItemPane
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration.Companion.minutes

/** How many just-added names the rapid-entry field keeps on screen as a receipt. */
private const val MAX_RAPID_ENTRY_RECEIPTS = 8

enum class GroceryPhase {
    NEED, PLANNING, SHOPPING;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    /** Shared by the phone NavigationBar and the tablet NavigationRail so they cannot drift. */
    val icon: ImageVector
        get() = when (this) {
            NEED -> Icons.AutoMirrored.Filled.List
            PLANNING -> Icons.Default.DateRange
            SHOPPING -> Icons.Default.ShoppingCart
        }
}

/** Width at or above which the layout stops being a phone layout (Material compact/medium boundary). */
private const val MEDIUM_WIDTH_BREAKPOINT_DP = 600

/**
 * The rail is wider than a Material NavigationRail's 80dp because it carries list names, not
 * just three icons. Wide enough for a name like "Costco run", narrow enough that a 600dp
 * portrait tablet still keeps two thirds of its width for the list itself.
 */
private val RAIL_WIDTH = 180.dp

/**
 * Entry point for the Grocery app. Applies [GroceryTheme] so every Grocery screen and
 * dialog renders with the Grocery palette regardless of what theme the host shell is using.
 */
@Composable
fun GroceryScreen(
    userId: String,
    onBack: () -> Unit,
    onManageConfig: () -> Unit,
    onNavigateToDebug: () -> Unit,
) = GroceryTheme {
    GroceryScreenContent(
        userId = userId,
        onBack = onBack,
        onManageConfig = onManageConfig,
        onNavigateToDebug = onNavigateToDebug,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GroceryScreenContent(
    userId: String, 
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit, 
    onManageConfig: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val groceryColors = GroceryTheme.colors
    val context = LocalContext.current
    val viewModel: GroceryViewModel = viewModel(
        factory = GroceryViewModelFactory(context.applicationContext as android.app.Application, userId),
    )
    
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel,
                duration = if (msg.action != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.action?.let { viewModel.onEvent(it) }
            }
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
    
    // Periodic sync every 5 minutes while Shopping tab is open
    LaunchedEffect(state.currentPhase) {
        if (state.currentPhase == GroceryPhase.SHOPPING) {
            // Immediate sync when entering Shopping phase
            fyi.teddy.android.network.SyncWorker.enqueue(context)
            while (true) {
                delay(5.minutes)
                fyi.teddy.android.network.SyncWorker.enqueue(context)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showAddItemSheet by remember { mutableStateOf(false) }
    
    var showAddListDialog by remember { mutableStateOf(value = false) }
    var showRenameListDialog by remember { mutableStateOf(value = false) }
    var showJoinListDialog by remember { mutableStateOf(value = false) }
    var showShareListDialog by remember { mutableStateOf(value = false) }
    var showReorderSpacesModal by remember { mutableStateOf(value = false) }
    
    val nameFocusRequester = remember { FocusRequester() }

    // Names filed since entry began, newest first, so a long entry run shows its own progress
    // without the list behind the sheet having to be visible.
    var addedThisSession by remember { mutableStateOf(emptyList<String>()) }

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

    // Material's compact/medium breakpoint. At medium and up the phases and the list switcher
    // both move to a rail, which hands ~80dp of height back to the list and puts them under
    // the thumb of whichever hand is holding the tablet's left bezel.
    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= MEDIUM_WIDTH_BREAKPOINT_DP

    val spaceOptions = remember(lists, state.hasItemsInDefaultList, state.selectedListId) {
        grocerySpaceOptions(lists, state.hasItemsInDefaultList, state.selectedListId)
    }

    // Wider still, the modal sheet is a full-width slab and the soft keyboard buries the very
    // list being added to, so entry docks beside the list instead of on top of it.
    val useDockedAddPane = shouldDockAddItemPane(LocalConfiguration.current.screenWidthDp)
    val showDockedAddPane = useDockedAddPane && state.currentPhase == GroceryPhase.NEED

    // Rotating into the docked layout should not leave a stale sheet queued up behind it.
    LaunchedEffect(useDockedAddPane) {
        if (useDockedAddPane) showAddItemSheet = false
    }

    // Rapid entry: each submit files the item, clears the field and hands focus straight back,
    // with the running tally acting as the receipt. Shared by the sheet and the docked pane.
    val submitItem: () -> Unit = {
        val entry = state.newItemInput.trim()
        if (entry.isNotEmpty()) {
            viewModel.onEvent(GroceryUiEvent.InsertItemFromInput(entry))
            addedThisSession = (listOf(entry) + addedThisSession).take(MAX_RAPID_ENTRY_RECEIPTS)
        }
        nameFocusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isError = state.snackbarMessage?.isError == true
                Snackbar(
                    containerColor = if (isError) groceryColors.danger else groceryColors.success,
                    contentColor = groceryColors.onStatus,
                    actionColor = groceryColors.onStatus,
                    snackbarData = data
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    // On a phone the title is the switcher, so lists can be changed without
                    // entering edit mode. On rail widths the rail carries the spaces instead.
                    if (useNavigationRail) {
                        Text("Grocery: ${state.currentPhase.displayName}")
                    } else {
                        GrocerySpaceSwitcherTitle(
                            phaseLabel = state.currentPhase.displayName,
                            options = spaceOptions,
                            selectedListId = state.selectedListId,
                            onSelect = { viewModel.onEvent(GroceryUiEvent.SetSelectedListId(it)) }
                        )
                    }
                },
                actions = {
                    if (state.currentPhase == GroceryPhase.NEED || state.currentPhase == GroceryPhase.SHOPPING) {
                        val syncIconColor = when (state.lastSyncStatus) {
                            "FAILURE", "RETRY" -> groceryColors.danger
                            else -> if (state.unsyncedCount > 0) groceryColors.price else groceryColors.onSurface
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
                        IconButton(onClick = { showReorderSpacesModal = true }) {
                            Icon(
                                Icons.Default.FormatLineSpacing,
                                contentDescription = "Reorder Spaces",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onManageConfig) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    if (state.currentPhase != GroceryPhase.SHOPPING) {
                        IconButton(onClick = { viewModel.onEvent(GroceryUiEvent.SetEditMode(!state.isEditMode)) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.edit_mode),
                                tint = if (state.isEditMode) MaterialTheme.colorScheme.primary else groceryColors.onSurface
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
                                title = { Text("Call it a trip?") },
                                text = { Text("Everything in your cart gets checked off and filed away.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.onEvent(GroceryUiEvent.MarkDoneForTrip)
                                        showConfirmTripDone = false
                                    }) { Text("That's a wrap") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmTripDone = false }) { Text("Not yet") }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = groceryColors.screen,
                    titleContentColor = groceryColors.onSurface,
                    actionIconContentColor = groceryColors.onSurface
                )
            )
        },
        floatingActionButton = {
            if (state.currentPhase == GroceryPhase.NEED && !useDockedAddPane) {
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
            // On a tablet the rail down the left already carries the three phases, and the
            // vertical axis is the scarce one — so the bar only exists on phone widths.
            if (!useNavigationRail) {
                NavigationBar(containerColor = groceryColors.screen) {
                    GroceryPhase.entries.forEach { phase ->
                        NavigationBarItem(
                            selected = state.currentPhase == phase,
                            onClick = { viewModel.onEvent(GroceryUiEvent.SetPhase(phase)) },
                            icon = { Icon(phase.icon, contentDescription = phase.displayName) },
                            label = { Text(phase.displayName) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (useNavigationRail) {
                GroceryRail(
                    currentPhase = state.currentPhase,
                    onSelectPhase = { viewModel.onEvent(GroceryUiEvent.SetPhase(it)) },
                    spaceOptions = spaceOptions,
                    selectedListId = state.selectedListId,
                    onSelectSpace = { viewModel.onEvent(GroceryUiEvent.SetSelectedListId(it)) }
                )
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = groceryColors.screen
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    // Edit mode is only the list *management* actions now: switching between
                    // spaces lives in the top bar (phone) or the rail (tablet).
                    val activeList = lists.find { it.id == state.selectedListId }
                    val activeListName = spaceOptions.nameFor(state.selectedListId)

                    if (state.isEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Editing $activeListName",
                                color = groceryColors.onSurfaceMuted,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showJoinListDialog = true }) {
                                        Icon(
                                            Icons.Default.GroupAdd,
                                            contentDescription = "Join List",
                                            tint = groceryColors.onSurface
                                        )
                                    }
                                    IconButton(onClick = { showAddListDialog = true }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "New List",
                                            tint = groceryColors.onSurface
                                        )
                                    }
                                    if (state.selectedListId != null) {
                                        IconButton(onClick = { showRenameListDialog = true }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Rename List",
                                                tint = groceryColors.onSurface
                                            )
                                        }
                                        IconButton(onClick = { showShareListDialog = true }) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share List",
                                                tint = groceryColors.onSurface
                                            )
                                        }
                                        IconButton(onClick = {
                                            viewModel.onEvent(GroceryUiEvent.DeleteList(activeList!!))
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete List",
                                                tint = groceryColors.danger
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

                    if (showReorderSpacesModal) {
                        ReorderGrocerySpacesDialog(
                            spaces = lists,
                            onDismiss = { showReorderSpacesModal = false },
                            onSave = { reordered ->
                                viewModel.onEvent(GroceryUiEvent.ReorderLists(reordered))
                                showReorderSpacesModal = false
                            }
                        )
                    }

    //                Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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

                        if (showDockedAddPane) {
                            // The pane's own receipt starts fresh each time the Need phase opens.
                            LaunchedEffect(Unit) { addedThisSession = emptyList() }

                            Spacer(modifier = Modifier.width(16.dp))
                            DockedAddItemPane(
                                input = state.newItemInput,
                                suggestions = suggestions,
                                isAiReady = state.isAiReady,
                                addedThisSession = addedThisSession,
                                focusRequester = nameFocusRequester,
                                onInputChange = { viewModel.onEvent(GroceryUiEvent.SetNewItemInput(it)) },
                                onSubmit = submitItem,
                                modifier = Modifier
                                    .width(DOCKED_ADD_PANE_WIDTH)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        if (showAddItemSheet && !useDockedAddPane) {
            // Rapid entry: the sheet stays open so a whole week's list can be typed in one
            // sitting.
            LaunchedEffect(showAddItemSheet) { addedThisSession = emptyList() }

            ModalBottomSheet(
                onDismissRequest = { showAddItemSheet = false },
                sheetState = sheetState,
                containerColor = groceryColors.card
            ) {
                AddItemForm(
                    input = state.newItemInput,
                    suggestions = suggestions,
                    isAiReady = state.isAiReady,
                    addedThisSession = addedThisSession,
                    focusRequester = nameFocusRequester,
                    onInputChange = { viewModel.onEvent(GroceryUiEvent.SetNewItemInput(it)) },
                    onSubmit = submitItem,
                    onClose = { showAddItemSheet = false },
                    autoFocusOnAppear = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                )
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

/**
 * The left-hand rail: Need / Planning / Shopping, then every grocery space, at widths where a
 * bottom bar would be spending height the lists need more than the navigation does.
 *
 * This is a plain Column rather than a Material NavigationRail because the spaces belong here
 * too, and a NavigationRail is 80dp wide — enough for three icons, not for "weeknight list".
 * Phases and spaces share one row style so the rail reads as a single column of destinations.
 */
@Composable
private fun GroceryRail(
    currentPhase: GroceryPhase,
    onSelectPhase: (GroceryPhase) -> Unit,
    spaceOptions: List<GrocerySpaceOption>,
    selectedListId: String?,
    onSelectSpace: (String?) -> Unit,
) {
    val groceryColors = GroceryTheme.colors
    Surface(
        modifier = Modifier.width(RAIL_WIDTH).fillMaxHeight(),
        color = groceryColors.screen
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp)) {
            GroceryPhase.entries.forEach { phase ->
                val selected = currentPhase == phase
                GroceryRailEntry(
                    label = phase.displayName,
                    selected = selected,
                    onClick = { onSelectPhase(phase) },
                    icon = {
                        Icon(
                            phase.icon,
                            contentDescription = null,
                            tint = if (selected) groceryColors.accentBright else groceryColors.onSurfaceMuted
                        )
                    }
                )
            }

            HorizontalDivider(
                color = groceryColors.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            GrocerySpaceRailSection(
                options = spaceOptions,
                selectedListId = selectedListId,
                onSelect = onSelectSpace,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}
