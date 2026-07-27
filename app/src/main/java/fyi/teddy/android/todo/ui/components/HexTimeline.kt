package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun HexTimeline(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale
    val today = LocalDate.now()
    val dates = (0..6).map { today.plusDays(it.toLong()) }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates) { date ->
            val dateStr = date.toString()
            val isSelected = dateStr == selectedDate
            
            val label = when {
                date == today -> "TODAY"
                date == today.plusDays(1) -> "TMRW"
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase()
            }
            
            val dayNum = date.dayOfMonth.toString()

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(80.dp)
                    .clip(ClippedCornerShape(12f))
                    .background(if (isSelected) NeonTeal.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) NeonTeal else MutedGrey.copy(alpha = 0.5f),
                        shape = ClippedCornerShape(12f)
                    )
                    .clickable { onDateSelected(dateStr) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonTeal else MutedGrey,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayNum,
                        color = if (isSelected) Color.White else Color.Gray,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(80.dp)
                    .clip(ClippedCornerShape(12f))
                    .border(
                        width = 2.dp,
                        color = MutedGrey.copy(alpha = 0.5f),
                        shape = ClippedCornerShape(12f)
                    )
                    .clickable { onOpenCalendar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = Color.White
                )
            }
        }
    }
}
