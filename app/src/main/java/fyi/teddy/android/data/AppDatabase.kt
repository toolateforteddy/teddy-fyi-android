package fyi.teddy.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem

@Database(entities = [TodoItem::class, GroceryItem::class, Store::class, GroceryItemStoreInfo::class, Category::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun groceryDao(): GroceryDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fixed: Use 'TEXT' instead of 'STRING'
                db.execSQL("DROP TABLE IF EXISTS `categories`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)")
                
                // Add column if missing. SQLite doesn't support 'IF NOT EXISTS' for columns, 
                // but we can wrap it in a try-catch for the migration.
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `categoryId` INTEGER")
                } catch (e: Exception) {
                    // Column already exists
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                // NOTE: Destructive migration is disabled to prevent data loss.
                // ALL future schema changes (bumping the version number) MUST 
                // include a corresponding migration script.
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
