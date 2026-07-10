package fyi.teddy.android.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserSyncMetadataTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UserSyncMetadataDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.userSyncMetadataDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun upsertAndGetMetadata() = runTest {
        val userId = "user-1"
        val timestamp = "2023-10-27T10:00:00Z"
        
        dao.upsert(UserSyncMetadata(userId, timestamp))
        
        val result = dao.getLastSyncedAt(userId)
        assertEquals(timestamp, result)
    }

    @Test
    fun upsertOverwritesExisting() = runTest {
        val userId = "user-1"
        val timestamp1 = "2023-10-27T10:00:00Z"
        val timestamp2 = "2023-10-27T11:00:00Z"
        
        dao.upsert(UserSyncMetadata(userId, timestamp1))
        dao.upsert(UserSyncMetadata(userId, timestamp2))
        
        val result = dao.getLastSyncedAt(userId)
        assertEquals(timestamp2, result)
    }

    @Test
    fun getNonExistentUserReturnsNull() = runTest {
        val result = dao.getLastSyncedAt("unknown")
        assertNull(result)
    }

    @Test
    fun clearSpecificUser() = runTest {
        dao.upsert(UserSyncMetadata("user-1", "time1"))
        dao.upsert(UserSyncMetadata("user-2", "time2"))
        
        dao.clear("user-1")
        
        assertNull(dao.getLastSyncedAt("user-1"))
        assertEquals("time2", dao.getLastSyncedAt("user-2"))
    }

    @Test
    fun clearAllUsers() = runTest {
        dao.upsert(UserSyncMetadata("user-1", "time1"))
        dao.upsert(UserSyncMetadata("user-2", "time2"))
        
        dao.clearAll()
        
        assertNull(dao.getLastSyncedAt("user-1"))
        assertNull(dao.getLastSyncedAt("user-2"))
    }
}
