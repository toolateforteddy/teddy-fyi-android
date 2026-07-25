package fyi.teddy.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList

@Suppress("MagicNumber")
@Database(
    entities = [
        TodoItem::class, 
        GroceryItem::class, 
        Store::class, 
        GroceryItemStoreInfo::class, 
        Category::class,
        TodoList::class,
        GroceryList::class,
        GroceryListMember::class,
        SyncLog::class,
        UserSyncMetadata::class,
    ], 
    version = 34,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun groceryDao(): GroceryDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun userSyncMetadataDao(): UserSyncMetadataDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
