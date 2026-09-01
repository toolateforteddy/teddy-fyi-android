package fyi.teddy.android.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.todo.ui.theme.TodoTheme

@Composable
fun ReorderSpacesDialog(
    spaces: List<TodoList>,
    onDismiss: () -> Unit,
    onSave: (List<TodoList>) -> Unit
) {
    var workingList by remember(spaces) { mutableStateOf(spaces) }
    val todoColors = TodoTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reorder Spaces",
                style = MaterialTheme.typography.titleLarge,
                color = todoColors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (workingList.isEmpty()) {
                    Text(
                        text = "No custom spaces created yet.",
                        color = todoColors.onSurfaceMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = workingList,
                            key = { _, item -> item.id }
                        ) { index, space ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = todoColors.panelRaised
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Grab bar",
                                        tint = todoColors.onSurfaceMuted,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = runCatching { Color(space.colorHex.toColorInt()) }.getOrDefault(todoColors.accent),
                                                shape = CircleShape
                                            )
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = space.name,
                                        color = todoColors.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = workingList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                workingList = mutable
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) todoColors.onSurface else todoColors.onSurfaceFaint
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < workingList.size - 1) {
                                                val mutable = workingList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                workingList = mutable
                                            }
                                        },
                                        enabled = index < workingList.size - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < workingList.size - 1) todoColors.onSurface else todoColors.onSurfaceFaint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(workingList) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = todoColors.accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = todoColors.onSurfaceMuted)
            ) {
                Text("Cancel")
            }
        },
        containerColor = todoColors.dialog,
        titleContentColor = todoColors.onSurface,
        textContentColor = todoColors.onSurface
    )
}
