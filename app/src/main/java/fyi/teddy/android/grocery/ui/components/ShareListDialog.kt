package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryListMember
import kotlinx.coroutines.flow.Flow

@Composable
fun ShareListDialog(
    listName: String,
    membersFlow: Flow<List<GroceryListMember>>,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onRemoveMember: (GroceryListMember) -> Unit
) {
    val members by membersFlow.collectAsState(initial = emptyList())
    var inviteUserId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share '$listName'") },
        text = {
            Column {
                Text("Share this list with another user by entering their User ID:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inviteUserId,
                    onValueChange = { inviteUserId = it },
                    label = { Text("User ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Current Members:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (members.isEmpty()) {
                    Text("Only you have access to this list.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(members) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(member.userId, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemoveMember(member) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inviteUserId.isNotBlank()) {
                        onShare(inviteUserId)
                        inviteUserId = ""
                    }
                }
            ) { Text("Share") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
