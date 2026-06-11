package fyi.teddy.android.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate16To17() {
        // Create database at version 16
        val db = helper.createDatabase(testDb, 16)
        
        // Add a task with isPlannedForToday = 1 and one with 0
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `isPlannedForToday`) VALUES (1, 'Today task', 0, 1000, 0, 1)")
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `isPlannedForToday`) VALUES (2, 'Future task', 0, 1000, 0, 0)")
        db.close()

        // Run migration to 17
        val migratedDb = helper.runMigrationsAndValidate(testDb, 17, true, AppDatabase.MIGRATION_16_17)
        
        // Verify today's task has scheduledDate set and future task has null
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        
        // Check task 1
        var todayTaskScheduledDate: String? = null
        var futureTaskScheduledDate: String? = null
        
        while(cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val scheduledDateIndex = cursor.getColumnIndex("scheduledDate")
            if (scheduledDateIndex != -1) {
                if (id == 1) todayTaskScheduledDate = cursor.getString(scheduledDateIndex)
                if (id == 2) futureTaskScheduledDate = cursor.getString(scheduledDateIndex)
            }
        }
        
        assert(todayTaskScheduledDate != null)
        assert(futureTaskScheduledDate == null)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18() {
        // Create database at version 17
        val db = helper.createDatabase(testDb, 17)
        
        // Insert parent and child items
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`) VALUES (1, 'Parent task', 0, 1000, 0, 1000, 0)")
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `parentId`, `isDaily`) VALUES (2, 'Child task', 0, 1000, 0, 1000, 1, 0)")
        db.close()

        // Run migration to 18
        val migratedDb = helper.runMigrationsAndValidate(testDb, 18, true, AppDatabase.MIGRATION_17_18)
        
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        
        var foundParent = false
        var foundChild = false
        
        while(cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val parentId = cursor.getString(cursor.getColumnIndexOrThrow("parentId"))
            val syncState = cursor.getString(cursor.getColumnIndexOrThrow("sync_state"))
            val version = cursor.getInt(cursor.getColumnIndexOrThrow("version"))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_deleted"))
            
            if (id == "legacy_uuid_1") {
                assert(title == "Parent task")
                assert(parentId == null)
                foundParent = true
            } else if (id == "legacy_uuid_2") {
                assert(title == "Child task")
                assert(parentId == "legacy_uuid_1")
                foundChild = true
            }
            
            assert(syncState == "SYNCED")
            assert(version == 1)
            assert(isDeleted == 0)
        }
        
        assert(foundParent)
        assert(foundChild)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate18To19() {
        // Create database at version 18
        val db = helper.createDatabase(testDb, 18)
        
        // Insert an item in version 18
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`, `sync_state`, `version`, `is_deleted`) VALUES ('uuid-1', 'Description Task', 0, 1000, 0, 1000, 0, 'SYNCED', 1, 0)")
        db.close()

        // Run migration to 19
        val migratedDb = helper.runMigrationsAndValidate(testDb, 19, true, AppDatabase.MIGRATION_18_19)
        
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        
        var foundTask = false
        while(cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val descriptionIndex = cursor.getColumnIndex("description")
            
            assert(descriptionIndex != -1)
            val description = cursor.getString(descriptionIndex)
            
            if (id == "uuid-1") {
                assert(title == "Description Task")
                assert(description == null)
                foundTask = true
            }
        }
        
        assert(foundTask)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate19To20() {
        // Create database at version 19
        val db = helper.createDatabase(testDb, 19)
        
        // Insert an item in version 19
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`, `sync_state`, `version`, `is_deleted`) VALUES ('uuid-1', 'Space Task', 0, 1000, 0, 1000, 0, 'SYNCED', 1, 0)")
        db.close()

        // Run migration to 20
        val migratedDb = helper.runMigrationsAndValidate(testDb, 20, true, AppDatabase.MIGRATION_19_20)
        
        // 1. Verify table todo_lists exists
        val listsCursor = migratedDb.query("SELECT * FROM `todo_lists`")
        listsCursor.close()

        // 2. Verify column listId exists and is null for legacy task
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        var foundTask = false
        while(cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val listIdIndex = cursor.getColumnIndex("listId")
            
            assert(listIdIndex != -1)
            val listId = cursor.getString(listIdIndex)
            
            if (id == "uuid-1") {
                assert(title == "Space Task")
                assert(listId == null)
                foundTask = true
            }
        }
        
        assert(foundTask)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate20To21() {
        // Create database at version 20
        val db = helper.createDatabase(testDb, 20)
        
        // Insert an item in version 20
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`, `sync_state`, `version`, `is_deleted`) VALUES ('uuid-1', 'Priority Task', 0, 1000, 0, 1000, 0, 'SYNCED', 1, 0)")
        db.close()

        // Run migration to 21
        val migratedDb = helper.runMigrationsAndValidate(testDb, 21, true, AppDatabase.MIGRATION_20_21)
        
        // Verify column priority exists and is 0 for legacy task
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        var foundTask = false
        while(cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val priorityIndex = cursor.getColumnIndex("priority")
            
            assert(priorityIndex != -1)
            val priority = cursor.getInt(priorityIndex)
            
            if (id == "uuid-1") {
                assert(title == "Priority Task")
                assert(priority == 0)
                foundTask = true
            }
        }
        
        assert(foundTask)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate21To22() {
        // Create database at version 21
        val db = helper.createDatabase(testDb, 21)
        
        // Insert an item in version 21
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`, `recurrenceIntervalDays`, `sync_state`, `version`, `is_deleted`, `priority`) VALUES ('uuid-1', 'Recurrence Task', 0, 1000, 0, 1000, 0, 5, 'SYNCED', 1, 0, 2)")
        db.close()

        // Run migration to 22
        val migratedDb = helper.runMigrationsAndValidate(testDb, 22, true, AppDatabase.MIGRATION_21_22)
        
        // Verify column recurrenceRule exists and is formatted as FREQ=DAILY;INTERVAL=5 for legacy task
        val cursor = migratedDb.query("SELECT * FROM `todo_items`")
        var foundTask = false
        while(cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val recurrenceRuleIndex = cursor.getColumnIndex("recurrenceRule")
            val legacyColIndex = cursor.getColumnIndex("recurrenceIntervalDays")
            
            assert(recurrenceRuleIndex != -1)
            assert(legacyColIndex == -1) // recurrenceIntervalDays should be dropped
            
            val rrule = cursor.getString(recurrenceRuleIndex)
            
            if (id == "uuid-1") {
                assert(title == "Recurrence Task")
                assert(rrule == "FREQ=DAILY;INTERVAL=5")
                foundTask = true
            }
        }
        
        assert(foundTask)
        cursor.close()
    }
}
