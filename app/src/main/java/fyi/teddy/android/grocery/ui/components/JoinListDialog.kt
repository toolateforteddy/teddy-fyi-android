package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun JoinListDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Shared List") },
        text = {
            Column {
                Text("Enter the 8-character invite code shared with you:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 8) code = it.uppercase() },
                    label = { Text("Invite Code") },
                    placeholder = { Text("e.g. AB12CD34") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.length == 8) {
                        onJoin(code)
                        onDismiss()
                    }
                },
                enabled = code.length == 8
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
