package fyi.teddy.android.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val unsyncedCount by db.todoDao().getUnsyncedCountFlow().collectAsState(initial = 0)

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
            delay(1000) // Poll/Tick every 1 second to keep UI/timers fresh
        }
    }

    // Determine Sync Worker Status color
    val tenMinutesMillis = 10 * 60 * 1000L
    val syncColor = when {
        lastStatus == "ERROR" -> Color.Red
        (lastStatus == "SUCCESS") && (currentTime - lastSuccessTime <= tenMinutesMillis) -> Color.Green
        else -> Color.Yellow // successfully synced > 10m ago, or no attempts yet (lastStatus != "ERROR")
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
                modifier = Modifier.fillMaxSize(),
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
                                            "ERROR" -> Color.Red
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
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
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
                                    recentLogs.forEach { log ->
                                        SyncLogItemRow(log = log)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
                Text(
                    text = "↑${log.todoChangesSent + log.groceryChangesSent} sent  |  ↓${log.todoChangesReceived + log.groceryChangesReceived} recv",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
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
