package fyi.teddy.android.grocery.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "grocery_item_store_info",
    primaryKeys = ["groceryItemId", "storeId"],
    foreignKeys = [
        ForeignKey(
            entity = GroceryItem::class,
            parentColumns = ["id"],
            childColumns = ["groceryItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Store::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groceryItemId"), Index("storeId")]
)
data class GroceryItemStoreInfo(
    val groceryItemId: Int,
    val storeId: Int,
    val price: Double? = null,
    val isAvailable: Boolean = true,
    val userId: String? = null
)
