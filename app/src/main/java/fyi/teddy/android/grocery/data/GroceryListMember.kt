package fyi.teddy.android.grocery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "grocery_list_members",
    foreignKeys = [
        ForeignKey(
            entity = GroceryList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroceryListMember(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val userId: String,
    val role: String = "MEMBER",
    val joinedAt: Long = System.currentTimeMillis(),

    // Cloud sync tracking columns
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING_INSERT",
    
    val version: Int = 1,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
