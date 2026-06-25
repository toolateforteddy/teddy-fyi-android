package fyi.teddy.android.grocery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryItemWithPriceDetails
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.ui.GroceryPhase

@Composable
fun GroceryItemRowContainer(
    item: GroceryItem,
    currentPhase: GroceryPhase,
    shoppingStoreId: String?,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    stores: List<Store>,
    categories: List<Category>,
    isEditMode: Boolean,
    index: Int,
    totalItems: Int,
    onUpdateItem: (GroceryItem) -> Unit,
    onDeleteItem: () -> Unit,
    onUpdateStoreInfo: (GroceryItemStoreInfo) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleBought: (GroceryItem, Boolean) -> Unit = { _, _ -> }
) {
    var showStoreTagging by remember { mutableStateOf(false) }
    var showEditQuantity by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf(false) }

    GroceryItemRow(
        item = item,
        currentPhase = currentPhase,
        shoppingStoreId = shoppingStoreId,
        itemStoreInfos = itemStoreInfos,
        stores = stores,
        isEditMode = isEditMode,
        onCheckedChange = { isChecked ->
            onToggleBought(item, isChecked)
        },
        onDelete = onDeleteItem,
        onTagStores = { showStoreTagging = true },
        onEditQuantity = { showEditQuantity = true },
        onEditCategory = { showEditCategory = true },
        onUpdatePrice = { storeId, price ->
            val currentInfo = itemStoreInfos.find { it.storeId == storeId }
            onUpdateStoreInfo(
                currentInfo?.copy(price = price) 
                    ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, price = price)
            )
        },
        onMoveUp = { if (index > 0) onMoveItem(index, index - 1) },
        onMoveDown = { if (index < totalItems - 1) onMoveItem(index, index + 1) }
    )
    
    if (showStoreTagging) {
        StoreTaggingDialog(
            stores = stores,
            itemStoreInfos = itemStoreInfos,
            onDismiss = { showStoreTagging = false },
            onToggleAvailability = { storeId, isAvailable ->
                val currentInfo = itemStoreInfos.find { it.storeId == storeId }
                onUpdateStoreInfo(
                    currentInfo?.copy(isAvailable = isAvailable)
                        ?: GroceryItemStoreInfo(groceryItemId = item.id, storeId = storeId, isAvailable = isAvailable)
                )
            }
        )
    }

    if (showEditQuantity) {
        EditItemDetailsDialog(
            item = item,
            onDismiss = { showEditQuantity = false },
            onConfirm = { qty, unit, notes ->
                onUpdateItem(item.copy(quantity = qty, unit = unit, notes = notes))
                showEditQuantity = false
            }
        )
    }

    if (showEditCategory) {
        AlertDialog(
            onDismissRequest = { showEditCategory = false },
            title = { Text("Change Category") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onUpdateItem(item.copy(categoryId = null))
                            showEditCategory = false
                        }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = item.categoryId == null, onClick = null)
                        Text("No Category", modifier = Modifier.padding(start = 8.dp))
                    }
                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdateItem(item.copy(categoryId = category.id))
                                showEditCategory = false
                            }.padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = item.categoryId == category.id, onClick = null)
                            Text(category.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditCategory = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    currentPhase: GroceryPhase,
    shoppingStoreId: String?,
    itemStoreInfos: List<GroceryItemStoreInfo>,
    stores: List<Store>,
    isEditMode: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTagStores: () -> Unit,
    onEditQuantity: () -> Unit,
    onEditCategory: () -> Unit,
    onUpdatePrice: (String, Double) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showPriceInput by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }

    val priceDetails = remember(item, shoppingStoreId, itemStoreInfos, stores) {
        GroceryItemWithPriceDetails.from(item, shoppingStoreId, itemStoreInfos, stores)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
            if (currentPhase == GroceryPhase.SHOPPING) showPriceInput = !showPriceInput
            else if (!isEditMode) onTagStores() 
        },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentPhase == GroceryPhase.SHOPPING) {
                    Checkbox(
                        checked = item.isBought,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray)
                    )
                    ShoppingItemRowContent(priceDetails = priceDetails)
                } else if (isEditMode) {
                    EditableItemRowContent(
                        item = item,
                        onEditCategory = onEditCategory,
                        onEditQuantity = onEditQuantity,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onDelete = onDelete
                    )
                } else {
                    StandardItemRowContent(
                        priceDetails = priceDetails,
                        onEditCategory = onEditCategory,
                        onEditQuantity = onEditQuantity
                    )
                }
            }
            
            if (showPriceInput && shoppingStoreId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter price paid...") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            priceText.toDoubleOrNull()?.let { onUpdatePrice(shoppingStoreId, it) }
                            showPriceInput = false
                        })
                    )
                    Button(onClick = {
                        priceText.toDoubleOrNull()?.let { onUpdatePrice(shoppingStoreId, it) }
                        showPriceInput = false
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
                
                if (itemStoreInfos.isNotEmpty()) {
                    Text("Price History:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    itemStoreInfos.filter { it.price != null }.forEach { info ->
                        val storeName = stores.find { it.id == info.storeId }?.name ?: "Unknown"
                        Text("- $storeName: $${info.price}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemRowContent(
    priceDetails: GroceryItemWithPriceDetails
) {
    val item = priceDetails.item
    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Text(
            text = item.name,
            color = if (item.isBought) Color.Gray else Color.White,
            style = if (item.isBought) MaterialTheme.typography.bodyLarge.copy(
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
            ) else MaterialTheme.typography.bodyLarge
        )
        val displayUnit = if (item.unit.isNullOrBlank()) "" else " ${item.unit}"
        Text(
            text = "Quantity: ${item.quantity}$displayUnit",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
        if (!item.notes.isNullOrBlank()) {
            Text(
                text = item.notes,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (priceDetails.isMoreExpensiveAtCurrentStore) {
            Text(
                text = "Note: ${priceDetails.cheaperStoreName} is cheaper ($$${priceDetails.cheaperStorePrice})",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun StandardItemRowContent(
    priceDetails: GroceryItemWithPriceDetails,
    onEditCategory: () -> Unit,
    onEditQuantity: () -> Unit
) {
    val item = priceDetails.item
    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Text(
            text = item.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable { onEditCategory() }
        )
        val displayUnit = if (item.unit.isNullOrBlank()) "" else " ${item.unit}"
        Text(
            text = "Quantity: ${item.quantity}$displayUnit",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { onEditQuantity() }
        )
        if (!item.notes.isNullOrBlank()) {
            Text(
                text = item.notes,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (priceDetails.isMoreExpensiveAtCurrentStore) {
            Text(
                text = "Note: ${priceDetails.cheaperStoreName} is cheaper ($$${priceDetails.cheaperStorePrice})",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun EditableItemRowContent(
    item: GroceryItem,
    onEditCategory: () -> Unit,
    onEditQuantity: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = item.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable { onEditCategory() }
            )
            val displayUnit = if (item.unit.isNullOrBlank()) "" else " ${item.unit}"
            Text(
                text = "Quantity: ${item.quantity}$displayUnit",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onEditQuantity() }
            )
            if (!item.notes.isNullOrBlank()) {
                Text(
                    text = item.notes,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
        }
        IconButton(onClick = onMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
        }
    }
}
