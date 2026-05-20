package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fyi.teddy.android.R

@Composable
fun TodoInputBar(
    onAddNewItem: (String) -> Unit
) {
    var newItemTitle by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = newItemTitle,
            onValueChange = { newItemTitle = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Add new task...", color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                cursorColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onAddNewItem(newItemTitle)
                    newItemTitle = ""
                }
            )
        )
        IconButton(onClick = {
            onAddNewItem(newItemTitle)
            newItemTitle = ""
        }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
