package fyi.teddy.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw colour values for the Teddy host shell -- login, home, debug and the other
 * screens that are neither Todo nor Grocery.
 *
 * The shell is the container the two apps currently live in. It has its own palette
 * so that splitting Todo and Grocery into separate apps only means deleting this
 * package, not untangling it. Feature palettes live in
 * [fyi.teddy.android.todo.ui.theme] and [fyi.teddy.android.grocery.ui.theme];
 * do not cross-reference between the three.
 *
 * Nothing outside this package should reference these values directly; read them
 * through [TeddyTheme.colors] so the mapping stays in one place.
 */
internal object TeddyPalette {
    val NightTop = Color(0xFF0A0814)
    val NightBottom = Color(0xFF050508)
    val Panel = Color(0xFF161424)
    val PanelRaised = Color(0xFF232135)
    val PanelSunken = Color(0xFF0D0B14)

    val Indigo = Color(0xFF3700B3)
    val Teal = Color(0xFF03DAC5)

    val Ink = Color(0xFFFFFFFF)
    val InkDim = Color(0xFFB0AAB8)
    val InkMuted = Color(0xFF7A7593)
    val InkFaint = Color(0xFF3A3548)

    val Verdant = Color(0xFF4CAF50)
    val Saffron = Color(0xFFE0B341)
    val Crimson = Color(0xFFD32F2F)
    val CrimsonSurface = Color(0xFF321414)
    val CrimsonInk = Color(0xFFFF8A80)
}
