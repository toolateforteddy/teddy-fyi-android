package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

/** A switchable grocery space. [id] is null for the implicit default list. */
data class GrocerySpaceOption(val id: String?, val name: String)

/**
 * The spaces a person can switch to right now: the named lists, plus the implicit
 * default list while anything still lives in it (or while it is what is on screen).
 */
fun grocerySpaceOptions(
    lists: List<GroceryList>,
    hasItemsInDefaultList: Boolean,
    selectedListId: String?,
): List<GrocerySpaceOption> = buildList {
    if (hasItemsInDefaultList || selectedListId == null) {
        add(GrocerySpaceOption(id = null, name = "Default List"))
    }
    lists.forEach { add(GrocerySpaceOption(id = it.id, name = it.name)) }
}

fun List<GrocerySpaceOption>.nameFor(selectedListId: String?): String =
    firstOrNull { it.id == selectedListId }?.name ?: "Default List"

/**
 * Top bar switcher for phones: the title *is* the space, and tapping it drops down
 * the other spaces. Switching lists is an everyday move, so it must not require
 * edit mode — whose neighbours are "delete list" and "reorder spaces".
 */
@Composable
fun GrocerySpaceSwitcherTitle(
    phaseLabel: String,
    options: List<GrocerySpaceOption>,
    selectedListId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groceryColors = GroceryTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button) { expanded = true }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Grocery · $phaseLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = groceryColors.onSurfaceMuted
                )
                Text(
                    text = options.nameFor(selectedListId),
                    style = MaterialTheme.typography.titleMedium,
                    color = groceryColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Switch list",
                tint = groceryColors.onSurface
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    leadingIcon = {
                        RadioButton(
                            selected = option.id == selectedListId,
                            onClick = null
                        )
                    },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Rail switcher for tablets: every space is on screen all the time, one tap away,
 * so the tablet on the counter never has to sit in edit mode to change lists.
 */
@Composable
fun GrocerySpaceRailSection(
    options: List<GrocerySpaceOption>,
    selectedListId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groceryColors = GroceryTheme.colors

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = null,
                tint = groceryColors.onSurfaceMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Lists",
                style = MaterialTheme.typography.labelMedium,
                color = groceryColors.onSurfaceMuted
            )
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { option ->
                GroceryRailEntry(
                    label = option.name,
                    selected = option.id == selectedListId,
                    onClick = { onSelect(option.id) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = if (option.id == selectedListId) groceryColors.accentBright
                            else groceryColors.onSurfaceMuted
                        )
                    }
                )
            }
        }
    }
}

/**
 * One tappable row in the tablet rail. Used for both spaces and phases so the rail
 * reads as a single column of destinations rather than two unrelated widgets.
 */
@Composable
fun GroceryRailEntry(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val groceryColors = GroceryTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) groceryColors.cardRaised else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) groceryColors.onSurface else groceryColors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
