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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.grocery.data.Category
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.groceryDao()
    
    val categories by dao.getAllCategories().collectAsState(initial = emptyList())
    var newCategoryName by remember { mutableStateOf("") }

    val onAddCategory = {
        if (newCategoryName.isNotBlank()) {
            scope.launch {
                val maxPos = categories.maxByOrNull { it.position }?.position ?: -1
                dao.insertCategory(Category(name = newCategoryName, position = maxPos + 1))
                newCategoryName = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAddCategory() })
                    )
                    IconButton(onClick = { onAddCategory() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(categories) { index, category ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.name, color = Color.White, modifier = Modifier.weight(1f))
                            
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val prevCat = categories[index - 1]
                                        scope.launch {
                                            dao.updateCategory(category.copy(position = prevCat.position))
                                            dao.updateCategory(prevCat.copy(position = category.position))
                                        }
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                            }
                            
                            IconButton(
                                onClick = {
                                    if (index < categories.size - 1) {
                                        val nextCat = categories[index + 1]
                                        scope.launch {
                                            dao.updateCategory(category.copy(position = nextCat.position))
                                            dao.updateCategory(nextCat.copy(position = category.position))
                                        }
                                    }
                                },
                                enabled = index < categories.size - 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                            }

                            IconButton(onClick = {
                                scope.launch { dao.deleteCategory(category) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
