package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(Color(0xFF121214), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = newItemTitle,
            onValueChange = { newItemTitle = it },
            modifier = Modifier.weight(1f),
            placeholder = { 
                Text(
                    text = "Add task... @tomorrow #work", 
                    color = Color.Gray.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = NeonTeal,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onAddNewItem(newItemTitle)
                    newItemTitle = ""
                }
            ),
            singleLine = true
        )
        IconButton(
            onClick = {
                onAddNewItem(newItemTitle)
                newItemTitle = ""
            },
            modifier = Modifier
                .padding(4.dp)
                .background(NeonTeal.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = NeonTeal)
        }
    }
}
