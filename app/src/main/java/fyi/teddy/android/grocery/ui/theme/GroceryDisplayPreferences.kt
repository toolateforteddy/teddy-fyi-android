package fyi.teddy.android.grocery.ui.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Display preferences that belong to the device rather than to a list: they say how this
 * particular tablet is being looked at, so they are never synced and never scoped to a user.
 *
 * A process-wide flow rather than ViewModel state, because the settings screen and the list
 * screen hold separate [fyi.teddy.android.grocery.ui.GroceryViewModel] instances -- writing
 * through a ViewModel would leave the screen behind it still drawing at the old size.
 */
object GroceryDisplayPreferences {

    /** Shared with GroceryViewModel; one file for everything grocery keeps on the device. */
    private const val PREFS_NAME = "grocery_prefs"
    private const val KEY_DENSITY = "display_density"

    private val _density = MutableStateFlow(GroceryDensity.Default)

    /** The chosen density. Emits [GroceryDensity.Default] until [load] has run. */
    val density: StateFlow<GroceryDensity> = _density.asStateFlow()

    @Volatile
    private var loaded = false

    /** Reads the stored density once per process. Safe to call from every composition. */
    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _density.value = try {
                GroceryDensity.fromStorageKey(prefs.getString(KEY_DENSITY, null))
            } catch (_: ClassCastException) {
                prefs.edit { remove(KEY_DENSITY) }
                GroceryDensity.Default
            }
            loaded = true
        }
    }

    /** Records a new density and pushes it to every Grocery screen currently composed. */
    fun setDensity(context: Context, density: GroceryDensity) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_DENSITY, density.storageKey) }
        loaded = true
        _density.value = density
    }

    /** Test seam: forgets what was read so the next [load] hits preferences again. */
    internal fun resetForTest() {
        loaded = false
        _density.value = GroceryDensity.Default
    }
}
