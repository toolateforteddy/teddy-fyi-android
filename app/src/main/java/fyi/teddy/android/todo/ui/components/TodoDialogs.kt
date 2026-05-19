package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
