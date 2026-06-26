package fyi.teddy.android.grocery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

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
    val id: String = UUID.randomUUID().toString(),
    val groceryItemId: String,
    val storeId: String,
    val price: Double? = null,
    val isAvailable: Boolean = true,
    val userId: String? = null,

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING_INSERT",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
