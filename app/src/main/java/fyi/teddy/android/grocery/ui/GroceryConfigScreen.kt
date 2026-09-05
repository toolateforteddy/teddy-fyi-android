package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.grocery.ui.theme.GroceryDensity
import fyi.teddy.android.grocery.ui.theme.GroceryDisplayPreferences
import fyi.teddy.android.grocery.ui.theme.GroceryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryConfigScreen(
    userId: String,
    onBack: () -> Unit,
    onManageStores: (String?) -> Unit,
    onManageCategories: (String?) -> Unit,
    /**
     * Signing out, where this screen is the only place to do it. Null in the full app, whose
     * dashboard already carries it — passing it there would put a second sign-out inside
     * grocery settings, which is not what settings for a list is for.
     */
    onSignOut: (() -> Unit)? = null,
) = GroceryTheme {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = GroceryTheme.colors.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GroceryTheme.colors.screen,
                    titleContentColor = GroceryTheme.colors.onSurface
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = GroceryTheme.colors.screen
        ) {
            // Scrolls rather than stretches: at the largest density setting the options
            // below are taller than a phone screen, and a settings screen that hides its
            // own way back is worse than one that scrolls.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // List Selector
                val activeList = lists.find { it.id == state.selectedListId }
                val activeListName = activeList?.name ?: "Default List"

                Text(
                    text = "Selected List",
                    color = GroceryTheme.colors.onSurfaceMuted,
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
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = GroceryTheme.colors.onSurfaceMuted)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = activeListName,
                            color = GroceryTheme.colors.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GroceryTheme.colors.onSurface)
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
                
                DensityPicker()

                if (onSignOut != null) {
                    ConfigItem(
                        title = stringResource(R.string.sign_out),
                        subtitle = stringResource(R.string.sign_out_subtitle),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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

/**
 * How big to draw the list.
 *
 * A device on the counter and the same device in your hands in aisle six want different
 * sizes, and nothing about the window can tell those apart -- so it is asked, not guessed.
 * The choice is kept on the device rather than on the list: it describes where this screen
 * is, not what anybody is buying.
 */
@Composable
private fun DensityPicker() {
    val context = LocalContext.current
    val selected by GroceryDisplayPreferences.density.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GroceryTheme.colors.card)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FormatSize, contentDescription = null, tint = GroceryTheme.colors.onSurface)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Text and tile size",
                    color = GroceryTheme.colors.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            GroceryDensity.entries.forEach { density ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { GroceryDisplayPreferences.setDensity(context, density) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = density == selected,
                        onClick = { GroceryDisplayPreferences.setDensity(context, density) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            density.label,
                            color = GroceryTheme.colors.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            density.blurb,
                            color = GroceryTheme.colors.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
        colors = CardDefaults.cardColors(containerColor = GroceryTheme.colors.card)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = GroceryTheme.colors.onSurface)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = GroceryTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = GroceryTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GroceryTheme.colors.onSurfaceMuted)
        }
    }
}
