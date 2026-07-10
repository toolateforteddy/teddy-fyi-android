package fyi.teddy.android.network

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.data.AppDatabase
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.data.TodoItem
import fyi.teddy.android.todo.data.TodoList
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.grocery.data.GroceryItem
import fyi.teddy.android.grocery.data.Store
import fyi.teddy.android.grocery.data.Category
import fyi.teddy.android.grocery.data.GroceryListMember
import fyi.teddy.android.grocery.data.GroceryItemStoreInfo
import fyi.teddy.android.grocery.repository.GroceryRepository
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
            AppDatabase::class.java,
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
        assertEquals(item.syncState, dto.syncState)
        assertEquals(item.version, dto.version)
        assertEquals(item.isDeleted, dto.isDeleted)

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
        assertEquals(list.syncState, dto.syncState)
        assertEquals(list.version, dto.version)
        assertEquals(list.isDeleted, dto.isDeleted)

        val entity = dto.toEntity()
        assertEquals(list, entity)
    }

    @Test
    fun testSerializationAndDeserialization() {
        val request = SyncRequest(
            lastSyncedAt = "2023-10-27T10:15:30Z",
            clientId = "test-client",
            todoChanges = listOf(
                TodoChangeDelta(
                    id = "1",
                    operationType = OperationType.INSERT,
                    version = 1,
                    data = json.encodeToJsonElement(TodoItemDto.serializer(), TodoItem(id = "1", title = "Task 1", syncState = "PENDING_INSERT", version = 1).toDto())
                )
            ),
            groceryChanges = emptyList()
        )

        val serialized = json.encodeToString(request)
        assertFalse(serialized.contains("test-fyi")) // safety check
        assertTrue(serialized.contains("Task 1"))
        assertTrue(serialized.contains("PENDING_INSERT"))

        val deserialized = json.decodeFromString<SyncRequest>(serialized)
        assertEquals(request.lastSyncedAt, deserialized.lastSyncedAt)
        assertEquals(request.todoChanges.size, deserialized.todoChanges.size)
        assertTrue(serialized.contains("INSERT"))
    }

    @Test
    fun testSyncResponseConciseness() {
        // Minimal JSON as might be sent by a concise backend
        val minimalJson = """
            {
                "server_timestamp": "2023-10-27T10:15:30Z"
            }
        """.trimIndent()

        val response = json.decodeFromString<SyncResponse>(minimalJson)
        
        assertEquals("2023-10-27T10:15:30Z", response.serverTimestamp)
        assertTrue(response.successIds.isEmpty())
        assertTrue(response.remoteTodoChanges.isEmpty())
        assertTrue(response.remoteGroceryChanges.isEmpty())
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

    @Test
    fun testLocalMutationLifecycleRules() = runTest {
        val repository = TodoRepository(todoDao)
        val item1 = TodoItem(id = "item1", title = "Task 1", syncState = "PENDING_INSERT", isDeleted = false)
        val item2 = TodoItem(id = "item2", title = "Task 2", syncState = "SYNCED", isDeleted = false)

        todoDao.insertItem(item1)
        todoDao.insertItem(item2)

        // 1. Update SYNCED item
        repository.updateItem(item2.copy(title = "Updated Task 2"))
        val updatedItem2 = todoDao.getUnsyncedItems().find { it.id == "item2" }
        assertNotNull(updatedItem2)
        assertEquals("PENDING_UPDATE", updatedItem2?.syncState)
        assertEquals("Updated Task 2", updatedItem2?.title)

        // 2. Delete PENDING_INSERT item (should hard-delete)
        repository.deleteItem(item1)
        val remainingItems = todoDao.getAllItemsOneShot()
        assertFalse(remainingItems.any { it.id == "item1" })

        // 3. Delete SYNCED/PENDING_UPDATE item (should soft-delete)
        repository.deleteItem(updatedItem2!!)
        val softDeletedItem2 = todoDao.getUnsyncedItems().find { it.id == "item2" }
        assertNotNull(softDeletedItem2)
        assertEquals("PENDING_DELETE", softDeletedItem2?.syncState)
        assertTrue(softDeletedItem2?.isDeleted == true)
    }

    @Test
    fun testGrocerySyncPipelineAndLifecycle() = runTest {
        val groceryDao = database.groceryDao()
        val repository = GroceryRepository(groceryDao)

        val groceryItem1 = GroceryItem(id = "1", name = "Bananas", syncState = "PENDING_INSERT")
        val groceryItem2 = GroceryItem(id = "2", name = "Apples", syncState = "SYNCED")

        groceryDao.insertItem(groceryItem1)
        groceryDao.insertItem(groceryItem2)

        // 1. Verify mapping
        val dto = groceryItem1.toDto()
        assertEquals(groceryItem1.id, dto.id)
        assertEquals(groceryItem1.name, dto.name)
        assertEquals(groceryItem1.syncState, dto.syncState)

        // 2. Update SYNCED item
        repository.updateItem(groceryItem2.copy(name = "Organic Apples"))
        val updatedItem2 = groceryDao.getUnsyncedItems().find { it.id == "2" }
        assertNotNull(updatedItem2)
        assertEquals("PENDING_UPDATE", updatedItem2?.syncState)
        assertEquals("Organic Apples", updatedItem2?.name)

        // 3. Delete PENDING_INSERT item (should hard-delete)
        repository.deleteItem(groceryItem1)
        val remainingItems = groceryDao.getAllItemsOneShot()
        assertFalse(remainingItems.any { it.id == "1" })

        // 4. Delete SYNCED/PENDING_UPDATE item (should soft-delete)
        repository.deleteItem(updatedItem2!!)
        val softDeletedItem2 = groceryDao.getUnsyncedItems().find { it.id == "2" }
        assertNotNull(softDeletedItem2)
        assertEquals("PENDING_DELETE", softDeletedItem2?.syncState)
        assertTrue(softDeletedItem2?.isDeleted == true)
    }

    @Test
    fun testStoreAndCategoryDtoMapping() {
        val store = Store(
            id = "42",
            name = "Trader Joe's",
            position = 2,
            isDefaultSupported = false,
            userId = "user-123",
            syncState = "PENDING_INSERT",
            version = 1,
            isDeleted = false
        )

        val storeDto = store.toDto()
        assertEquals(store.id, storeDto.id)
        assertEquals(store.name, storeDto.name)
        assertEquals(store.position, storeDto.position)
        assertEquals(store.isDefaultSupported, storeDto.isDefaultSupported)
        assertEquals(store.userId, storeDto.userId)
        assertEquals(store.syncState, storeDto.syncState)
        assertEquals(store.version, storeDto.version)
        assertEquals(store.isDeleted, storeDto.isDeleted)

        val storeEntity = storeDto.toEntity()
        assertEquals(store, storeEntity)

        val category = Category(
            id = "15",
            name = "Produce",
            position = 4,
            userId = "user-123",
            syncState = "PENDING_UPDATE",
            version = 2,
            isDeleted = false
        )

        val categoryDto = category.toDto()
        assertEquals(category.id, categoryDto.id)
        assertEquals(category.name, categoryDto.name)
        assertEquals(category.position, categoryDto.position)
        assertEquals(category.userId, categoryDto.userId)
        assertEquals(category.syncState, categoryDto.syncState)
        assertEquals(category.version, categoryDto.version)
        assertEquals(category.isDeleted, categoryDto.isDeleted)

        val categoryEntity = categoryDto.toEntity()
        assertEquals(category, categoryEntity)
    }

    @Test
    fun testGroceryListMemberAndStoreInfoDtoMapping() {
        val member = GroceryListMember(
            id = "member-uuid",
            listId = "list-uuid",
            userId = "user-uuid",
            role = "ADMIN",
            joinedAt = 12345678L,
            syncState = "PENDING_INSERT",
            version = 1,
            isDeleted = false
        )

        val memberDto = member.toDto()
        assertEquals(member.id, memberDto.id)
        assertEquals(member.listId, memberDto.listId)
        assertEquals(member.userId, memberDto.userId)
        assertEquals(member.role, memberDto.role)
        assertEquals(member.joinedAt, memberDto.joinedAt)
        assertEquals(member.syncState, memberDto.syncState)
        assertEquals(member.version, memberDto.version)
        assertEquals(member.isDeleted, memberDto.isDeleted)

        val memberEntity = memberDto.toEntity()
        assertEquals(member, memberEntity)

        val storeInfo = GroceryItemStoreInfo(
            groceryItemId = "50",
            storeId = "12",
            price = 4.99,
            isAvailable = true,
            userId = "user-uuid",
            syncState = "PENDING_UPDATE",
            version = 3,
            isDeleted = false
        )

        val infoDto = storeInfo.toDto()
        assertEquals(storeInfo.groceryItemId, infoDto.groceryItemId)
        assertEquals(storeInfo.storeId, infoDto.storeId)
        assertEquals(storeInfo.price, infoDto.price)
        assertEquals(storeInfo.isAvailable, infoDto.isAvailable)
        assertEquals(storeInfo.userId, infoDto.userId)
        assertEquals(storeInfo.syncState, infoDto.syncState)
        assertEquals(storeInfo.version, infoDto.version)
        assertEquals(storeInfo.isDeleted, infoDto.isDeleted)

        val infoEntity = infoDto.toEntity()
        assertEquals(storeInfo, infoEntity)
    }
}
