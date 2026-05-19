package fyi.teddy.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.auth.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserSessionTest {

    private lateinit var context: Context
    private lateinit var session: UserSession

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = UserSession()
    }

    @Test
    fun saveAndLoadSession() = runBlocking {
        session.userName = "Teddy"
        session.idToken = "token123"
        session.profilePictureUri = "https://example.com/pic.jpg"
        
        session.save(context)
        
        val newSession = UserSession()
        newSession.load(context)
        
        assertEquals("Teddy", newSession.userName)
        assertEquals("token123", newSession.idToken)
        assertEquals("https://example.com/pic.jpg", newSession.profilePictureUri)
    }

    @Test
    fun clearSession() = runBlocking {
        session.userName = "Teddy"
        session.save(context)
        
        session.clear(context)
        
        assertNull(session.userName)
        assertNull(session.idToken)
        
        val loadedSession = UserSession()
        loadedSession.load(context)
        assertNull(loadedSession.userName)
    }

    @Test
    fun loadEmptySession() = runBlocking {
        session.load(context)
        assertNull(session.userName)
        assertNull(session.idToken)
    }
}
