package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryConfigScreen(
    userId: String,
    onBack: () -> Unit,
    onManageStores: (String?) -> Unit,
    onManageCategories: (String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: GroceryViewModel = viewModel(
        factory = GroceryViewModelFactory(context.applicationContext as android.app.Application, userId),
    )
    
    val state by viewModel.state.collectAsState()
    val lists by viewModel.lists.collectAsState()
    
    var showListSelectorMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.grocery_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // List Selector
                val activeList = lists.find { it.id == state.selectedListId }
                val activeListName = activeList?.name ?: "Default List"

                Text(
                    text = "Selected List",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showListSelectorMenu = true }
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = activeListName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    
                    DropdownMenu(
                        expanded = showListSelectorMenu,
                        onDismissRequest = { showListSelectorMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default List") },
                            onClick = {
                                viewModel.onEvent(GroceryUiEvent.SetSelectedListId(null))
                                showListSelectorMenu = false
                            }
                        )
                        lists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    viewModel.onEvent(GroceryUiEvent.SetSelectedListId(list.id))
                                    showListSelectorMenu = false
                                }
                            )
                        }
                    }
                }

                ConfigItem(
                    title = stringResource(R.string.manage_stores),
                    subtitle = "Trader Joe\'s, Whole Foods, etc.",
                    icon = Icons.Default.Store,
                    onClick = { onManageStores(state.selectedListId) }
                )
                
                ConfigItem(
                    title = stringResource(R.string.manage_categories),
                    subtitle = "Produce, Dairy, etc.",
                    icon = Icons.Default.Category,
                    onClick = { onManageCategories(state.selectedListId) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.back_to_list))
                }
            }
        }
    }
}

@Composable
fun ConfigItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
