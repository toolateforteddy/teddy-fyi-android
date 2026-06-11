package fyi.teddy.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StylizedShoppingCart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Basket shape
        val basketPath = Path().apply {
            moveTo(w * 0.28f, h * 0.18f)
            lineTo(w * 0.74f, h * 0.18f)
            lineTo(w * 0.65f, h * 0.52f)
            lineTo(w * 0.35f, h * 0.52f)
            close()
        }
        
        // 1. Fill the basket with deep navy-indigo/purple
        drawPath(
            path = basketPath,
            color = Color(0xFF1E1A3C)
        )
        
        // 2. Draw the grid inside the basket
        val gridColor = Color(0xFF6E68B5)
        val gridStrokeWidth = 2.dp.toPx()
        
        // Vertical grid lines
        for (i in 1..3) {
            val fraction = i / 4f
            val topX = w * (0.28f + fraction * 0.46f)
            val bottomX = w * (0.35f + fraction * 0.30f)
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(topX, h * 0.18f),
                end = androidx.compose.ui.geometry.Offset(bottomX, h * 0.52f),
                strokeWidth = gridStrokeWidth
            )
        }
        
        // Horizontal grid lines
        for (i in 1..2) {
            val fraction = i / 3f
            val y = h * (0.18f + fraction * 0.34f)
            val leftX = w * (0.28f + fraction * 0.07f)
            val rightX = w * (0.74f - fraction * 0.09f)
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(leftX, y),
                end = androidx.compose.ui.geometry.Offset(rightX, y),
                strokeWidth = gridStrokeWidth
            )
        }
        
        // 3. Draw the basket outline (thick)
        val outerStrokeWidth = 3.5.dp.toPx()
        val outlineColor = Color(0xFF6158A7)
        drawPath(
            path = basketPath,
            color = outlineColor,
            style = Stroke(
                width = outerStrokeWidth,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // 4. Draw the handle
        val handlePath = Path().apply {
            moveTo(w * 0.28f, h * 0.18f)
            lineTo(w * 0.20f, h * 0.08f)
            lineTo(w * 0.13f, h * 0.08f)
        }
        drawPath(
            path = handlePath,
            color = outlineColor,
            style = Stroke(
                width = outerStrokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // 5. Draw the bottom support bar
        val supportPath = Path().apply {
            moveTo(w * 0.35f, h * 0.52f)
            quadraticBezierTo(w * 0.27f, h * 0.52f, w * 0.27f, h * 0.59f)
            quadraticBezierTo(w * 0.27f, h * 0.65f, w * 0.35f, h * 0.65f)
            lineTo(w * 0.64f, h * 0.65f)
        }
        drawPath(
            path = supportPath,
            color = outlineColor,
            style = Stroke(
                width = outerStrokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // 6. Draw the wheels
        val wheelRadius = w * 0.075f
        val innerWheelRadius = w * 0.025f
        
        val leftWheelCenter = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.76f)
        val rightWheelCenter = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.76f)
        
        // Draw Left Wheel
        drawCircle(
            color = Color(0xFF1E1A3C),
            radius = wheelRadius,
            center = leftWheelCenter
        )
        drawCircle(
            color = outlineColor,
            radius = wheelRadius,
            center = leftWheelCenter,
            style = Stroke(width = outerStrokeWidth)
        )
        drawCircle(
            color = outlineColor,
            radius = innerWheelRadius,
            center = leftWheelCenter
        )
        
        // Draw Right Wheel
        drawCircle(
            color = Color(0xFF1E1A3C),
            radius = wheelRadius,
            center = rightWheelCenter
        )
        drawCircle(
            color = outlineColor,
            radius = wheelRadius,
            center = rightWheelCenter,
            style = Stroke(width = outerStrokeWidth)
        )
        drawCircle(
            color = outlineColor,
            radius = innerWheelRadius,
            center = rightWheelCenter
        )
    }
}

@Composable
fun BronzeGroceryTile(itemCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cornerRadius = 24.dp.toPx()
                val inset = 12.dp.toPx()
                
                // 1. Draw outer rounded rectangle background
                val outerRectPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                        )
                    )
                }
                
                // Rich warm brown radial gradient background
                val backgroundBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4C362B), // Lighter warm brown/bronze center
                        Color(0xFF1F120B)  // Rich dark chocolate/black-brown edge
                    ),
                    center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.4f),
                    radius = w * 0.65f
                )
                drawPath(path = outerRectPath, brush = backgroundBrush)
                
                // 2. Draw outer metallic border frame
                val frameBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBCA085), // Lighter metallic highlight
                        Color(0xFF6E5241)  // Darker shadow
                    )
                )
                drawPath(
                    path = outerRectPath,
                    brush = frameBrush,
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // 3. Draw inner notched border line
                val r = cornerRadius - inset
                val notchPath = Path().apply {
                    // Start at top-left inner straight edge end
                    moveTo(inset + r, inset)
                    // Top edge
                    lineTo(w - (inset + r), inset)
                    // Top-Right corner notch
                    quadraticBezierTo(w - (inset + r), inset + r, w - inset, inset + r)
                    // Right edge
                    lineTo(w - inset, h - (inset + r))
                    // Bottom-Right corner notch
                    quadraticBezierTo(w - (inset + r), h - (inset + r), w - (inset + r), h - inset)
                    // Bottom edge
                    lineTo(inset + r, h - inset)
                    // Bottom-Left corner notch
                    quadraticBezierTo(inset + r, h - (inset + r), inset, h - (inset + r))
                    // Left edge
                    lineTo(inset, inset + r)
                    // Top-Left corner notch
                    quadraticBezierTo(inset + r, inset + r, inset + r, inset)
                    close()
                }
                
                drawPath(
                    path = notchPath,
                    color = Color(0xFF8B6C57), // Metallic golden bronze color
                    style = Stroke(width = 1.5.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Stylized Shopping Cart Icon
            StylizedShoppingCart(
                modifier = Modifier
                    .size(width = 120.dp, height = 80.dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Title: "Grocery List" in Serif
            Text(
                text = "Grocery List",
                color = Color(0xFFE6D5C3), // Light warm gold/bronze
                fontSize = 24.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF100907),
                        offset = androidx.compose.ui.geometry.Offset(1f, 2f),
                        blurRadius = 3f
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtitle: "• 15 items •" in Serif
            Text(
                text = "• $itemCount items •",
                color = Color(0xFFBCADA0), // Lighter gray-bronze
                fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF100907),
                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}
