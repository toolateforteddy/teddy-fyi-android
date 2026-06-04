package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.StandardUnit

@Composable
fun EditItemDetailsDialog(
    item: GroceryItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var editedQuantity by remember { mutableStateOf(item.quantity) }
    var editedUnit by remember { mutableStateOf(item.unit ?: "") }
    var editedNotes by remember { mutableStateOf(item.notes ?: "") }
    val commonUnits = StandardUnit.labels
    var expandedUnitDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item Details") },
        text = {
            Column {
                OutlinedTextField(
                    value = editedQuantity,
                    onValueChange = { editedQuantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editedUnit,
                        onValueChange = { editedUnit = it },
                        label = { Text("Unit (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expandedUnitDropdown = !expandedUnitDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Units")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedUnitDropdown,
                        onDismissRequest = { expandedUnitDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(No Unit)") },
                            onClick = {
                                editedUnit = ""
                                expandedUnitDropdown = false
                            }
                        )
                        commonUnits.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    editedUnit = u
                                    expandedUnitDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    editedQuantity,
                    if (editedUnit.isBlank()) null else editedUnit,
                    if (editedNotes.isBlank()) null else editedNotes
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
