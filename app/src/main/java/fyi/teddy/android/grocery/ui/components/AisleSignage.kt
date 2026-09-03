package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.utils.getIconByName

/**
 * Aisle signage: the shared visual identity for a grocery category.
 *
 * A category is recognisable in three places at once -- the sign above its items, the
 * tint edge on each of its tiles, and the glyph on the tile itself -- so you can find
 * "produce" by shape and colour while holding the phone at arm's length in a shop,
 * without reading anything.
 *
 * Categories already carry an [fyi.teddy.android.grocery.data.Category.icon]; this is
 * where that icon finally reaches the lists people actually use.
 */

/**
 * The hue for a category. Stable for the life of a category because it is derived from
 * its id, so the same aisle is the same colour on every screen and every launch.
 * Uncategorised items get the bronze accent rather than a hue of their own.
 */
@Composable
@ReadOnlyComposable
fun aisleTint(categoryId: String?): Color {
    val colors = GroceryTheme.colors
    if (categoryId == null) return colors.accent
    val tints = colors.aisleTints
    return tints[Math.floorMod(categoryId.hashCode(), tints.size)]
}

/** The icon for a category, falling back to a basket when none has been picked. */
fun aisleIcon(iconName: String?): ImageVector = getIconByName(iconName) ?: Icons.Default.ShoppingBasket

/**
 * The sign hanging over one aisle.
 *
 * Pass [isExpanded] and [onToggle] together to make the sign collapse its aisle; leave
 * both null for a sign that is only a label.
 */
@Composable
fun AisleHeader(
    name: String,
    icon: ImageVector,
    tint: Color,
    itemCount: Int,
    modifier: Modifier = Modifier,
    doneCount: Int? = null,
    isExpanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    val colors = GroceryTheme.colors
    val collapsible = isExpanded != null && onToggle != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (collapsible) Modifier.clickable { onToggle!!() } else Modifier),
        color = colors.card,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The tint edge, repeated on every tile below this sign.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(tint),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
                color = colors.accentBright,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (doneCount != null && doneCount > 0) "$doneCount/$itemCount" else "$itemCount",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceMuted,
            )
            if (collapsible) {
                Icon(
                    imageVector = if (isExpanded!!) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = if (isExpanded) "Collapse $name" else "Expand $name",
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
    }
}

/**
 * The mark at the head of an item tile: the item's own glyph when we recognise it
 * ("🍌" for bananas), otherwise the aisle's icon in the aisle's colour. Something always
 * shows, so tiles never fall back to a wall of plain text.
 */
@Composable
fun ItemLeadingMark(
    itemName: String,
    fallbackIcon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val glyph = remember(itemName) { glyphForItem(itemName) }
    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Text(text = glyph, fontSize = 14.sp)
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
