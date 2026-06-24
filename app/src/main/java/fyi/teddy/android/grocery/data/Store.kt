package fyi.teddy.android.grocery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stores",
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
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val position: Int = 0,
    val isDefaultSupported: Boolean = true,
    val userId: String? = null,
    val listId: String? = null,

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING_INSERT",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
