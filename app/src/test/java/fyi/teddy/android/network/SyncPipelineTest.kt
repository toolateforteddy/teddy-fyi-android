package fyi.teddy.android.network

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncPipelineTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        todoDao = database.todoDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testTodoItemToDtoMapping() {
        val item = TodoItem(
            id = "test-item-123",
            title = "Integrate Sync",
            isCompleted = false,
            createdAt = 123456789L,
            position = 3,
            scheduledDate = "2023-10-27",
            recurrenceRule = "FREQ=DAILY",
            scheduledAt = 987654321L,
            userId = "user-1",
            parentId = "parent-id",
            isDaily = true,
            dueDate = 555555L,
            description = "Let's do some cloud syncing!",
            listId = "list-1",
            priority = 2,
            syncState = "PENDING_INSERT",
            version = 1,
            isDeleted = false
        )

        // Map to DTO
        val dto = item.toDto()
        assertEquals(item.id, dto.id)
        assertEquals(item.title, dto.title)
        assertEquals(item.isCompleted, dto.isCompleted)
        assertEquals(item.createdAt, dto.createdAt)
        assertEquals(item.position, dto.position)
        assertEquals(item.scheduledDate, dto.scheduledDate)
        assertEquals(item.recurrenceRule, dto.recurrenceRule)
        assertEquals(item.scheduledAt, dto.scheduledAt)
        assertEquals(item.userId, dto.userId)
        assertEquals(item.parentId, dto.parentId)
        assertEquals(item.isDaily, dto.isDaily)
        assertEquals(item.dueDate, dto.dueDate)
        assertEquals(item.description, dto.description)
        assertEquals(item.listId, dto.listId)
        assertEquals(item.priority, dto.priority)
        assertEquals(item.syncState, dto.sync_state)
        assertEquals(item.version, dto.version)
        assertEquals(item.isDeleted, dto.is_deleted)

        // Map back to entity
        val entity = dto.toEntity()
        assertEquals(item, entity)
    }

    @Test
    fun testTodoListToDtoMapping() {
        val list = TodoList(
            id = "test-list-123",
            name = "Work Tasks",
            colorHex = "#FF0000",
            userId = "user-1",
            createdAt = 123456789L,
            syncState = "PENDING_UPDATE",
            version = 4,
            isDeleted = false
        )

        val dto = list.toDto()
        assertEquals(list.id, dto.id)
        assertEquals(list.name, dto.name)
        assertEquals(list.colorHex, dto.colorHex)
        assertEquals(list.userId, dto.userId)
        assertEquals(list.createdAt, dto.createdAt)
        assertEquals(list.syncState, dto.sync_state)
        assertEquals(list.version, dto.version)
        assertEquals(list.isDeleted, dto.is_deleted)

        val entity = dto.toEntity()
        assertEquals(list, entity)
    }

    @Test
    fun testSerializationAndDeserialization() {
        val request = SyncRequest(
            last_synced_at = "2023-10-27T10:15:30Z",
            todo_changes = TodoChangesDto(
                items = listOf(
                    TodoItem(id = "1", title = "Task 1", syncState = "PENDING_INSERT", version = 1).toDto()
                ),
                lists = listOf(
                    TodoList(id = "l1", name = "List 1", syncState = "PENDING_UPDATE", version = 2).toDto()
                )
            ),
            grocery_changes = GroceryChangesDto()
        )

        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("test-fyi") == false) // safety check
        assertTrue(serialized.contains("Task 1"))
        assertTrue(serialized.contains("PENDING_INSERT"))

        val deserialized = json.decodeFromString<SyncRequest>(serialized)
        assertEquals(request.last_synced_at, deserialized.last_synced_at)
        assertEquals(request.todo_changes.items.size, deserialized.todo_changes.items.size)
        assertEquals(request.todo_changes.items[0].title, deserialized.todo_changes.items[0].title)
    }

    @Test
    fun testUnsyncedDatabaseQueries() = runTest {
        // Insert some synced and unsynced items
        val item1 = TodoItem(id = "1", title = "Synced item", syncState = "SYNCED", isDeleted = false)
        val item2 = TodoItem(id = "2", title = "Pending insert item", syncState = "PENDING_INSERT", isDeleted = false)
        val item3 = TodoItem(id = "3", title = "Soft deleted item", syncState = "SYNCED", isDeleted = true)

        todoDao.insertItem(item1)
        todoDao.insertItem(item2)
        todoDao.insertItem(item3)

        // Query unsynced items
        val unsynced = todoDao.getUnsyncedItems()
        assertEquals(2, unsynced.size)
        assertTrue(unsynced.any { it.id == "2" })
        assertTrue(unsynced.any { it.id == "3" })
        assertFalse(unsynced.any { it.id == "1" })

        // Insert some synced and unsynced lists
        val list1 = TodoList(id = "l1", name = "Synced List", syncState = "SYNCED", isDeleted = false)
        val list2 = TodoList(id = "l2", name = "Pending List", syncState = "PENDING_UPDATE", isDeleted = false)

        todoDao.insertList(list1)
        todoDao.insertList(list2)

        val unsyncedLists = todoDao.getUnsyncedLists()
        assertEquals(1, unsyncedLists.size)
        assertEquals("l2", unsyncedLists[0].id)
    }

    @Test
    fun testHardDeleteOperations() = runTest {
        val item = TodoItem(id = "1", title = "Task 1")
        val list = TodoList(id = "l1", name = "List 1")

        todoDao.insertItem(item)
        todoDao.insertList(list)

        todoDao.hardDeleteItem("1")
        todoDao.hardDeleteList("l1")

        val unsyncedItems = todoDao.getUnsyncedItems()
        val unsyncedLists = todoDao.getUnsyncedLists()

        assertTrue(unsyncedItems.isEmpty())
        assertTrue(unsyncedLists.isEmpty())
    }
}
