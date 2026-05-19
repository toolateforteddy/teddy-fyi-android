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

@Database(entities = [TodoItem::class, GroceryItem::class, Store::class, GroceryItemStoreInfo::class, Category::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun groceryDao(): GroceryDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `isDefaultSupported` INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `categories`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)")
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `categoryId` INTEGER")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `timesBought` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `isPlannedForToday` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `recurrenceIntervalDays` INTEGER")
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `scheduledAt` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE `todo_items` SET `scheduledAt` = `createdAt` WHERE `scheduledAt` = 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `userId` TEXT")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `parentId` INTEGER")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `isDaily` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `dueDate` INTEGER")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `userId` TEXT")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `userId` TEXT")
                    db.execSQL("ALTER TABLE `categories` ADD COLUMN `userId` TEXT")
                } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
