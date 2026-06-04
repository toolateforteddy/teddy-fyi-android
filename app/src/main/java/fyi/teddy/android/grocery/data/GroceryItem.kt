package fyi.teddy.android.grocery.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grocery_items",
    foreignKeys = [
        ForeignKey(
            entity = GroceryList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: String = "1",
    val isBought: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val categoryId: Int? = null,
    val timesBought: Int = 0,
    val userId: String? = null,
    val isActive: Boolean = true,
    val listId: String? = null
)
