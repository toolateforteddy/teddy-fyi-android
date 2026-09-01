package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import fyi.teddy.android.todo.ui.theme.TodoSpaceSwatches
import fyi.teddy.android.todo.ui.theme.TodoTheme
import fyi.teddy.android.utils.getIconByName

/**
 * Shared by both the Todo and Grocery apps, so it deliberately reads only
 * [MaterialTheme.colorScheme] -- whichever app theme wraps it supplies the colours.
 */
@Composable
fun IconPickerDialog(
    initialIcon: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onAutoAssign: (() -> Unit)? = null
) {
    val iconNames = listOf(
        "Build", "Home", "Plumbing", "ElectricalServices", "CleaningServices",
        "Brush", "Yard", "Work", "AttachMoney", "CreditCard",
        "ReceiptLong", "Email", "Phone", "Analytics", "ShoppingCart",
        "LocalShipping", "DirectionsCar", "Storefront", "LocalPharmacy", "FitnessCenter",
        "DirectionsBike", "DirectionsRun", "MedicalInformation", "Restaurant", "Bed",
        "Event", "Schedule", "List", "Group", "Person",
        "Settings", "Computer", "MenuBook", "Movie", "Palette",
        "MusicNote", "Pets", "Flight", "Eco", "Lock"
    )

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
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

@Composable
fun RecurrenceDialog(
    initialRule: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val options = listOf(
        null to "None",
        "FREQ=DAILY;INTERVAL=1" to "Every Day",
        "FREQ=DAILY;INTERVAL=7" to "Every Week",
        "FREQ=WEEKLY;BYDAY=TU,TH" to "Tuesday & Thursday",
        "FREQ=WEEKLY;BYDAY=MO,WE,FR" to "Monday, Wednesday & Friday",
        "FREQ=MONTHLY;INTERVAL=1" to "Every Month"
    )
    
    var selectedRule by remember { mutableStateOf(initialRule) }
    var customDaysText by remember { 
        mutableStateOf(
            if (initialRule?.startsWith("FREQ=DAILY;INTERVAL=") == true) {
                initialRule.substringAfter("FREQ=DAILY;INTERVAL=")
            } else ""
        ) 
    }
    var isCustomSelected by remember { 
        mutableStateOf(
            initialRule != null && options.none { it.first == initialRule } && initialRule.startsWith("FREQ=DAILY;INTERVAL=")
        ) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Recurrence Schedule") },
        text = {
            Column {
                options.forEach { (rule, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedRule = rule
                                isCustomSelected = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRule == rule && !isCustomSelected,
                            onClick = { 
                                selectedRule = rule
                                isCustomSelected = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustomSelected = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCustomSelected,
                        onClick = { isCustomSelected = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom Days:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = customDaysText,
                        onValueChange = { 
                            customDaysText = it
                            isCustomSelected = true
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isCustomSelected) {
                    val days = customDaysText.toIntOrNull() ?: 1
                    onConfirm("FREQ=DAILY;INTERVAL=$days")
                } else {
                    onConfirm(selectedRule)
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onConfirm(null)
            }) {
                Text("Clear")
            }
        }
    )
}

@Composable
fun EditTitleDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedTitle by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task Title") },
        text = {
            TextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                label = { Text("Title") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (editedTitle.isNotBlank()) {
                    onConfirm(editedTitle)
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AddSubtaskDialog(
    parentTaskTitle: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onFinish: (String) -> Unit
) {
    var subtaskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subtask to '$parentTaskTitle'") },
        text = {
            TextField(
                value = subtaskTitle,
                onValueChange = { subtaskTitle = it },
                label = { Text("Subtask Title") },
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (subtaskTitle.isNotBlank()) {
                            onAdd(subtaskTitle)
                            subtaskTitle = ""
                        }
                    }
                ),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (subtaskTitle.isNotBlank()) {
                    onAdd(subtaskTitle)
                    subtaskTitle = ""
                }
            }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onFinish(subtaskTitle)
            }) {
                Text("Finish")
            }
        }
    )
}

@Composable
fun EditDescriptionDialog(
    initialDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var editedDescription by remember { mutableStateOf(initialDescription ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task Description") },
        text = {
            TextField(
                value = editedDescription,
                onValueChange = { editedDescription = it },
                label = { Text("Description") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(editedDescription.trim().ifEmpty { null })
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onConfirm(null)
            }) {
                Text("Clear")
            }
        }
    )
}

@Composable
fun AddListDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = TodoSpaceSwatches
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Space") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Space Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Color:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { colorStr ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(colorStr)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedColor = colorStr }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == colorStr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TodoTheme.colors.onSurface.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name.trim(), selectedColor)
                }
            }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditListDialog(
    list: fyi.teddy.android.todo.data.TodoList,
    onDismiss: () -> Unit,
    onConfirm: (fyi.teddy.android.todo.data.TodoList) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(list.name) }
    val colors = TodoSpaceSwatches
    var selectedColor by remember { mutableStateOf(list.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Space") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Space Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Color:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { colorStr ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(colorStr)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedColor = colorStr }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == colorStr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TodoTheme.colors.onSurface.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = TodoTheme.colors.danger)
                ) {
                    Text("Delete")
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(list.copy(name = name.trim(), colorHex = selectedColor))
                        }
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    )
}

@Composable
fun PriorityDialog(
    initialPriority: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val priorities = listOf(
        0 to "Low",
        1 to "Medium",
        2 to "High"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Task Priority") },
        text = {
            Column {
                priorities.forEach { (value, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = initialPriority == value,
                            onClick = { onConfirm(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SpacePickerDialog(
    allLists: List<fyi.teddy.android.todo.data.TodoList>,
    currentListId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Space") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // "None" or "All" option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfirm(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentListId == null,
                        onClick = { onConfirm(null) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("No Space (All)", style = MaterialTheme.typography.bodyLarge)
                }

                allLists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(list.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentListId == list.id,
                            onClick = { onConfirm(list.id) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(list.colorHex)),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(list.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
