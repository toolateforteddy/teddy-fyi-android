package fyi.teddy.android.grocery

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fyi.teddy.android.grocery.ui.theme.GroceryDensity
import fyi.teddy.android.grocery.ui.theme.GroceryDisplayPreferences
import fyi.teddy.android.grocery.ui.theme.metricsFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroceryDisplayPreferencesTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("grocery_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        GroceryDisplayPreferences.resetForTest()
    }

    @After
    fun tearDown() {
        GroceryDisplayPreferences.resetForTest()
    }

    @Test
    fun `a device that has never chosen gets the default density`() {
        GroceryDisplayPreferences.load(application)

        assertEquals(GroceryDensity.Default, GroceryDisplayPreferences.density.value)
    }

    @Test
    fun `a chosen density survives a restart`() {
        GroceryDisplayPreferences.setDensity(application, GroceryDensity.ACROSS_THE_KITCHEN)
        GroceryDisplayPreferences.resetForTest()

        GroceryDisplayPreferences.load(application)

        assertEquals(GroceryDensity.ACROSS_THE_KITCHEN, GroceryDisplayPreferences.density.value)
    }

    @Test
    fun `choosing a density publishes it without waiting for a reload`() {
        GroceryDisplayPreferences.load(application)

        GroceryDisplayPreferences.setDensity(application, GroceryDensity.COMPACT)

        assertEquals(GroceryDensity.COMPACT, GroceryDisplayPreferences.density.value)
    }

    @Test
    fun `a stored value nobody recognises falls back to the default`() {
        application.getSharedPreferences("grocery_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("display_density", "enormous")
            .commit()

        GroceryDisplayPreferences.load(application)

        assertEquals(GroceryDensity.Default, GroceryDisplayPreferences.density.value)
    }

    @Test
    fun `a value stored under the wrong type is discarded rather than crashing`() {
        application.getSharedPreferences("grocery_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("display_density", 2)
            .commit()

        GroceryDisplayPreferences.load(application)

        assertEquals(GroceryDensity.Default, GroceryDisplayPreferences.density.value)
    }

    @Test
    fun `asking for the flow loads the stored choice`() {
        GroceryDisplayPreferences.setDensity(application, GroceryDensity.COMPACT)
        GroceryDisplayPreferences.resetForTest()

        val flow = GroceryDisplayPreferences.densityIn(application)

        assertEquals(GroceryDensity.COMPACT, flow.value)
    }

    @Test
    fun `every density resolves to its own sizes, ordered smallest to largest`() {
        val compact = metricsFor(GroceryDensity.COMPACT)
        val comfortable = metricsFor(GroceryDensity.COMFORTABLE)
        val roomy = metricsFor(GroceryDensity.ACROSS_THE_KITCHEN)

        assertTrue(compact.tileHeight < comfortable.tileHeight)
        assertTrue(comfortable.tileHeight < roomy.tileHeight)
        assertTrue(compact.minTileWidth < comfortable.minTileWidth)
        assertTrue(comfortable.minTileWidth < roomy.minTileWidth)
        assertTrue(compact.itemFontSize.value < comfortable.itemFontSize.value)
        assertTrue(comfortable.itemFontSize.value < roomy.itemFontSize.value)
    }

    @Test
    fun `storage keys are stable and unique`() {
        val keys = GroceryDensity.entries.map { it.storageKey }

        assertEquals(keys.size, keys.toSet().size)
        assertEquals(
            listOf("compact", "comfortable", "across_the_kitchen"),
            keys,
        )
        GroceryDensity.entries.forEach { density ->
            assertEquals(density, GroceryDensity.fromStorageKey(density.storageKey))
        }
    }
}
