package fyi.teddy.android.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.todo.ui.theme.TodoTheme

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTasksScreen(userId: String, onBack: () -> Unit) = TodoTheme {
    val todoColors = TodoTheme.colors
    val context = LocalContext.current
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(context.applicationContext as android.app.Application, userId)
    )
    val allItems by viewModel.allItems.collectAsState()
    
    val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    
    val scheduledItems = remember(allItems, todayString) {
        allItems.filter { it.scheduledDate != null && it.scheduledDate > todayString && !it.isCompleted }
            .sortedBy { it.scheduledDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = todoColors.screen, titleContentColor = todoColors.onSurface)
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = todoColors.screen) {
            if (scheduledItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No scheduled tasks", color = todoColors.onSurfaceMuted)
                }
            } else {
                LazyColumn {
                    items(scheduledItems) { item ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = todoColors.screen),
                            headlineContent = { Text(item.title, color = todoColors.onSurface) },
                            supportingContent = { Text("Scheduled for: ${item.scheduledDate}", color = todoColors.accent) }
                        )
                        HorizontalDivider(color = todoColors.onSurfaceFaint)
                    }
                }
            }
        }
    }
}
