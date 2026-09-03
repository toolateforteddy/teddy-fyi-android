package fyi.teddy.android.grocery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Raw colour values for the Grocery app's "bronze" look: warm metallic accents on
 * neutral dark surfaces, matching the bronze token on the home screen.
 *
 * This palette is deliberately self-contained. The Todo app keeps its own in
 * [fyi.teddy.android.todo.ui.theme], and the two must never share constants --
 * the apps are intended to be split into separate modules later, and a shared
 * palette would have to be untangled first.
 *
 * Nothing outside this package should reference these values directly; read them
 * through [GroceryTheme.colors] so the mapping stays in one place.
 */
internal object GroceryPalette {
    val Void = Color(0xFF000000)
    val Well = Color(0xFF0A0A0A)
    val Card = Color(0xFF1A1A1A)
    val CardRaised = Color(0xFF1E1E1E)
    val Dialog = Color(0xFF121212)

    val Bronze = Color(0xFFBCA085)
    val BronzeBright = Color(0xFFE6D5C3)
    val BronzeDeep = Color(0xFF8B6C57)
    val BronzeShadow = Color(0xFF6E5241)
    val BronzeEmber = Color(0xFF4C362B)
    val BronzeDark = Color(0xFF1F120B)
    val BronzeInk = Color(0xFF100907)

    val Ink = Color(0xFFFFFFFF)
    val InkDim = Color(0xFFB0AAA3)
    val InkMuted = Color(0xFF8A837B)
    val InkFaint = Color(0xFF44403B)

    // Aisle tints: one muted hue per category, picked by category id so a category
    // keeps the same colour between sessions. Deliberately desaturated so they read as
    // signage next to the bronze accent rather than competing with it.
    val AisleSage = Color(0xFF7FA07A)
    val AisleRust = Color(0xFFC4744A)
    val AisleSlate = Color(0xFF6E86B5)
    val AislePlum = Color(0xFF9A6E9E)
    val AisleAmber = Color(0xFFD2A24C)
    val AisleTeal = Color(0xFF5E9A94)

    val Verdant = Color(0xFF388E3C)
    val Crimson = Color(0xFFD32F2F)
    val CrimsonSurface = Color(0xFF3A1512)
    val CrimsonInk = Color(0xFFFFB4A8)
    val Saffron = Color(0xFFE0B341)

    // Indigo shopping-cart glyph engraved on the bronze token: deliberate cool contrast.
    val CartBody = Color(0xFF1E1A3C)
    val CartGrid = Color(0xFF6E68B5)
    val CartOutline = Color(0xFF6158A7)
}

/**
 * ARGB ints for the Grocery home-screen widget.
 *
 * The widget draws to a [android.graphics.Canvas], which takes packed ints rather than
 * Compose [Color]s and runs outside any composition, so it cannot read
 * [GroceryTheme.colors]. Keep these in step with [GroceryPalette] -- they are the same
 * look, in the form the canvas needs.
 */
object GroceryWidgetPalette {
    val chassis = GroceryPalette.BronzeDark.toArgb()
    val chassisEdge = GroceryPalette.Bronze.toArgb()
    val header = GroceryPalette.BronzeEmber.toArgb()
    val title = GroceryPalette.BronzeBright.toArgb()
    val label = GroceryPalette.Bronze.toArgb()
    val subLabel = GroceryPalette.BronzeShadow.toArgb()
    val badge = GroceryPalette.Saffron.toArgb()
    val ink = GroceryPalette.Ink.toArgb()
}
