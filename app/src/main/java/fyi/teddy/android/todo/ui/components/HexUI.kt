package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

val NeonTeal = Color(0xFF00F2FE)
val DeepCharcoal = Color(0xFF0B0B0F)
val MutedGrey = Color(0xFF666666)

class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val radius = minOf(size.width, size.height) / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            for (i in 0 until 6) {
                val angle = Math.toRadians(i * 60.0 - 30.0)
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

class ClippedCornerShape(val cornerSize: Float = 12f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(cornerSize, 0f)
            lineTo(size.width - cornerSize, 0f)
            lineTo(size.width, cornerSize)
            lineTo(size.width, size.height - cornerSize)
            lineTo(size.width - cornerSize, size.height)
            lineTo(cornerSize, size.height)
            lineTo(0f, size.height - cornerSize)
            lineTo(0f, cornerSize)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun HexCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonTeal
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(HexagonShape())
            .background(if (checked) color else Color.Transparent)
            .border(
                BorderStroke(2.dp, if (checked) color else MutedGrey),
                HexagonShape()
            ),
        contentAlignment = Alignment.Center
    ) {
        // No tick needed, just solid fill as requested
    }
}
