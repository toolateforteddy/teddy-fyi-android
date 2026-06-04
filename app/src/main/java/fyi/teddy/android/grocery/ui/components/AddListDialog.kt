package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardCapitalization

@Composable
fun AddListDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newListName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New List") },
        text = {
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text("List Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newListName.isNotBlank()) {
                        onConfirm(newListName)
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
