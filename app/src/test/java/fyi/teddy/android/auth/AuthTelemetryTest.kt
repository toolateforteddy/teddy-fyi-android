package fyi.teddy.android.auth

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthTelemetryTest {

    @Before
    fun setup() {
        AuthTelemetry.clear()
    }

    @Test
    fun testTokenMasking() {
        assertEquals("null", AuthTelemetry.maskToken(null))
        assertEquals("empty", AuthTelemetry.maskToken(""))
        assertEquals("empty", AuthTelemetry.maskToken("   "))
        assertEquals("***", AuthTelemetry.maskToken("1234567890"))
        assertEquals("abcdef...7890", AuthTelemetry.maskToken("abcdefghijklmnopqrstuvwxyz1234567890"))
    }

    @Test
    fun testCircularBufferLimit() {
        for (i in 1..40) {
            AuthTelemetry.logBreadcrumb("ACTION_$i", "Details $i")
        }

        val breadcrumbs = AuthTelemetry.getBreadcrumbs()
        assertEquals(30, breadcrumbs.size)
        assertTrue(breadcrumbs.first().contains("ACTION_11"))
        assertTrue(breadcrumbs.last().contains("ACTION_40"))
    }

    @Test
    fun testFlushBreadcrumbsDoesNotCrash() {
        AuthTelemetry.logBreadcrumb("LOGIN_INIT", "Starting login flow")
        AuthTelemetry.logBreadcrumb("TOKEN_STORED", "Token stored successfully")
        
        AuthTelemetry.flushBreadcrumbs("Test session cleanup trigger")
        assertEquals(2, AuthTelemetry.getBreadcrumbs().size)
    }
}
