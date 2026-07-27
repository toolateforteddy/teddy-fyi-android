package fyi.teddy.android.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import fyi.teddy.android.todo.data.TodoItem
import kotlin.math.cos
import kotlin.math.sin

object TacticalHexCanvasRenderer {

    private val HEX_COLORS = intArrayOf(
        "#03DAC5".toColorInt(), // Cyan
        "#E91E63".toColorInt(), // Pink
        "#9C27B0".toColorInt(), // Purple
        "#CDDC39".toColorInt(), // Lime
        "#FF9800".toColorInt(), // Orange
        "#FFEB3B".toColorInt()  // Yellow
    )

    fun renderHexGrid(
        todoItems: List<TodoItem>,
        widthPx: Int,
        heightPx: Int,
        density: Float
    ): Bitmap {
        val width = widthPx.coerceAtLeast(100)
        val height = heightPx.coerceAtLeast(100)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 1. Fill Tactical dark background (#161424)
        val bgPaint = Paint().apply {
            color = "#161424".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer glow border
        val borderPaint = Paint().apply {
            color = "#3700B3".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            isAntiAlias = true
        }
        val borderRect = RectF(2f * density, 2f * density, width - 2f * density, height - 2f * density)
        canvas.drawRoundRect(borderRect, 8f * density, 8f * density, borderPaint)

        // 2. Tactical Banner at top
        val bannerHeight = (28f * density).coerceAtMost(height * 0.25f)
        val bannerPaddingHorizontal = 8f * density

        val bannerPaint = Paint().apply {
            color = "#221D38".toColorInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bannerStrokePaint = Paint().apply {
            color = "#03DAC5".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            isAntiAlias = true
        }
        val bannerRect = RectF(
            bannerPaddingHorizontal,
            6f * density,
            width - bannerPaddingHorizontal,
            6f * density + bannerHeight
        )
        canvas.drawRoundRect(bannerRect, 4f * density, 4f * density, bannerPaint)
        canvas.drawRoundRect(bannerRect, 4f * density, 4f * density, bannerStrokePaint)

        val bannerTextPaint = Paint().apply {
            color = "#BCADA0".toColorInt()
            textSize = (11f * density).coerceAtMost(bannerHeight * 0.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val textY = bannerRect.centerY() - (bannerTextPaint.descent() + bannerTextPaint.ascent()) / 2f
        canvas.drawText("TODAY'S TACTICAL", bannerRect.centerX(), textY, bannerTextPaint)

        // 3. Grid area layout calculations
        val availableTop = bannerRect.bottom + 6f * density
        val availableWidth = width - 16f * density
        val availableHeight = height - availableTop - 8f * density

        if (availableWidth <= 0 || availableHeight <= 0) return bitmap

        // Scale columns based on allocated widget width
        val numCols = when {
            widthPx >= 350 * density -> 5
            widthPx >= 250 * density -> 4
            else -> 3
        }

        // Calculate hex dimensions (interlocking honeycomb layout)
        // Horizontal distance between hex centers = hexWidth * 0.75 or (width / numCols)
        val hexWidth = availableWidth / numCols
        val radius = hexWidth / 1.732f // height factor for flat-topped hex
        val rowHeight = radius * 1.5f

        val maxRows = ((availableHeight / rowHeight).toInt()).coerceAtLeast(1)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = (9f * density).coerceAtLeast(10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            isAntiAlias = true
        }

        // Prepare items list
        val activeTasks = todoItems.filter { !it.isCompleted }
        val itemsCount = todoItems.size
        var taskIdx = 0

        val startX = 8f * density + hexWidth / 2f
        val startY = availableTop + radius

        // Draw slots grid
        for (row in 0 until maxRows) {
            val colsInRow = if (row % 2 == 0) numCols else (numCols - 1)
            val offsetX = if (row % 2 == 0) 0f else (hexWidth / 2f)

            for (col in 0 until colsInRow) {
                val cx = startX + col * hexWidth + offsetX
                val cy = startY + row * rowHeight

                // Check bounds
                if (cy + radius > height - 4f * density || cx + radius > width - 4f * density) {
                    continue
                }

                val hexPath = createHexagonPath(cx, cy, radius * 0.88f)

                if (row == 0 && col == 0) {
                    // Slot 0: Counter Badge Hex
                    fillPaint.color = "#2A1F45".toColorInt()
                    canvas.drawPath(hexPath, fillPaint)

                    strokePaint.color = "#03DAC5".toColorInt()
                    canvas.drawPath(hexPath, strokePaint)

                    textPaint.color = "#03DAC5".toColorInt()
                    canvas.drawText("${itemsCount}", cx, cy + (textPaint.textSize / 3f), textPaint)
                } else if (taskIdx < activeTasks.size) {
                    // Task Hex
                    val item = activeTasks[taskIdx]
                    val colorHex = HEX_COLORS[taskIdx % HEX_COLORS.size]

                    fillPaint.color = "#1F1C33".toColorInt()
                    canvas.drawPath(hexPath, fillPaint)

                    strokePaint.color = colorHex
                    canvas.drawPath(hexPath, strokePaint)

                    textPaint.color = Color.WHITE
                    val displayTitle = if (item.title.length > 5) {
                        item.title.take(4) + "…"
                    } else {
                        item.title
                    }
                    canvas.drawText(displayTitle, cx, cy + (textPaint.textSize / 3f), textPaint)
                    taskIdx++
                } else if (activeTasks.isEmpty() && row == 0 && col == 1) {
                    // Empty state indicator
                    fillPaint.color = "#1B2A26".toColorInt()
                    canvas.drawPath(hexPath, fillPaint)

                    strokePaint.color = "#03DAC5".toColorInt()
                    canvas.drawPath(hexPath, strokePaint)

                    textPaint.color = "#03DAC5".toColorInt()
                    canvas.drawText("CLEAR", cx, cy + (textPaint.textSize / 3f), textPaint)
                }
            }
        }

        return bitmap
    }

    private fun createHexagonPath(cx: Float, cy: Float, radius: Float): Path {
        return Path().apply {
            for (i in 0 until 6) {
                val angleRad = Math.toRadians((60 * i - 30).toDouble())
                val x = cx + radius * cos(angleRad).toFloat()
                val y = cy + radius * sin(angleRad).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}
