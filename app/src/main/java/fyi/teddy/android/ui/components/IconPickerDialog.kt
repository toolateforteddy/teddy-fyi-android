package fyi.teddy.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.utils.getIconByName

/**
 * Shared, app-neutral icon picker. Used by both the Todo and Grocery features, so it must
 * theme itself only from [MaterialTheme.colorScheme] and never import a feature theme.
 */
private val iconNames = listOf(
    "Build", "Home", "Plumbing", "ElectricalServices", "CleaningServices",
    "Brush", "Yard", "Work", "AttachMoney", "CreditCard",
    "ReceiptLong", "Email", "Phone", "Analytics", "ShoppingCart",
    "LocalShipping", "DirectionsCar", "Storefront", "LocalPharmacy", "FitnessCenter",
    "DirectionsBike", "DirectionsRun", "MedicalInformation", "Restaurant", "Bed",
    "Event", "Schedule", "List", "Group", "Person",
    "Settings", "Computer", "MenuBook", "Movie", "Palette",
    "MusicNote", "Pets", "Flight", "Eco", "Lock"
)

@Composable
fun IconPickerDialog(
    initialIcon: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onAutoAssign: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Icon") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(300.dp)
            ) {
                items(iconNames) { name ->
                    val icon = getIconByName(name)
                    val isSelected = initialIcon == name
                    if (icon != null) {
                        IconButton(
                            onClick = { onConfirm(name) },
                            modifier = if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            } else Modifier
                        ) {
                            Icon(
                                icon,
                                contentDescription = name,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAutoAssign != null) {
                    TextButton(
                        onClick = onAutoAssign,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Magic")
                    }
                }
                TextButton(
                    onClick = { onConfirm(null) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = null
    )
}
