package fyi.teddy.android.grocery.data

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
    val joinedAt: Long = System.currentTimeMillis()
)
