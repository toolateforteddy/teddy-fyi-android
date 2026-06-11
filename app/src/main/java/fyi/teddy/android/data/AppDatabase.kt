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
    ], 
    version = 27,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun groceryDao(): GroceryDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_item_store_info` ADD COLUMN `userId` TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `isDefaultSupported` INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `categories`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)")
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `categoryId` INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `timesBought` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `isPlannedForToday` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `recurrenceIntervalDays` INTEGER")
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `scheduledAt` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE `todo_items` SET `scheduledAt` = `createdAt` WHERE `scheduledAt` = 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `userId` TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `parentId` INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `isDaily` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `dueDate` INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `userId` TEXT")
                    db.execSQL("ALTER TABLE `stores` ADD COLUMN `userId` TEXT")
                    db.execSQL("ALTER TABLE `categories` ADD COLUMN `userId` TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add scheduledDate column
                db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `scheduledDate` TEXT")
                
                // 2. Initialize scheduledDate for tasks planned for today
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                db.execSQL("UPDATE `todo_items` SET `scheduledDate` = '$today' WHERE `isPlannedForToday` = 1")
                
                // 3. Drop isPlannedForToday column (requires temporary table for SQLite < 3.35)
                db.execSQL("CREATE TABLE `todo_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL, `position` INTEGER NOT NULL DEFAULT 0, `scheduledDate` TEXT, `recurrenceIntervalDays` INTEGER, `scheduledAt` INTEGER NOT NULL, `userId` TEXT, `parentId` INTEGER, `isDaily` INTEGER NOT NULL DEFAULT 0, `dueDate` INTEGER)")
                db.execSQL("INSERT INTO `todo_items_new` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, `recurrenceIntervalDays`, `scheduledAt`, `userId`, `parentId`, `isDaily`, `dueDate`) SELECT `id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, `recurrenceIntervalDays`, `scheduledAt`, `userId`, `parentId`, `isDaily`, `dueDate` FROM `todo_items`")
                db.execSQL("DROP TABLE `todo_items`")
                db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create table `todo_items_new` with TEXT id and parentId, and additional sync columns
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_items_new` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL, 
                        `scheduledDate` TEXT, 
                        `recurrenceIntervalDays` INTEGER, 
                        `scheduledAt` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `parentId` TEXT, 
                        `isDaily` INTEGER NOT NULL, 
                        `dueDate` INTEGER,
                        `sync_state` TEXT NOT NULL DEFAULT 'SYNCED',
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. Insert existing data, converting parentId and id into TEXT keys
                db.execSQL("""
                    INSERT INTO `todo_items_new` (
                        `id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, 
                        `recurrenceIntervalDays`, `scheduledAt`, `userId`, `parentId`, `isDaily`, `dueDate`, 
                        `sync_state`, `version`, `is_deleted`
                    ) 
                    SELECT 
                        'legacy_uuid_' || `id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, 
                        `recurrenceIntervalDays`, `scheduledAt`, `userId`, 
                        CASE WHEN `parentId` IS NOT NULL THEN 'legacy_uuid_' || `parentId` ELSE NULL END, 
                        `isDaily`, `dueDate`, 
                        'SYNCED', 1, 0 
                    FROM `todo_items`
                """.trimIndent())

                // 3. Drop old table
                db.execSQL("DROP TABLE `todo_items`")

                // 4. Rename new table to old table
                db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `description` TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create todo_lists table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_lists` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `colorHex` TEXT NOT NULL DEFAULT '#000000', 
                        `userId` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        `sync_state` TEXT NOT NULL DEFAULT 'SYNCED', 
                        `version` INTEGER NOT NULL DEFAULT 1, 
                        `is_deleted` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. Add listId column to todo_items
                try {
                    db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `listId` TEXT")
                } catch (_: Exception) {}

                // 3. Create index on listId
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_listId` ON `todo_items` (`listId`)")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                """
                ALTER TABLE `todo_items` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0
                """
            )
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create table `todo_items_new` with the updated schema (replacing recurrenceIntervalDays with recurrenceRule)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_items_new` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL, 
                        `scheduledDate` TEXT, 
                        `recurrenceRule` TEXT, 
                        `scheduledAt` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `parentId` TEXT, 
                        `isDaily` INTEGER NOT NULL, 
                        `dueDate` INTEGER,
                        `description` TEXT,
                        `listId` TEXT,
                        `priority` INTEGER NOT NULL DEFAULT 0,
                        `sync_state` TEXT NOT NULL DEFAULT 'SYNCED',
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`listId`) REFERENCES `todo_lists`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                    )
                """.trimIndent())

                // 2. Insert existing data, converting recurrenceIntervalDays to standard iCalendar RRULE format (FREQ=DAILY;INTERVAL=X)
                db.execSQL("""
                    INSERT INTO `todo_items_new` (
                        `id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, 
                        `recurrenceRule`, `scheduledAt`, `userId`, `parentId`, `isDaily`, `dueDate`, 
                        `description`, `listId`, `priority`, `sync_state`, `version`, `is_deleted`
                    ) 
                    SELECT 
                        `id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledDate`, 
                        CASE WHEN `recurrenceIntervalDays` IS NOT NULL THEN 'FREQ=DAILY;INTERVAL=' || `recurrenceIntervalDays` ELSE NULL END, 
                        `scheduledAt`, `userId`, `parentId`, `isDaily`, `dueDate`, 
                        `description`, `listId`, `priority`, `sync_state`, `version`, `is_deleted`
                    FROM `todo_items`
                """.trimIndent())

                // 3. Drop old table
                db.execSQL("DROP TABLE `todo_items`")

                // 4. Rename new table to old table
                db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")

                // 5. Re-create index on listId
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_listId` ON `todo_items` (`listId`)")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grocery_lists` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `ownerId` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grocery_list_members` (
                        `id` TEXT NOT NULL, 
                        `listId` TEXT NOT NULL, 
                        `userId` TEXT NOT NULL, 
                        `role` TEXT NOT NULL DEFAULT 'MEMBER', 
                        `joinedAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`listId`) REFERENCES `grocery_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                // Recreate grocery_items with the Foreign Key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grocery_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `quantity` TEXT NOT NULL, 
                        `isBought` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL, 
                        `categoryId` INTEGER, 
                        `timesBought` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `isActive` INTEGER NOT NULL, 
                        `listId` TEXT, 
                        FOREIGN KEY(`listId`) REFERENCES `grocery_lists`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                    )
                """.trimIndent())

                // Copy existing data from old table to new table (listId will default to NULL)
                db.execSQL("""
                    INSERT INTO `grocery_items_new` (
                        `id`, `name`, `quantity`, `isBought`, `createdAt`, `position`, `categoryId`, 
                        `timesBought`, `userId`, `isActive`, `listId`
                    ) 
                    SELECT 
                        `id`, `name`, `quantity`, `isBought`, `createdAt`, `position`, `categoryId`, 
                        `timesBought`, `userId`, `isActive`, NULL 
                    FROM `grocery_items`
                """.trimIndent())

                // Drop the old table
                db.execSQL("DROP TABLE `grocery_items`")

                // Rename new table to old table
                db.execSQL("ALTER TABLE `grocery_items_new` RENAME TO `grocery_items`")

                // Recreate index on listId
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_items_listId` ON `grocery_items` (`listId`)")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `unit` TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a temporary table with the correct schema including the Foreign Key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grocery_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `quantity` TEXT NOT NULL, 
                        `isBought` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL, 
                        `categoryId` INTEGER, 
                        `timesBought` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `isActive` INTEGER NOT NULL, 
                        `listId` TEXT, 
                        `unit` TEXT, 
                        `notes` TEXT, 
                        FOREIGN KEY(`listId`) REFERENCES `grocery_lists`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                    )
                """.trimIndent())

                // 2. Check if 'notes' or 'unit' columns already exist in the original grocery_items table
                val cursor = db.query("PRAGMA table_info(`grocery_items`)")
                var hasNotes = false
                var hasUnit = false
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "notes") {
                        hasNotes = true
                    }
                    if (columnName == "unit") {
                        hasUnit = true
                    }
                }
                cursor.close()

                // 3. Construct select columns
                val selectColumns = StringBuilder("`id`, `name`, `quantity`, `isBought`, `createdAt`, `position`, `categoryId`, `timesBought`, `userId`, `isActive`, `listId`")
                if (hasUnit) {
                    selectColumns.append(", `unit`")
                } else {
                    selectColumns.append(", NULL as `unit`")
                }
                if (hasNotes) {
                    selectColumns.append(", `notes`")
                } else {
                    selectColumns.append(", NULL as `notes`")
                }

                // 4. Copy the data
                db.execSQL("""
                    INSERT INTO `grocery_items_new` (
                        `id`, `name`, `quantity`, `isBought`, `createdAt`, `position`, `categoryId`, 
                        `timesBought`, `userId`, `isActive`, `listId`, `unit`, `notes`
                    ) 
                    SELECT $selectColumns FROM `grocery_items`
                """.trimIndent())

                // 5. Drop the old table
                db.execSQL("DROP TABLE `grocery_items`")

                // 6. Rename new table to old table
                db.execSQL("ALTER TABLE `grocery_items_new` RENAME TO `grocery_items`")

                // 7. Recreate index on listId
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_items_listId` ON `grocery_items` (`listId`)")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add sync columns to grocery_items
                db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `grocery_items` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")

                // Add sync columns to grocery_lists
                db.execSQL("ALTER TABLE `grocery_lists` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `grocery_lists` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `grocery_lists` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")

                // Add sync columns to grocery_list_members
                db.execSQL("ALTER TABLE `grocery_list_members` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `grocery_list_members` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `grocery_list_members` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")

                // Add sync columns to stores
                db.execSQL("ALTER TABLE `stores` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `stores` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `stores` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")

                // Add sync columns to categories
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")

                // Add sync columns to grocery_item_store_info
                db.execSQL("ALTER TABLE `grocery_item_store_info` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `grocery_item_store_info` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `grocery_item_store_info` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `durationMillis` INTEGER NOT NULL, 
                        `errorMessage` TEXT, 
                        `todoChangesSent` INTEGER NOT NULL, 
                        `groceryChangesSent` INTEGER NOT NULL, 
                        `todoChangesReceived` INTEGER NOT NULL, 
                        `groceryChangesReceived` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(
                        MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, 
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, 
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, 
                        MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                        MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
                        MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                        MIGRATION_25_26, MIGRATION_26_27
                    )
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
