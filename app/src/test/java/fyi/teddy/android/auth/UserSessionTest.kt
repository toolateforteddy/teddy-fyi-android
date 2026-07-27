package fyi.teddy.android.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserSessionTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var tokenStorage: TokenStorage
    private lateinit var userSession: UserSession

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_session_" + UUID.randomUUID().toString()) }
        )
        tokenStorage = TokenStorage(testDataStore)
        userSession = UserSession(tokenStorage)
        AuthTelemetry.clear()
    }

    @Test
    fun testUpdateTokensSyncsWithTokenStorage() {
        userSession.updateTokens("access_123", "refresh_456")

        assertEquals("access_123", userSession.accessToken)
        assertEquals("refresh_456", userSession.refreshToken)
        assertTrue(userSession.isLoggedIn)
    }

    @Test
    fun testSaveAndLoadSession() = runBlocking {
        userSession.userId = "user_007"
        userSession.userName = "Teddy Bear"
        userSession.updateTokens("acc_tok", "ref_tok")

        userSession.save(context, testDataStore)

        val restoredSession = UserSession(tokenStorage)
        restoredSession.load(context, testDataStore)

        assertEquals("user_007", restoredSession.userId)
        assertEquals("Teddy Bear", restoredSession.userName)
        assertEquals("acc_tok", restoredSession.accessToken)
        assertEquals("ref_tok", restoredSession.refreshToken)
    }

    @Test
    fun testClearSessionPreservesClientUuid() = runBlocking {
        userSession.userId = "user_to_clear"
        userSession.clientUuid = "uuid_preserve_123"
        userSession.updateTokens("acc", "ref")

        userSession.save(context, testDataStore)
        userSession.clear(context, testDataStore, reason = "Test clear")

        assertNull(userSession.userId)
        assertNull(userSession.accessToken)
        assertFalse(userSession.isLoggedIn)
        assertEquals("uuid_preserve_123", userSession.clientUuid)
    }
}