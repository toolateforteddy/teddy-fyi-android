package fyi.teddy.android.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate16To17() {
        // Create database at version 16
        val db = helper.createDatabase(TEST_DB, 16)
        
        // Add a task with isPlannedForToday = 1 and one with 0
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `isPlannedForToday`) VALUES (1, 'Today task', 0, 1000, 0, 1)")
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `isPlannedForToday`) VALUES (2, 'Future task', 0, 1000, 0, 0)")
        db.close()

        // Run migration to 17
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 17, true, AppDatabase.MIGRATION_16_17)
        
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
        val db = helper.createDatabase(TEST_DB, 17)
        
        // Insert parent and child items
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `isDaily`) VALUES (1, 'Parent task', 0, 1000, 0, 1000, 0)")
        db.execSQL("INSERT INTO `todo_items` (`id`, `title`, `isCompleted`, `createdAt`, `position`, `scheduledAt`, `parentId`, `isDaily`) VALUES (2, 'Child task', 0, 1000, 0, 1000, 1, 0)")
        db.close()

        // Run migration to 18
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 18, true, AppDatabase.MIGRATION_17_18)
        
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
}
