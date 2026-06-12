package fyi.teddy.android.grocery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.teddy.android.R
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.todo.ui.components.IconPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(userId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: GroceryViewModel = viewModel(
        factory = GroceryViewModelFactory(context.applicationContext as android.app.Application, userId)
    )
    
    val categories by viewModel.categories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToPickIconFor by remember { mutableStateOf<Category?>(null) }

    val onAddCategory = {
        if (newCategoryName.isNotBlank()) {
            viewModel.onEvent(GroceryUiEvent.InsertCategory(newCategoryName))
            newCategoryName = ""
        }
    }

    if (categoryToPickIconFor != null) {
        IconPickerDialog(
            onDismiss = { categoryToPickIconFor = null },
            onConfirm = { iconName ->
                categoryToPickIconFor?.let { 
                    viewModel.onEvent(GroceryUiEvent.UpdateCategory(it.copy(icon = iconName)))
                }
                categoryToPickIconFor = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Category name...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onAddCategory() })
                    )
                    IconButton(onClick = { onAddCategory() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                        CategoryItemRow(
                            category = category,
                            onPickIcon = {
                                categoryToPickIconFor = category
                            },
                            onDelete = {
                                viewModel.onEvent(GroceryUiEvent.DeleteCategory(category))
                            },
                            onMoveUp = {
                                val targetCategory = categories[index - 1]
                                viewModel.onEvent(GroceryUiEvent.SwapCategoryPositions(category, targetCategory))
                            },
                            onMoveDown = {
                                val targetCategory = categories[index + 1]
                                viewModel.onEvent(GroceryUiEvent.SwapCategoryPositions(category, targetCategory))
                            },
                            isFirst = index == 0,
                            isLast = index == categories.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemRow(
    category: Category,
    onPickIcon: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickIcon) {
                val icon = fyi.teddy.android.utils.getIconByName(category.icon)
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Pick Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Pick Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(category.name, color = Color.White, modifier = Modifier.weight(1f))
            
            IconButton(
                onClick = onMoveUp, 
                enabled = !isFirst
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp, 
                    contentDescription = "Move Up", 
                    tint = if (isFirst) Color.Gray else Color.White
                )
            }
            IconButton(
                onClick = onMoveDown, 
                enabled = !isLast
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Move Down", 
                    tint = if (isLast) Color.Gray else Color.White
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
            }
        }
    }
}
