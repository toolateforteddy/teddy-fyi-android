package fyi.teddy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.utils.getIconForTask
import fyi.teddy.android.utils.getIconByName
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
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


sealed class HexCellType {
    data class Task(val item: TodoItem, val color: Color) : HexCellType()
    data class Counter(val count: Int) : HexCellType()
    data class Backlog(val count: Int) : HexCellType()
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BattleMapTodoGrid(
    todoItems: List<TodoItem>,
    backlogCount: Int,
    onNavigateToTodo: (String?) -> Unit,
    onTodoLongClick: (TodoItem) -> Unit = {},
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
                .clip(CutCornerShape(12.dp))
                .background(Color(0xFF161424))
                .border(2.dp, Color(0xFF3700B3), CutCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "What shall we do today?",
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
            
            // Grid is exactly 3 columns (spans 3.0 * W)
            val hexWidthDp = totalWidth / 3f
            val hexHeightDp = hexWidthDp / 0.866f

            val xPadding = 8.dp
            val yPadding = 0.dp

            // Build our dynamic nested cells list (alternating rows of 3 and 2)
            val cells = mutableListOf<Triple<Int, Int, HexCellType>>()

            if (todoItems.isEmpty()) {
                // Row 0 has 3 slots: [Backlog, Counter] (Placeholder removed)
                cells.add(Triple(0, 0, HexCellType.Backlog(backlogCount)))
                cells.add(Triple(2, 0, HexCellType.Counter(0)))
            } else {
                val tasks = todoItems
                var taskIndex = 0
                val numTasks = tasks.size
                var row = 0
                
                while (taskIndex < numTasks || row < 3) {
                    val slotsInRow = if (row % 2 == 0) 3 else 2
                    for (col in 0 until slotsInRow) {
                        if (row == 0 && col == 2) {
                            // Counter is always at top-right (Row 0, Col 2)
                            cells.add(Triple(col, row, HexCellType.Counter(numTasks)))
                        } else {
                            if (taskIndex < numTasks) {
                                // Assign colors dynamically in loop
                                val color = when (taskIndex % 6) {
                                    0 -> Color(0xFF03DAC5) // Cyan
                                    1 -> Color(0xFFE91E63) // Pink
                                    2 -> Color(0xFF9C27B0) // Purple
                                    3 -> Color(0xFFCDDC39) // Lime
                                    4 -> Color(0xFFFF9800) // Orange
                                    else -> Color(0xFFFFEB3B) // Yellow
                                }
                                cells.add(Triple(col, row, HexCellType.Task(tasks[taskIndex], color)))
                                taskIndex++
                            }
                        }
                    }
                    row++
                }
            }

            val totalRows = cells.maxOf { it.second } + 1
            // Ensure at least 6 rows to maintain structural depth even when tasks are few
            val displayRows = maxOf(totalRows, 6)
            val gridHeight = hexHeightDp * (displayRows * 0.75f + 0.25f)

            // Inner container holding the exact size of the interlocking grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .drawBehind {
                        val hexWidth = hexWidthDp.toPx()
                        val hexHeight = hexHeightDp.toPx()
                        val xPaddingPx = xPadding.toPx()
                        val yPaddingPx = yPadding.toPx()
                        val gridColor = Color(0xFF1B182B).copy(alpha = 0.2f)
                        val strokeWidth = 1.dp.toPx()

                        val outline = HexagonShape().createOutline(Size(hexWidth, hexHeight), layoutDirection, this)
                        if (outline is Outline.Generic) {
                            val hexPath = outline.path
                            for (r in 0 until displayRows) {
                                val slotsInRow = if (r % 2 == 0) 3 else 2
                                for (c in 0 until slotsInRow) {
                                    val xOffset = xPaddingPx + hexWidth * (if (r % 2 == 1) c.toFloat() + 0.5f else c.toFloat())
                                    val yOffset = yPaddingPx + hexHeight * (r.toFloat() * 0.75f)

                                    translate(left = xOffset, top = yOffset) {
                                        drawPath(
                                            path = hexPath,
                                            color = gridColor,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }
                            }
                        }
                    }
            ) {
                cells.forEach { (col, row, type) ->
                    val xOffset = xPadding + hexWidthDp * (if (row % 2 == 1) col.toFloat() + 0.5f else col.toFloat())
                    val yOffset = yPadding + hexHeightDp * (row.toFloat() * 0.75f)

                    Box(
                        modifier = Modifier
                            .size(hexWidthDp, hexHeightDp)
                            .offset(x = xOffset, y = yOffset),
                        contentAlignment = Alignment.Center
                    ) {
                        when (type) {
                            is HexCellType.Counter -> {
                                val color = Color(0xFFBCADA0)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .neonGlow(color, HexagonShape(), blurRadius = 4.dp)
                                        .clip(HexagonShape())
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(color.copy(alpha = 0.15f), Color(0xFF12101A))
                                            )
                                        )
                                        .border(2.dp, color, HexagonShape())
                                        .combinedClickable(
                                            onClick = { onNavigateToTodo(null) }
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = type.count.toString(),
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "REMAINING",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                            is HexCellType.Backlog -> {
                                val color = Color(0xFF3700B3)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .neonGlow(color, HexagonShape(), blurRadius = 4.dp)
                                        .clip(HexagonShape())
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(color.copy(alpha = 0.15f), Color(0xFF12101A))
                                            )
                                        )
                                        .border(2.dp, color, HexagonShape())
                                        .combinedClickable(
                                            onClick = { onNavigateToTodo("BACKLOG") }
                                        )
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .offset(y = (-4).dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "BACKLOG",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "(${type.count} Items)",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            is HexCellType.Task -> {
                                val title = type.item.title
                                val color = type.color
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .neonGlow(color, HexagonShape(), blurRadius = 4.dp)
                                        .clip(HexagonShape())
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(color.copy(alpha = 0.15f), Color(0xFF12101A))
                                            )
                                        )
                                        .border(2.dp, color, HexagonShape())
                                        .combinedClickable(
                                            onClick = { onNavigateToTodo(null) },
                                            onLongClick = { onTodoLongClick(type.item) }
                                        )
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = getIconByName(type.item.icon) ?: getIconForTask(title, Icons.Default.Build),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .offset(y = (-4).dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        var fontSize by remember(title) { mutableStateOf(14.sp) }
                                        
                                        Text(
                                            text = title.uppercase(),
                                            color = Color.White,
                                            fontSize = fontSize,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            lineHeight = fontSize * 1.2f,
                                            onTextLayout = { textLayoutResult ->
                                                if (textLayoutResult.hasVisualOverflow && fontSize > 11.sp) {
                                                    fontSize = (fontSize.value - 0.5f).sp
                                                }
                                            }
                                        )
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
