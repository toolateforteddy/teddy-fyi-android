package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import fyi.teddy.android.todo.ui.theme.TodoTheme

@Composable
fun TodoInputBar(
    onAddNewItem: (String) -> Unit,
    isSearchMode: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    var newItemTitle by remember { mutableStateOf("") }
    val todoColors = TodoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(todoColors.panel, RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (isSearchMode) todoColors.accent.copy(alpha = 0.5f) else todoColors.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchMode) {
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = todoColors.accent
                )
            }
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Search backlog...",
                        color = todoColors.onSurfaceMuted.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = todoColors.onSurface,
                    unfocusedTextColor = todoColors.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = todoColors.accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChange("")
                    } else {
                        onClearSearch()
                    }
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = todoColors.onSurfaceMuted
                )
            }
        } else {
            TextField(
                value = newItemTitle,
                onValueChange = { newItemTitle = it },
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        text = "Add task... @tomorrow #work", 
                        color = todoColors.onSurfaceMuted.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = todoColors.onSurface,
                    unfocusedTextColor = todoColors.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = todoColors.accent,
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
                    .background(todoColors.accent.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = todoColors.accent)
            }
        }
    }
}
