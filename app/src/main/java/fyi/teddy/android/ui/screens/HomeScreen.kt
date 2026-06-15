package fyi.teddy.android.ui.screens

import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.repository.TeddyRepository
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.todo.ui.components.TodoItemIntent
import fyi.teddy.android.todo.ui.components.TodoItemMenu
import fyi.teddy.android.todo.ui.components.TodoMenuContext
import fyi.teddy.android.ui.components.BattleMapTodoGrid
import fyi.teddy.android.ui.components.BronzeGroceryTile
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    userId: String?,
    userName: String?,
    profilePic: String?,
    onNavigateToWeather: () -> Unit,
    onNavigateToAuthed: () -> Unit,
    onNavigateToTodo: (String?) -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onLogout: () -> Unit,
) {
    Log.d("HomeScreen", "Rendering HomeScreen. userName=$userName, profilePic=$profilePic")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isClusterHappy by remember { mutableStateOf<Boolean?>(null) }
    
    var selectedTodoForItemMenu by remember { mutableStateOf<TodoItem?>(null) }

    LaunchedEffect(Unit) {
        isClusterHappy = TeddyRepository.checkClusterHealth()
    }

    val db = remember { AppDatabase.getDatabase(context) }
    val todoRepository = remember(db) { TodoRepository(db.todoDao(), context) }

    // Fetch live data from Database
    val todoItems by remember(userId) {
        if (userId != null) {
            val todayString = java.time.LocalDate.now().toString()
            db.todoDao().getTodayItems(userId, todayString)
                .map { items ->
                    items.filter { 
                        (!it.isCompleted) && 
                        (!it.isDeleted) && 
                        (it.scheduledDate == todayString) && 
                        (it.parentId == null) 
                    }
                }
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val groceryCount by remember(userId) {
        if (userId != null) {
            db.groceryDao().getAllItems(userId)
                .map { items -> items.filter { it.isActive && !it.isBought }.size }
        } else {
            flowOf(0)
        }
    }.collectAsState(initial = 0)

    val backlogCount by remember(userId) {
        if (userId != null) {
            db.todoDao().getAllItems(userId)
                .map { items ->
                    items.filter { 
                        !it.isCompleted && 
                        !it.isDeleted && 
                        it.scheduledDate == null && 
                        it.parentId == null 
                    }.size
                }
        } else {
            flowOf(0)
        }
    }.collectAsState(initial = 0)

    val greetingName by remember(userName) {
        derivedStateOf {
            val parenthesisMatch = userName?.let { Regex("\\(([^)]+)\\)").find(it) }
            if (parenthesisMatch != null) {
                parenthesisMatch.groupValues[1]
            } else {
                userName?.split(" ")?.firstOrNull() ?: "Teddy"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A0814), Color(0xFF050508))
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, $greetingName",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${todoItems.size} Remaining Today",
                            color = Color(0xFF03DAC5),
                            fontSize = 16.sp
                        )
                    }

                    // Profile Picture or Account icon (click to logout)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profilePic != null) {
                            AsyncImage(
                                model = profilePic,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            val credentialManager = CredentialManager.create(context)
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                            onLogout()
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    val credentialManager = CredentialManager.create(context)
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    onLogout()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Logout",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Tactical Hex Todo Grid (Click to open Todo Manager)
                Box(modifier = Modifier.weight(1f)) {
                    BattleMapTodoGrid(
                        todoItems = todoItems,
                        backlogCount = backlogCount,
                        onNavigateToTodo = onNavigateToTodo,
                        onTodoLongClick = { selectedTodoForItemMenu = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    selectedTodoForItemMenu?.let { item ->
                        TodoItemMenu(
                            item = item,
                            expanded = true,
                            onDismissRequest = { selectedTodoForItemMenu = null },
                            context = TodoMenuContext.DASHBOARD_HEX,
                            onIntent = { intent ->
                                scope.launch {
                                    when (intent) {
                                        is TodoItemIntent.Update -> todoRepository.updateItem(intent.item)
                                        is TodoItemIntent.Delete -> todoRepository.deleteItem(intent.item)
                                        is TodoItemIntent.ToggleComplete -> todoRepository.updateItem(intent.item.copy(isCompleted = intent.isChecked))
                                        is TodoItemIntent.AssignIcon -> {
                                            val session = fyi.teddy.android.auth.UserSession()
                                            session.load(context)
                                            val token = session.idToken
                                            if (token != null) {
                                                todoRepository.assignIcon(intent.item, token)
                                            }
                                        }
                                        is TodoItemIntent.AddSubtask -> {
                                            todoRepository.insertItem(TodoItem(
                                                title = intent.title,
                                                userId = item.userId,
                                                parentId = intent.parentId,
                                                listId = item.listId
                                            ))
                                        }
                                        // Movement intents might be harder without the full list, 
                                        // but we can try basic implementations if needed.
                                        else -> { /* Not implemented on home screen yet */ }
                                    }
                                }
                                selectedTodoForItemMenu = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

//                // Tactical Navigation Buttons for secondary functions
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Button(
//                        onClick = onNavigateToWeather,
//                        modifier = Modifier.weight(1f),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161424)),
//                        shape = RoundedCornerShape(8.dp),
//                        border = BorderStroke(1.dp, Color(0xFF3700B3))
//                    ) {
//                        Text("WEATHER", fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
//                    }
//                    Button(
//                        onClick = onNavigateToAuthed,
//                        modifier = Modifier.weight(1f),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161424)),
//                        shape = RoundedCornerShape(8.dp),
//                        border = BorderStroke(1.dp, Color(0xFF3700B3))
//                    ) {
//                        Text("AUTH TEST", fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))

                // Anchored Grocery Tile
                BronzeGroceryTile(
                    itemCount = groceryCount,
                    modifier = Modifier.clickable { onNavigateToGrocery() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Health Check Icon in bottom right
        if (isClusterHappy != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isClusterHappy!!) Color.Green else Color.Red)
                    .clickable { onNavigateToDebug() }
            ) {
                Icon(
                    imageVector = if (isClusterHappy!!) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = "Health Check",
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
    }
}
