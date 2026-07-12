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
import org.robolectric.annotation.Config
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserSessionTest {

    private lateinit var context: Context
    private lateinit var session: UserSession
    private lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = UserSession()
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_datastore_" + java.util.UUID.randomUUID().toString()) }
        )
    }

    @Test
    fun saveAndLoadSession() = runBlocking {
        session.userName = "Teddy"
        session.idToken = "token123"
        session.profilePictureUri = "https://example.com/pic.jpg"
        
        session.save(context, testDataStore)
        
        val newSession = UserSession()
        newSession.load(context, testDataStore)
        
        assertEquals("Teddy", newSession.userName)
        assertEquals("token123", newSession.idToken)
        assertEquals("https://example.com/pic.jpg", newSession.profilePictureUri)
    }

    @Test
    fun clearSession() = runBlocking {
        session.userName = "Teddy"
        session.save(context, testDataStore)
        
        session.clear(context, testDataStore)
        
        assertNull(session.userName)
        assertNull(session.idToken)
        
        val loadedSession = UserSession()
        loadedSession.load(context, testDataStore)
        assertNull(loadedSession.userName)
    }

    @Test
    fun loadEmptySession() = runBlocking {
        session.load(context, testDataStore)
        assertNull(session.userName)
        assertNull(session.idToken)
    }
}
