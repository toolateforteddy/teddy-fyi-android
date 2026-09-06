package fyi.teddy.android.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.GroceryList
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.network.GroceryChangeDelta
import fyi.teddy.android.network.OperationType
import fyi.teddy.android.network.SyncResponse
import fyi.teddy.android.network.TodoChangeDelta
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one-off re-key from the provider subject to the account's surrogate id.
 *
 * Two things are being pinned here, and the second matters more than the first: that the
 * migration moves everything when it runs, and that it does not run early. A mismatch between the
 * two ids is the normal state for as long as the server keeps sending subjects, and re-keying on
 * it would leave every query scoped to an id no arriving row carries.
 */
@RunWith(RobolectricTestRunner::class)
class UserIdMigrationTest {

    private lateinit var database: AppDatabase
    private val subject = "108164327759211"
    private val surrogate = "3f1b9c22-0b6a-4a4b-9d1a-2c9a0d0b7f11"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() = database.close()

    private fun response(
        todo: List<TodoChangeDelta> = emptyList(),
        grocery: List<GroceryChangeDelta> = emptyList()
    ) = SyncResponse(
        remoteTodoChanges = todo,
        remoteGroceryChanges = grocery,
        serverTimestamp = "2026-09-06T00:00:00Z"
    )

    private fun groceryDeltaOwnedBy(userId: String) = GroceryChangeDelta(
        id = "row_1",
        operationType = OperationType.UPDATE,
        version = 2,
        data = Json.parseToJsonElement("""{"id":"row_1","user_id":"$userId"}""")
    )

    @Test
    fun `a row the server sent under the surrogate is the signal to migrate`() {
        assertTrue(
            UserIdMigration.cutoverEvidence(
                response(grocery = listOf(groceryDeltaOwnedBy(surrogate))),
                surrogate
            )
        )
    }

    @Test
    fun `rows still arriving under the subject are not a signal`() {
        assertFalse(
            UserIdMigration.cutoverEvidence(
                response(grocery = listOf(groceryDeltaOwnedBy(subject))),
                surrogate
            )
        )
    }

    @Test
    fun `an empty response is not a signal`() {
        assertFalse(UserIdMigration.cutoverEvidence(response(), surrogate))
    }

    @Test
    fun `a delete, which carries no data, is not a signal`() {
        val deleted = GroceryChangeDelta(
            id = "row_1",
            operationType = OperationType.DELETE,
            version = 3,
            data = null
        )
        assertFalse(UserIdMigration.cutoverEvidence(response(grocery = listOf(deleted)), surrogate))
    }

    @Test
    fun `no stored surrogate is never a signal`() {
        assertFalse(
            UserIdMigration.cutoverEvidence(
                response(grocery = listOf(groceryDeltaOwnedBy(surrogate))),
                userUuid = null
            )
        )
    }

    /** A list names its owner `owner_id`, not `user_id`, and that is just as much proof. */
    @Test
    fun `a list owned by the surrogate is a signal too`() {
        val listDelta = fyi.teddy.android.network.GroceryListChangeDelta(
            id = "list_1",
            operationType = OperationType.UPDATE,
            version = 2,
            data = Json.parseToJsonElement("""{"id":"list_1","owner_id":"$surrogate"}""")
        )
        val body = SyncResponse(
            remoteGroceryListChanges = listOf(listDelta),
            serverTimestamp = "2026-09-06T00:00:00Z"
        )
        assertTrue(UserIdMigration.cutoverEvidence(body, surrogate))
    }

    @Test
    fun `the migration moves every table this device owns rows in`() = runTest {
        val groceryDao = database.groceryDao()
        val todoDao = database.todoDao()

        todoDao.insertList(TodoList(id = "tl", name = "Errands", userId = subject))
        todoDao.insertItem(TodoItem(id = "ti", title = "Milk", userId = subject))
        groceryDao.insertList(GroceryList(id = "gl", name = "Weekly", ownerId = subject))
        groceryDao.insertListMember(
            GroceryListMember(id = "gm", listId = "gl", userId = subject, role = "owner")
        )
        groceryDao.insertItem(GroceryItem(id = "gi", name = "Apples", userId = subject))
        groceryDao.insertStore(Store(id = "gs", name = "Corner shop", userId = subject))
        groceryDao.insertCategory(Category(id = "gc", name = "Fruit", userId = subject))
        database.userSyncMetadataDao()
            .upsert(UserSyncMetadata(userId = subject, lastSyncedAt = "2026-09-01T00:00:00Z"))

        UserIdMigration.migrate(database.userIdMigrationDao(), subject, surrogate)

        assertEquals(1, groceryDao.getAllItems(surrogate).first().size)
        assertEquals(
            "nothing may be left behind under the old id",
            0,
            groceryDao.getAllItems(subject).first().size
        )
        assertEquals(1, groceryDao.getAllStores(surrogate).first().size)
        assertEquals(1, groceryDao.getAllCategories(surrogate).first().size)
        assertEquals(1, todoDao.getAllItems(surrogate).first().size)
        assertEquals(1, todoDao.getAllLists(surrogate).first().size)
    }

    /**
     * Losing the cursor would make the next sync a first sync, which re-labels every local row
     * `PENDING_INSERT` and uploads the entire database.
     */
    @Test
    fun `the sync cursor follows the rows`() = runTest {
        val cursors = database.userSyncMetadataDao()
        cursors.upsert(UserSyncMetadata(userId = subject, lastSyncedAt = "2026-09-01T00:00:00Z"))

        UserIdMigration.migrate(database.userIdMigrationDao(), subject, surrogate)

        assertEquals("2026-09-01T00:00:00Z", cursors.getLastSyncedAt(surrogate))
        assertNull(cursors.getLastSyncedAt(subject))
    }

    @Test
    fun `a re-key is not an edit and does not mark the rows for upload`() = runTest {
        val groceryDao = database.groceryDao()
        groceryDao.insertItem(
            GroceryItem(id = "gi", name = "Apples", userId = subject, syncState = "SYNCED", version = 4)
        )

        UserIdMigration.migrate(database.userIdMigrationDao(), subject, surrogate)

        val moved = groceryDao.getAllItems(surrogate).first().single()
        assertEquals("SYNCED", moved.syncState)
        assertEquals(4, moved.version)
    }

    @Test
    fun `running it twice is harmless`() = runTest {
        val groceryDao = database.groceryDao()
        groceryDao.insertItem(GroceryItem(id = "gi", name = "Apples", userId = subject))

        UserIdMigration.migrate(database.userIdMigrationDao(), subject, surrogate)
        UserIdMigration.migrate(database.userIdMigrationDao(), subject, surrogate)

        assertEquals(1, groceryDao.getAllItems(surrogate).first().size)
    }
}
