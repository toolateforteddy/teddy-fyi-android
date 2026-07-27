package fyi.teddy.android.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
class TokenStorageTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var tokenStorage: TokenStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_tokens_" + UUID.randomUUID().toString()) }
        )
        tokenStorage = TokenStorage(testDataStore)
        AuthTelemetry.clear()
    }

    @Test
    fun testSaveAndGetTokens() = runBlocking {
        tokenStorage.saveTokens(context, "access_123456789", "refresh_987654321", testDataStore)

        val access = tokenStorage.getAccessToken(context, testDataStore)
        val refresh = tokenStorage.getRefreshToken(context, testDataStore)

        assertEquals("access_123456789", access)
        assertEquals("refresh_987654321", refresh)
    }

    @Test
    fun testConcurrentReadsAndWrites() {
        runBlocking {
            val jobs = (1..20).map { index ->
                async(Dispatchers.IO) {
                    tokenStorage.saveTokens(context, "access_$index", "refresh_$index", testDataStore)
                    val readAccess = tokenStorage.getAccessToken(context, testDataStore)
                    assertNotNull(readAccess)
                }
            }
            jobs.awaitAll()
        }
    }

    @Test
    fun testClearTokens() = runBlocking {
        tokenStorage.saveTokens(context, "access_123", "refresh_123", testDataStore)
        tokenStorage.clear(context, testDataStore)

        assertNull(tokenStorage.getAccessToken(context, testDataStore))
        assertNull(tokenStorage.getRefreshToken(context, testDataStore))
    }
}
