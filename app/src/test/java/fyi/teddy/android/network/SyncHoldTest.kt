package fyi.teddy.android.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncHoldTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        SyncHold.release(context)
    }

    @After
    fun tearDown() {
        SyncHold.release(context)
    }

    @Test
    fun `isHeld returns false initially`() {
        assertFalse(SyncHold.isHeld())
    }

    @Test
    fun `hold activates sync hold and release clears it`() {
        SyncHold.hold(context, 10_000L)
        assertTrue(SyncHold.isHeld())

        SyncHold.release(context)
        assertFalse(SyncHold.isHeld())
    }

    @Test
    fun `hold extends existing hold if new duration is longer`() {
        SyncHold.hold(context, 5_000L)
        assertTrue(SyncHold.isHeld())

        SyncHold.hold(context, 20_000L)
        assertTrue(SyncHold.isHeld())
    }
}
