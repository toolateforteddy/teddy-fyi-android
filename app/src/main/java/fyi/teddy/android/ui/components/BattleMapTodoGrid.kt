package fyi.teddy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

// Reusable Shape-Aware Glow Modifier for that Battle Map aesthetic
fun Modifier.neonGlow(
    color: Color,
    shape: Shape,
    blurRadius: Dp = 8.dp
) = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().apply {
                setShadowLayer(blurRadius.toPx(), 0f, 0f, color.toArgb())
            }
        }
        when (outline) {
            is Outline.Generic -> {
                canvas.drawPath(outline.path, paint)
            }
            is Outline.Rectangle -> {
                canvas.drawRect(outline.rect, paint)
            }
            is Outline.Rounded -> {
                val path = Path().apply {
                    addRoundRect(outline.roundRect)
                }
                canvas.drawPath(path, paint)
            }
        }
    }
}

fun getIconForTask(title: String, defaultIcon: androidx.compose.ui.graphics.vector.ImageVector): androidx.compose.ui.graphics.vector.ImageVector {
    val t = title.lowercase()
    return when {
        t.contains("code") || t.contains("deploy") || t.contains("prod") || t.contains("programming") || t.contains("develop") || t.contains("software") || t.contains("fix") -> {
            Icons.Default.Code
        }
        t.contains("car") || t.contains("maintenance") || t.contains("vehicle") || t.contains("drive") || t.contains("tire") || t.contains("oil") -> {
            Icons.Default.DirectionsCar
        }
        t.contains("interview") || t.contains("respond") || t.contains("vitally") || t.contains("call") || t.contains("forum") || t.contains("email") || t.contains("follow-up") || t.contains("message") || t.contains("chat") -> {
            Icons.Default.Forum
        }
        t.contains("pot") || t.contains("kitchen") || t.contains("cook") || t.contains("stock") || t.contains("acquire") || t.contains("store") || t.contains("shop") || t.contains("buy") -> {
            Icons.Default.ShoppingBasket
        }
        t.contains("osso") || t.contains("ingredient") || t.contains("food") || t.contains("dinner") || t.contains("eat") || t.contains("buco") || t.contains("meal") || t.contains("grocery") || t.contains("recipe") || t.contains("lunch") -> {
            Icons.Default.Restaurant
        }
        else -> defaultIcon
    }
}

@Composable
fun BattleMapTodoGrid(
    todoItems: List<String>,
    backlogCount: Int,
    onNavigateToTodo: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cyberpunk beveled title banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .neonGlow(Color(0xFF3700B3), CutCornerShape(12.dp), blurRadius = 4.dp)
                .border(2.dp, Color(0xFF3700B3), CutCornerShape(12.dp))
                .background(Color(0xFF161424))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tactical Battle Map Todo",
                color = Color(0xFFBCADA0),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        // 2. Honeycomb grid container with vertical scroll state
        val scrollState = rememberScrollState()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            val totalWidth = maxWidth - 16.dp // Breathing room at left and right edges for glowing outlines
            
            // Grid is 3 columns (spans 2.5 * W)
            val hexWidthDp = totalWidth / 2.5f
            val hexHeightDp = hexWidthDp / 0.866f
            val gridHeight = hexHeightDp * 3.5f

            val xPadding = 8.dp
            val yPadding = 0.dp

            // Inner container holding the exact size of the interlocking grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            ) {
                // Helper function to render a hexagon cell
                @Composable
                fun HexagonCell(
                    col: Int,
                    row: Int,
                    content: @Composable () -> Unit
                ) {
                    Box(
                        modifier = Modifier
                            .size(hexWidthDp, hexHeightDp)
                            .offset(
                                x = xPadding + hexWidthDp * (col * 0.75f),
                                y = yPadding + (if (col % 2 == 1) hexHeightDp * (row + 0.5f) else hexHeightDp * row.toFloat())
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        content()
                    }
                }

                // We render each of the 9 positions in the honeycomb grid
                for (col in 0..2) {
                    for (row in 0..2) {
                        // Map col and row to our layout
                        when (col to row) {
                            // Slot 2: Counter Cell (Col 2, Row 0)
                            2 to 0 -> {
                                HexagonCell(col, row) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .neonGlow(Color(0xFFBCADA0), HexagonShape(), blurRadius = 4.dp)
                                            .clip(HexagonShape())
                                            .background(Color(0xFF161424))
                                            .border(2.dp, Color(0xFFBCADA0), HexagonShape())
                                            .clickable { onNavigateToTodo(null) }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${todoItems.size}",
                                                color = Color(0xFFBCADA0),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "Remaining",
                                                color = Color(0xFFBCADA0),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Slot 0: Col 0, Row 0 (First task or Backlog)
                            0 to 0 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.isEmpty()) {
                                        // Show Backlog
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFF3700B3), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFF3700B3), HexagonShape())
                                                .clickable { onNavigateToTodo("BACKLOG") }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = null,
                                                    tint = Color(0xFF03DAC5),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Backlog ($backlogCount)",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        // Show Task 0 (Cyan/Blue)
                                        val title = todoItems[0]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFF03DAC5), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFF03DAC5), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFF03DAC5),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Slot 1: Col 1, Row 0 (Task 1 or Placeholder)
                            1 to 0 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.size >= 2) {
                                        val title = todoItems[1]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFFE91E63), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFFE91E63), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFFE91E63),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        PlaceholderHex()
                                    }
                                }
                            }

                            // Slot 3: Col 0, Row 1 (Task 2 or Placeholder)
                            0 to 1 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.size >= 3) {
                                        val title = todoItems[2]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFF9C27B0), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFF9C27B0), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFF9C27B0),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        PlaceholderHex()
                                    }
                                }
                            }

                            // Slot 4: Col 1, Row 1 (Task 3 or Placeholder)
                            1 to 1 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.size >= 4) {
                                        val title = todoItems[3]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFFCDDC39), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFFCDDC39), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFFCDDC39),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        PlaceholderHex()
                                    }
                                }
                            }

                            // Slot 5: Col 2, Row 1 (Task 4 or Placeholder)
                            2 to 1 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.size >= 5) {
                                        val title = todoItems[4]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFFFF9800), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFFFF9800), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF9800),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        PlaceholderHex()
                                    }
                                }
                            }

                            // Slot 6: Col 0, Row 2 (Task 5 or Placeholder)
                            0 to 2 -> {
                                HexagonCell(col, row) {
                                    if (todoItems.size >= 6) {
                                        val title = todoItems[5]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .neonGlow(Color(0xFFFFEB3B), HexagonShape(), blurRadius = 4.dp)
                                                .clip(HexagonShape())
                                                .background(Color(0xFF161424))
                                                .border(2.dp, Color(0xFFFFEB3B), HexagonShape())
                                                .clickable { onNavigateToTodo(null) }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = getIconForTask(title, Icons.Default.Build),
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFEB3B),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        PlaceholderHex()
                                    }
                                }
                            }

                            // Slot 7 & Slot 8 & unused spaces:
                            else -> {
                                HexagonCell(col, row) {
                                    PlaceholderHex()
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
fun PlaceholderHex() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(HexagonShape())
            .background(Color(0xFF0D0B14))
            .border(1.dp, Color(0xFF221F35), HexagonShape())
    )
}
