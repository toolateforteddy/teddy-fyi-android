package fyi.teddy.android.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.ui.theme.GroceryWidgetPalette

object GroceryWidgetRenderer {

    fun renderGroceryCard(
        groceryItems: List<GroceryItem>,
        widthPx: Int,
        heightPx: Int,
        density: Float
    ): Bitmap {
        val width = widthPx.coerceAtLeast(100)
        val height = heightPx.coerceAtLeast(60)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 1. Fill the bronze card background
        val bgPaint = Paint().apply {
            color = GroceryWidgetPalette.chassis
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer metallic rim
        val borderPaint = Paint().apply {
            color = GroceryWidgetPalette.chassisEdge
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        val borderRect = RectF(3f * density, 3f * density, width - 3f * density, height - 3f * density)
        canvas.drawRoundRect(borderRect, 10f * density, 10f * density, borderPaint)

        // Count active items
        val activeItems = groceryItems.filter { it.isActive }
        val unboughtItems = activeItems.filter { !it.isBought }
        val toBuyCount = unboughtItems.size
        val totalActive = activeItems.size

        val isCompact = heightPx < 110 * density

        if (isCompact) {
            // Compact horizontal banner view
            val countPaint = Paint().apply {
                color = GroceryWidgetPalette.title
                textSize = (24f * density).coerceAtMost(height * 0.45f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val labelPaint = Paint().apply {
                color = GroceryWidgetPalette.label
                textSize = (12f * density).coerceAtMost(height * 0.25f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val countStr = "$toBuyCount"
            val countWidth = countPaint.measureText(countStr)
            val startX = 14f * density

            val countY = height / 2f - (countPaint.descent() + countPaint.ascent()) / 2f
            canvas.drawText(countStr, startX, countY, countPaint)

            val labelX = startX + countWidth + 8f * density
            val labelText = if (toBuyCount == 1) "ITEM TO BUY" else "ITEMS TO BUY"
            canvas.drawText(labelText, labelX, countY - 2f * density, labelPaint)

            val subTextPaint = Paint().apply {
                color = GroceryWidgetPalette.subLabel
                textSize = (9f * density)
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            canvas.drawText("$totalActive TOTAL ON LIST", labelX, countY + 12f * density, subTextPaint)
        } else {
            // Expanded Card layout with Header + Item Preview List
            val headerPaint = Paint().apply {
                color = GroceryWidgetPalette.header
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val headerRect = RectF(6f * density, 6f * density, width - 6f * density, 34f * density)
            canvas.drawRoundRect(headerRect, 6f * density, 6f * density, headerPaint)

            val titlePaint = Paint().apply {
                color = GroceryWidgetPalette.title
                textSize = 12f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("GROCERY LIST", 14f * density, 24f * density, titlePaint)

            val countBadgePaint = Paint().apply {
                color = GroceryWidgetPalette.badge
                textSize = 11f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText("$toBuyCount TO BUY", width - 14f * density, 24f * density, countBadgePaint)

            // Draw Item list rows
            val itemPaint = Paint().apply {
                color = GroceryWidgetPalette.ink
                textSize = 11f * density
                isAntiAlias = true
            }
            val dotPaint = Paint().apply {
                color = GroceryWidgetPalette.chassisEdge
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            var currentY = 52f * density
            val rowHeight = 20f * density
            val maxRows = ((height - currentY - 8f * density) / rowHeight).toInt().coerceAtLeast(1)

            val itemsToShow = unboughtItems.take(maxRows)
            for (item in itemsToShow) {
                if (currentY + 10f * density > height - 6f * density) break

                canvas.drawCircle(18f * density, currentY - 4f * density, 3f * density, dotPaint)
                val truncatedName = if (item.name.length > 20) item.name.take(18) + "…" else item.name
                canvas.drawText(truncatedName, 28f * density, currentY, itemPaint)

                currentY += rowHeight
            }

            if (unboughtItems.isEmpty()) {
                val emptyPaint = Paint().apply {
                    color = GroceryWidgetPalette.title
                    textSize = 12f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("ALL STOCKED!", width / 2f, height / 2f + 10f * density, emptyPaint)
            }
        }

        return bitmap
    }
}
