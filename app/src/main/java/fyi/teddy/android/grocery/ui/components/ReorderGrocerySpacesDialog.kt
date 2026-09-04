package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.ui.layout.fractionOfWindowHeight

@Composable
fun ReorderGrocerySpacesDialog(
    spaces: List<GroceryList>,
    onDismiss: () -> Unit,
    onSave: (List<GroceryList>) -> Unit
) {
    var workingList by remember(spaces) { mutableStateOf(spaces) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reorder Spaces",
                style = MaterialTheme.typography.titleLarge,
                color = GroceryTheme.colors.onSurface
            )
        },
        text = {
            val listMaxHeight = fractionOfWindowHeight(fraction = 0.5f, min = 200.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listMaxHeight)
            ) {
                if (workingList.isEmpty()) {
                    Text(
                        text = "No custom spaces created yet.",
                        color = GroceryTheme.colors.onSurfaceMuted,
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
                                color = GroceryTheme.colors.cardRaised
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
                                        tint = GroceryTheme.colors.onSurfaceMuted,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    Text(
                                        text = space.name,
                                        color = GroceryTheme.colors.onSurface,
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
                                            tint = if (index > 0) GroceryTheme.colors.onSurface else GroceryTheme.colors.onSurfaceFaint
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
                                            tint = if (index < workingList.size - 1) GroceryTheme.colors.onSurface else GroceryTheme.colors.onSurfaceFaint
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
                    containerColor = MaterialTheme.colorScheme.primary,
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
                colors = ButtonDefaults.textButtonColors(contentColor = GroceryTheme.colors.onSurfaceMuted)
            ) {
                Text("Cancel")
            }
        },
        containerColor = GroceryTheme.colors.dialog,
        titleContentColor = GroceryTheme.colors.onSurface,
        textContentColor = GroceryTheme.colors.onSurface
    )
}
