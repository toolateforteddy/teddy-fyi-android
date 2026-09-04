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
import androidx.compose.ui.unit.dp
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.ui.theme.GroceryTheme
import fyi.teddy.android.ui.layout.fractionOfWindowHeight
import kotlinx.coroutines.flow.Flow

@Composable
fun ShareListDialog(
    listName: String,
    membersFlow: Flow<List<GroceryListMember>>,
    activeInviteCode: String?,
    onDismiss: () -> Unit,
    onCreateInvite: () -> Unit,
    onRemoveMember: (GroceryListMember) -> Unit
) {
    val members by membersFlow.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share '$listName'") },
        text = {
            Column {
                Text("Invite someone to this list by sharing a code:")
                Spacer(modifier = Modifier.height(16.dp))
                
                if (activeInviteCode != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeInviteCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Expires in 24 hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onCreateInvite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Invite Code")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Current Members:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (members.isEmpty()) {
                    Text("Only you have access to this list.", style = MaterialTheme.typography.bodyMedium, color = GroceryTheme.colors.onSurfaceMuted)
                } else {
                    // The member list shares the dialog with the invite code block, so it
                    // takes a smaller slice of the window than a full-height list would.
                    val membersMaxHeight = fractionOfWindowHeight(fraction = 0.22f, min = 120.dp, max = 280.dp)
                    LazyColumn(modifier = Modifier.heightIn(max = membersMaxHeight)) {
                        items(members) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(member.userId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onRemoveMember(member) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = GroceryTheme.colors.danger)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
