package fyi.teddy.android.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.data.SyncLog
import fyi.teddy.android.repository.TeddyRepository
import fyi.teddy.android.network.SyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    idToken: String?,
    onNavigateToAuthed: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    
    // Live reactive count of unsynced items + lists
    val unsyncedTodoCount by db.todoDao().getUnsyncedCountFlow().collectAsState(initial = 0)
    val unsyncedGroceryCount by db.groceryDao().getUnsyncedCountFlow().collectAsState(initial = 0)
    val unsyncedCount = unsyncedTodoCount + unsyncedGroceryCount

    // Table Counts
    val todoItemsCount by db.todoDao().getTodoItemsCountFlow().collectAsState(initial = 0)
    val todoListsCount by db.todoDao().getTodoListsCountFlow().collectAsState(initial = 0)
    val groceryItemsCount by db.groceryDao().getGroceryItemsCountFlow().collectAsState(initial = 0)
    val groceryListsCount by db.groceryDao().getGroceryListsCountFlow().collectAsState(initial = 0)
    val storesCount by db.groceryDao().getStoresCountFlow().collectAsState(initial = 0)
    val categoriesCount by db.groceryDao().getCategoriesCountFlow().collectAsState(initial = 0)
    val storeInfosCount by db.groceryDao().getStoreInfosCountFlow().collectAsState(initial = 0)

    // Unsynced per-table counts
    val unsyncedTodoItemsCount by db.todoDao().getUnsyncedItemsCountFlow().collectAsState(initial = 0)
    val unsyncedTodoListsCount by db.todoDao().getUnsyncedListsCountFlow().collectAsState(initial = 0)
    val unsyncedGroceryItemsCount by db.groceryDao().getUnsyncedItemsCountFlow().collectAsState(initial = 0)
    val unsyncedGroceryListsCount by db.groceryDao().getUnsyncedListsCountFlow().collectAsState(initial = 0)
    val unsyncedStoresCount by db.groceryDao().getUnsyncedStoresCountFlow().collectAsState(initial = 0)
    val unsyncedCategoriesCount by db.groceryDao().getUnsyncedCategoriesCountFlow().collectAsState(initial = 0)
    val unsyncedStoreInfosCount by db.groceryDao().getUnsyncedStoreInfosCountFlow().collectAsState(initial = 0)
    val unsyncedMembersCount by db.groceryDao().getUnsyncedMembersCountFlow().collectAsState(initial = 0)

    // Detailed Pending Changes
    val pendingTodoItems by db.todoDao().getUnsyncedItemsFlow().collectAsState(initial = emptyList())
    val pendingTodoLists by db.todoDao().getUnsyncedListsFlow().collectAsState(initial = emptyList())
    val pendingGroceryItems by db.groceryDao().getUnsyncedItemsFlow().collectAsState(initial = emptyList())
    val pendingGroceryLists by db.groceryDao().getUnsyncedListsFlow().collectAsState(initial = emptyList())
    val pendingStores by db.groceryDao().getUnsyncedStoresFlow().collectAsState(initial = emptyList())
    val pendingCategories by db.groceryDao().getUnsyncedCategoriesFlow().collectAsState(initial = emptyList())
    val pendingMembers by db.groceryDao().getUnsyncedListMembersFlow().collectAsState(initial = emptyList())
    val pendingStoreInfos by db.groceryDao().getUnsyncedStoreInfosFlow().collectAsState(initial = emptyList())

    val allPendingChanges = remember(
        pendingTodoItems, pendingTodoLists, pendingGroceryItems, pendingGroceryLists,
        pendingStores, pendingCategories, pendingMembers, pendingStoreInfos
    ) {
        val list = mutableListOf<PendingChange>()
        
        pendingTodoItems.forEach { item ->
            list.add(PendingChange(item.id, "Todo Item", item.title, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.todoDao().hardDeleteItem(item.id)
                        else db.todoDao().upsertItem(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.todoDao().upsertItem(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingTodoLists.forEach { item ->
            list.add(PendingChange(item.id, "Todo List", item.name, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.todoDao().hardDeleteList(item.id)
                        else db.todoDao().upsertList(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.todoDao().upsertList(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingGroceryItems.forEach { item ->
            list.add(PendingChange(item.id, "Grocery Item", item.name, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteItem(item.id)
                        else db.groceryDao().upsertItem(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertItem(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingGroceryLists.forEach { item ->
            list.add(PendingChange(item.id, "Grocery List", item.name, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteList(item.id)
                        else db.groceryDao().upsertList(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertList(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingStores.forEach { item ->
            list.add(PendingChange(item.id, "Store", item.name, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteStore(item.id)
                        else db.groceryDao().upsertStore(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertStore(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingCategories.forEach { item ->
            list.add(PendingChange(item.id, "Category", item.name, item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteCategory(item.id)
                        else db.groceryDao().upsertCategory(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertCategory(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingMembers.forEach { item ->
            list.add(PendingChange(item.id, "List Member", "User: ${item.userId}", item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteListMember(item.id)
                        else db.groceryDao().upsertListMember(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertListMember(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        pendingStoreInfos.forEach { item ->
            list.add(PendingChange("${item.groceryItemId}:${item.storeId}", "Store Info", "Price/Aisle Info", item.syncState, item.isDeleted,
                onRevert = {
                    scope.launch {
                        if (item.syncState == "PENDING_INSERT") db.groceryDao().hardDeleteStoreInfo(item.groceryItemId, item.storeId)
                        else db.groceryDao().upsertStoreInfo(item.copy(syncState = "SYNCED", isDeleted = false))
                    }
                },
                onForceUpdate = {
                    scope.launch {
                        db.groceryDao().upsertStoreInfo(item.copy(syncState = "NEED_UPDATE"))
                        SyncWorker.enqueue(context)
                    }
                }
            ))
        }
        
        list.sortedByDescending { it.type }
    }

    // Authed Hello Check State
    var authedHelloBody by remember { mutableStateOf<String?>(null) }
    var isLoadingAuthedHello by remember { mutableStateOf(value = false) }

    // Sync Logs and States observed directly from Room!
    val recentLogs by db.syncLogDao().getRecentLogs(limit = 10).collectAsState(initial = emptyList())

    val latestLog = recentLogs.firstOrNull()
    val lastStatus = latestLog?.status
    val lastAttemptTime = latestLog?.timestamp ?: 0L
    val lastError = latestLog?.errorMessage
    val lastSuccessTime = recentLogs.firstOrNull { it.status == "SUCCESS" }?.timestamp ?: 0L

    var lastSyncedAtState by remember { mutableStateOf<String?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var isSyncExpanded by remember { mutableStateOf(false) }
    var isTablesExpanded by remember { mutableStateOf(false) }

    // Helper to reload shared preference metadata (e.g. server high-watermark)
    fun reloadSyncMetadata() {
        val prefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
        lastSyncedAtState = prefs.getString("last_synced_at", null)
    }

    // Refresh data periodically
    LaunchedEffect(Unit) {
        if (idToken != null) {
            isLoadingAuthedHello = true
            authedHelloBody = TeddyRepository.fetchAuthedHelloBody(idToken)
            isLoadingAuthedHello = false
        } else {
            authedHelloBody = "No ID Token found"
        }

        while (true) {
            reloadSyncMetadata()
            currentTime = System.currentTimeMillis()
            delay(1000.milliseconds) // Poll/Tick every 1 second to keep UI/timers fresh
        }
    }

    // Determine Sync Worker Status color
    val syncColor = when {
        unsyncedCount > 0 -> Color.Yellow
        lastStatus == "FAILURE" || lastStatus == "RETRY" -> Color.Red
        lastStatus == "SUCCESS" -> Color.Green
        else -> Color.Gray
    }

    fun formatTime(timeMillis: Long): String {
        if (timeMillis == 0L) return "Never"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TACTICAL DIAGNOSTICS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0B14)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D0B14), Color(0xFF050508))
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ROW 1: Authed Hello Check
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAuthed() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161424)),
                    border = BorderStroke(1.dp, Color(0xFF3700B3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Indicator Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isLoadingAuthedHello -> Color.Gray
                                        authedHelloBody == "OK" -> Color.Green
                                        else -> Color.Red
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingAuthedHello) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (authedHelloBody == "OK") Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authed Hello Check",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isLoadingAuthedHello -> "Verifying endpoint..."
                                    authedHelloBody == "OK" -> "Status: OK (Green)"
                                    authedHelloBody != null -> "Status: Fail (Red) - ${authedHelloBody!!.take(100)}"
                                    else -> "Unknown status"
                                },
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = Color.Gray
                        )
                    }
                }

                // ROW 2: Sync Worker Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSyncExpanded = !isSyncExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161424)),
                    border = BorderStroke(1.dp, Color(0xFF3700B3))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Status Indicator Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(syncColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (syncColor) {
                                        Color.Green -> Icons.Default.Sync
                                        Color.Yellow -> Icons.Default.SyncProblem
                                        else -> Icons.Default.SyncDisabled
                                    },
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync Worker Status",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unsynced entries: $unsyncedCount",
                                    color = if (unsyncedCount > 0) Color(0xFF03DAC5) else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(
                                imageVector = if (isSyncExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Expand",
                                tint = Color.Gray
                            )
                        }

                        AnimatedVisibility(
                            visible = isSyncExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Divider(color = Color(0xFF3700B3), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Last Succeeded
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Last Successful Sync:", color = Color.Gray, fontSize = 12.sp)
                                    Text(formatTime(lastSuccessTime), color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Last Synced At (Server Time)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Last Synced At (Server):", color = Color.Gray, fontSize = 12.sp)
                                    Text(lastSyncedAtState ?: "None", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Last Attempted
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Last Attempted Sync:", color = Color.Gray, fontSize = 12.sp)
                                    Text(formatTime(lastAttemptTime), color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Last Status
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Last Run Status:", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        text = lastStatus ?: "No previous runs",
                                        color = when (lastStatus) {
                                            "SUCCESS" -> Color.Green
                                            "FAILURE", "RETRY" -> Color.Red
                                            else -> Color.Gray
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (lastError != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF321414)),
                                        border = BorderStroke(1.dp, Color.Red),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = "Captured Exception Info:",
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = lastError,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3700B3)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FORCE SYNC ATTEMPT NOW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val prefs = context.getSharedPreferences("sync_metadata", Context.MODE_PRIVATE)
                                            prefs.edit()
                                                .remove("last_synced_at")
                                                .commit()
                                            db.syncLogDao().clearAll()
                                            SyncWorker.enqueue(context)
                                            reloadSyncMetadata()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("RESET METADATA & FORCE FULL SYNC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                if (recentLogs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Divider(color = Color(0xFF3700B3), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "EXECUTION LOG HISTORY",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        recentLogs.forEach { log ->
                                            SyncLogItemRow(log = log)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ROW 3: Syncable Tables & Data Recovery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTablesExpanded = !isTablesExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161424)),
                    border = BorderStroke(1.dp, Color(0xFF3700B3))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color.Cyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Syncable Table Management",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isTablesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Expand",
                                tint = Color.Gray
                            )
                        }

                        AnimatedVisibility(
                            visible = isTablesExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(color = Color(0xFF3700B3), thickness = 1.dp)
                                Text(
                                    "Force items in these tables to re-sync as new insertions.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                val tables = listOf(
                                    TableInfo("Todo Lists", todoListsCount, unsyncedTodoListsCount) {
                                        scope.launch {
                                            val list = db.todoDao().getAllListsOneShot()
                                            db.todoDao().insertLists(list.map { it.copy(syncState = "PENDING_INSERT") })
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Todo Items", todoItemsCount, unsyncedTodoItemsCount) {
                                        scope.launch {
                                            val list = db.todoDao().getAllItemsOneShot()
                                            db.todoDao().insertItems(list.map { it.copy(syncState = "PENDING_INSERT") })
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Grocery Lists", groceryListsCount, unsyncedGroceryListsCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllListsOneShot()
                                            list.forEach { db.groceryDao().insertList(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Grocery Items", groceryItemsCount, unsyncedGroceryItemsCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllItemsOneShot()
                                            list.forEach { db.groceryDao().insertItem(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Stores", storesCount, unsyncedStoresCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllStoresOneShot()
                                            list.forEach { db.groceryDao().insertStore(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Categories", categoriesCount, unsyncedCategoriesCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllCategoriesOneShot()
                                            list.forEach { db.groceryDao().insertCategory(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("Store Info", storeInfosCount, unsyncedStoreInfosCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllStoreInfosOneShot()
                                            list.forEach { db.groceryDao().insertStoreInfo(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    },
                                    TableInfo("List Members", 0, unsyncedMembersCount) {
                                        scope.launch {
                                            val list = db.groceryDao().getAllListMembersOneShot()
                                            list.forEach { db.groceryDao().insertListMember(it.copy(syncState = "PENDING_INSERT")) }
                                            SyncWorker.enqueue(context)
                                        }
                                    }
                                )

                                tables.forEach { table ->
                                    Button(
                                        onClick = { table.action() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232135)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF3700B3))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = table.name,
                                                    fontSize = 11.sp,
                                                    color = Color.Cyan
                                                )
                                                Text(
                                                    text = "${table.totalCount} total rows",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier.width(90.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (table.unsyncedCount > 0) {
                                                    Text(
                                                        text = "${table.unsyncedCount} PENDING",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Yellow
                                                    )
                                                } else {
                                                    Text(
                                                        text = "SYNCED",
                                                        fontSize = 10.sp,
                                                        color = Color.Green
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "FORCE PUSH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.width(80.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ROW 4: Detailed Pending Changes
                if (allPendingChanges.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161424)),
                        border = BorderStroke(1.dp, Color(0xFF3700B3))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Pending Sync Changes",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFF3700B3), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allPendingChanges.forEach { change ->
                                    PendingChangeRow(change)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PendingChange(
    val id: String,
    val type: String,
    val label: String,
    val syncState: String,
    val isDeleted: Boolean,
    val onRevert: () -> Unit,
    val onForceUpdate: () -> Unit
)

@Composable
fun PendingChangeRow(change: PendingChange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF232135), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = change.type,
                    fontSize = 10.sp,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                if (change.isDeleted) {
                    Text(
                        text = "DELETED",
                        fontSize = 9.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (change.syncState == "NEED_UPDATE") {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "REFRESHING",
                        fontSize = 9.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = change.label,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = "State: ${change.syncState} | ID: ${change.id.take(8)}...",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
        
        Row {
            if (change.syncState != "PENDING_INSERT" && change.syncState != "NEED_UPDATE") {
                IconButton(onClick = change.onForceUpdate) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Force Update from Server",
                        tint = Color.Cyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = change.onRevert) {
                Icon(
                    imageVector = Icons.Default.SettingsBackupRestore,
                    contentDescription = "Revert",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class TableInfo(
    val name: String,
    val totalCount: Int,
    val unsyncedCount: Int,
    val action: () -> Unit
)

@Composable
fun SyncLogItemRow(log: SyncLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (log.status) {
                            "SUCCESS" -> Color.Green
                            "FAILURE" -> Color.Red
                            else -> Color.Yellow
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = log.status,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                val sentCount = log.todoListsSent + log.todoItemsSent + log.groceryListsSent + 
                               log.groceryMembersSent + log.storesSent + log.categoriesSent + 
                               log.groceryItemsSent + log.storeInfosSent
                val recvCount = log.todoListsReceived + log.todoItemsReceived + log.groceryListsReceived + 
                               log.groceryMembersReceived + log.storesReceived + log.categoriesReceived + 
                               log.groceryItemsReceived + log.storeInfosReceived
                
                Text(
                    text = "↑$sentCount sent  |  ↓$recvCount recv",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                
                // Show granular details if there were changes
                if (sentCount > 0 || recvCount > 0) {
                    val details = mutableListOf<String>()
                    if (log.todoListsSent > 0) details.add("todoLists:↑${log.todoListsSent}")
                    if (log.todoItemsSent > 0) details.add("todoItems:↑${log.todoItemsSent}")
                    if (log.groceryListsSent > 0) details.add("groceryLists:↑${log.groceryListsSent}")
                    if (log.groceryMembersSent > 0) details.add("groceryMembers:↑${log.groceryMembersSent}")
                    if (log.storesSent > 0) details.add("stores:↑${log.storesSent}")
                    if (log.categoriesSent > 0) details.add("categories:↑${log.categoriesSent}")
                    if (log.groceryItemsSent > 0) details.add("groceryItems:↑${log.groceryItemsSent}")
                    if (log.storeInfosSent > 0) details.add("storeInfos:↑${log.storeInfosSent}")

                    if (log.todoListsReceived > 0) details.add("todoLists:↓${log.todoListsReceived}")
                    if (log.todoItemsReceived > 0) details.add("todoItems:↓${log.todoItemsReceived}")
                    if (log.groceryListsReceived > 0) details.add("groceryLists:↓${log.groceryListsReceived}")
                    if (log.groceryMembersReceived > 0) details.add("groceryMembers:↓${log.groceryMembersReceived}")
                    if (log.storesReceived > 0) details.add("stores:↓${log.storesReceived}")
                    if (log.categoriesReceived > 0) details.add("categories:↓${log.categoriesReceived}")
                    if (log.groceryItemsReceived > 0) details.add("groceryItems:↓${log.groceryItemsReceived}")
                    if (log.storeInfosReceived > 0) details.add("storeInfos:↓${log.storeInfosReceived}")

                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(", "),
                            color = Color.DarkGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp
                        )
                    }
                }
                if (!log.errorMessage.isNullOrEmpty()) {
                    Text(
                        text = log.errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            Text(
                text = sdf.format(Date(log.timestamp)),
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${log.durationMillis}ms",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}
