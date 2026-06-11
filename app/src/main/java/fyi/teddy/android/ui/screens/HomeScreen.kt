package fyi.teddy.android.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.repository.TeddyRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Custom Hexagon Shape
class HexagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val radius = minOf(size.width, size.height) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            for (i in 0 until 6) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val x = cx + radius * Math.cos(angle).toFloat()
                val y = cy + radius * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// Reusable Glow Modifier for that Battle Map aesthetic
fun Modifier.neonGlow(
    color: Color,
    blurRadius: Dp = 8.dp,
    borderRadius: Dp = 0.dp
) = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            this.color = color.toArgb()
            setShadowLayer(blurRadius.toPx(), 0f, 0f, color.toArgb())
        }
        canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            borderRadius.toPx(), borderRadius.toPx(),
            paint
        )
    }
}

@Composable
fun BronzeGroceryTile(itemCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFF2A1F1A), shape = RoundedCornerShape(12.dp)) // Bronze background
            .border(2.dp, Color(0xFF8C6D58), shape = RoundedCornerShape(12.dp)) // Metallic trim
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Neon Purple Shopping Cart
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Market List",
            tint = Color(0xFFBB86FC),
            modifier = Modifier
                .size(48.dp)
                .neonGlow(Color(0xFFBB86FC), blurRadius = 6.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = "Household Market List",
                color = Color(0xFFE5D5C5), // Light warm bronze text
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Shared with Katherine • $itemCount items",
                color = Color(0xFFA8998D),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BattleMapHomeScreen(todoItems: List<String>, groceryCount: Int, backlogCount: Int = 0) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0B14), Color(0xFF050508))
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Block
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hello, Teddy", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${todoItems.size} Remaining Today", color = Color(0xFF03DAC5), fontSize = 16.sp)
                }
            }

            // Tactical Hex Todo Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (todoItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1.1f)
                                .background(Color(0xFF161424), shape = HexagonShape())
                                .border(2.dp, Color(0xFF3700B3), shape = HexagonShape())
                                .neonGlow(Color(0xFF3700B3), blurRadius = 4.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF03DAC5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Backlog ($backlogCount)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(todoItems.take(6)) { task ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1.1f)
                                .background(Color(0xFF161424), shape = HexagonShape())
                                .border(2.dp, Color(0xFF3700B3), shape = HexagonShape())
                                .neonGlow(Color(0xFF3700B3), blurRadius = 4.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Build, // Swap with your actual task category vectors
                                    contentDescription = null,
                                    tint = Color(0xFF03DAC5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = task,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Anchored Grocery Tile
            BronzeGroceryTile(itemCount = groceryCount)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

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
    onLogout: () -> Unit
) {
    Log.d("HomeScreen", "Rendering HomeScreen. userName=$userName, profilePic=$profilePic")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isClusterHappy by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isClusterHappy = TeddyRepository.checkClusterHealth()
    }

    val db = remember { AppDatabase.getDatabase(context) }

    // Fetch live data from Database
    val todoItems by remember(userId) {
        if (userId != null) {
            val todayString = java.time.LocalDate.now().toString()
            db.todoDao().getTodayItems(userId, todayString)
                .map { items ->
                    items.filter { 
                        !it.isCompleted && 
                        !it.isDeleted && 
                        it.scheduledDate == todayString && 
                        it.parentId == null 
                    }.map { it.title }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D0B14), Color(0xFF050508))
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
                            text = "Hello, ${userName ?: "Teddy"}",
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTodo(null) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (todoItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.1f)
                                    .background(Color(0xFF161424), shape = HexagonShape())
                                    .border(2.dp, Color(0xFF3700B3), shape = HexagonShape())
                                    .neonGlow(Color(0xFF3700B3), blurRadius = 4.dp)
                                    .clickable { onNavigateToTodo("BACKLOG") }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color(0xFF03DAC5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Backlog ($backlogCount)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(todoItems.take(6)) { task ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.1f)
                                    .background(Color(0xFF161424), shape = HexagonShape())
                                    .border(2.dp, Color(0xFF3700B3), shape = HexagonShape())
                                    .neonGlow(Color(0xFF3700B3), blurRadius = 4.dp)
                                    .clickable { onNavigateToTodo(null) }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color(0xFF03DAC5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = task,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tactical Navigation Buttons for secondary functions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToWeather,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161424)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF3700B3))
                    ) {
                        Text("WEATHER", fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
                    }
                    Button(
                        onClick = onNavigateToAuthed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161424)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF3700B3))
                    ) {
                        Text("AUTH TEST", fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
