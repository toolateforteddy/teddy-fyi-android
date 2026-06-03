package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SnoozeForDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Boolean) -> Unit // daysOrMonths, isMonths
) {
    var amount by remember { mutableStateOf("") }
    var isMonths by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze For...") },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Amount") }
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = !isMonths, onClick = { isMonths = false })
                    Text("Days")
                    RadioButton(selected = isMonths, onClick = { isMonths = true })
                    Text("Months")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toIntOrNull() ?: 0
                onConfirm(value, isMonths)
            }) { Text("Snooze") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
