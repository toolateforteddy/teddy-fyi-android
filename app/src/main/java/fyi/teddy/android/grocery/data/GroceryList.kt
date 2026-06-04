package fyi.teddy.android.grocery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "grocery_lists")
data class GroceryList(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
