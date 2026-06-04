package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
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

@Composable
fun RecurrenceDialog(
    initialInterval: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var daysText by remember { mutableStateOf(initialInterval?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Recurrence") },
        text = {
            Column {
                Text("Re-schedule this task X days after completion.")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = daysText.toIntOrNull()
                onConfirm(days)
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
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var subtaskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subtask") },
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
            TextButton(onClick = onDismiss) {
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
    val colors = listOf("#00FFFF", "#FF00FF", "#FFA500", "#00FF00", "#FFFF00", "#FF4500", "#1E90FF")
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
                                    color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorStr)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedColor = colorStr }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == colorStr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
    val colors = listOf("#00FFFF", "#FF00FF", "#FFA500", "#00FF00", "#FFFF00", "#FF4500", "#1E90FF")
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
                                    color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorStr)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedColor = colorStr }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == colorStr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
                    colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Red)
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
