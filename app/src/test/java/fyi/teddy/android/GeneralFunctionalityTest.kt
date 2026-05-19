package fyi.teddy.android

import fyi.teddy.android.auth.AuthUtils
import fyi.teddy.android.grocery.data.GroceryDao
import fyi.teddy.android.grocery.repository.GroceryRepository
import fyi.teddy.android.todo.data.TodoDao
import fyi.teddy.android.todo.repository.TodoRepository
import fyi.teddy.android.ui.navigation.Screen
import io.mockk.mockk
import io.mockk.verify
import io.mockk.coVerify
import org.junit.Assert.assertEquals
import org.junit.Test
import android.util.Base64
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
class GeneralFunctionalityTest {

    @Test
    fun screenRoutes_areUniqueAndCorrect() {
        assertEquals("login", Screen.Login.route)
        assertEquals("hello", Screen.Home.route)
        assertEquals("weather", Screen.Weather.route)
        assertEquals("todo", Screen.Todo.route)
        assertEquals("grocery", Screen.Grocery.route)
        
        val routes = listOf(
            Screen.Login.route, Screen.Home.route, Screen.Weather.route,
            Screen.Authed.route, Screen.Todo.route, Screen.Grocery.route,
            Screen.GroceryConfig.route, Screen.Stores.route, Screen.Categories.route
        )
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun authUtils_md5_isCaseAndSpaceInsensitive() {
        val hash1 = AuthUtils.md5("user@example.com")
        val hash2 = AuthUtils.md5("  USER@EXAMPLE.COM  ")
        assertEquals(hash1, hash2)
    }

    private fun createFakeToken(payload: Map<String, Any>): String {
        val header = Base64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val payloadJson = JSONObject(payload).toString()
        val payloadEncoded = Base64.encodeToString(payloadJson.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$header.$payloadEncoded.signature"
    }

    @Test
    fun authUtils_extractUserId_fromSubClaim() {
        val sub = "google-uid-123"
        val token = createFakeToken(mapOf("sub" to sub))
        val extracted = AuthUtils.extractUserIdFromToken(token)
        assertEquals(sub, extracted)
    }

    @Test
    fun todoRepository_delegatesToDao() = runTest {
        val dao = mockk<TodoDao>(relaxed = true)
        val repo = TodoRepository(dao)
        val userId = "user"
        
        repo.getAllItems(userId)
        verify { dao.getAllItems(userId) }
        
        repo.deleteAll(userId)
        coVerify { dao.deleteAll(userId) }
    }

    @Test
    fun groceryRepository_delegatesToDao() = runTest {
        val dao = mockk<GroceryDao>(relaxed = true)
        val repo = GroceryRepository(dao)
        val userId = "user"
        
        repo.getAllStores(userId)
        verify { dao.getAllStores(userId) }
        
        repo.getAllCategories(userId)
        verify { dao.getAllCategories(userId) }
    }
}
