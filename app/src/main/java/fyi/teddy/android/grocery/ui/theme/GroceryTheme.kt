package fyi.teddy.android.grocery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour slots for the Grocery app.
 *
 * Everything the Grocery UI paints goes through one of these names, so a screen never
 * has to know that "price" happens to be `#E0B341`. Material's own
 * [androidx.compose.material3.ColorScheme] covers the generic slots
 * (primary/surface/error/...); this covers the ones Material has no name for.
 */
@Immutable
data class GroceryColors(
    /** Full-bleed screen background, behind scaffolds and bars. */
    val screen: Color,
    /** Background of an item card or list row. */
    val card: Color,
    /** A row inside a dialog or reorder list. */
    val cardRaised: Color,
    /** Recessed area: text fields and item wells inside a card. */
    val well: Color,
    /** Dialog container. */
    val dialog: Color,
    /** The one accent that means "active / selected / on". */
    val accent: Color,
    /** Brighter accent for text and icons sitting on dark surfaces. */
    val accentBright: Color,
    /** Hairlines and inactive outlines. */
    val outline: Color,
    /** Primary text. */
    val onSurface: Color,
    /** Secondary text and inactive icons. */
    val onSurfaceMuted: Color,
    /** Tertiary text, placeholders, disabled arrows. */
    val onSurfaceFaint: Color,
    /** Bought / crossed-off item text. */
    val onSurfaceDone: Color,
    /** Prices and unit hints. */
    val price: Color,
    /**
     * One muted hue per category, used for aisle signage and the tint edge on item
     * tiles. Index into it with [fyi.teddy.android.grocery.ui.components.aisleTint]
     * rather than by hand, so a category keeps its colour everywhere it appears.
     */
    val aisleTints: List<Color>,
    /** Confirmation snackbars and success states. */
    val success: Color,
    /** Delete actions and error snackbars. */
    val danger: Color,
    /** Fill behind a destructive affordance, e.g. the swipe-to-delete backdrop. */
    val dangerSurface: Color,
    /** Text and icons on a [dangerSurface] fill. */
    val onDangerSurface: Color,
    /** Text and icons drawn on top of [success] / [danger] fills. */
    val onStatus: Color,
    /** Warm gradient used by the bronze token: centre then edge. */
    val tokenFace: List<Color>,
    /** Metallic rim of the bronze token: highlight then shadow. */
    val tokenRim: List<Color>,
    /** Body of the shopping-cart glyph on the bronze token. */
    val cartBody: Color,
    /** Grid inside the cart basket. */
    val cartGrid: Color,
    /** Cart outline, handle and wheels. */
    val cartOutline: Color,
    /** Engraved detail on the bronze token. */
    val tokenEngraving: Color,
    /** Drop shadow behind engraved token text. */
    val tokenShadow: Color,
)

private val GroceryDarkColors = GroceryColors(
    screen = GroceryPalette.Void,
    card = GroceryPalette.Card,
    cardRaised = GroceryPalette.CardRaised,
    well = GroceryPalette.Well,
    dialog = GroceryPalette.Dialog,
    accent = GroceryPalette.Bronze,
    accentBright = GroceryPalette.BronzeBright,
    outline = GroceryPalette.InkFaint,
    onSurface = GroceryPalette.Ink,
    onSurfaceMuted = GroceryPalette.InkDim,
    onSurfaceFaint = GroceryPalette.InkFaint,
    onSurfaceDone = GroceryPalette.InkMuted,
    price = GroceryPalette.Saffron,
    aisleTints = listOf(
        GroceryPalette.AisleSage,
        GroceryPalette.AisleRust,
        GroceryPalette.AisleSlate,
        GroceryPalette.AislePlum,
        GroceryPalette.AisleAmber,
        GroceryPalette.AisleTeal,
    ),
    success = GroceryPalette.Verdant,
    danger = GroceryPalette.Crimson,
    dangerSurface = GroceryPalette.CrimsonSurface,
    onDangerSurface = GroceryPalette.CrimsonInk,
    onStatus = GroceryPalette.Ink,
    tokenFace = listOf(GroceryPalette.BronzeEmber, GroceryPalette.BronzeDark),
    tokenRim = listOf(GroceryPalette.Bronze, GroceryPalette.BronzeShadow),
    cartBody = GroceryPalette.CartBody,
    cartGrid = GroceryPalette.CartGrid,
    cartOutline = GroceryPalette.CartOutline,
    tokenEngraving = GroceryPalette.BronzeDeep,
    tokenShadow = GroceryPalette.BronzeInk,
)

private val GroceryDarkColorScheme = darkColorScheme(
    primary = GroceryPalette.Bronze,
    onPrimary = GroceryPalette.BronzeInk,
    secondary = GroceryPalette.BronzeDeep,
    onSecondary = GroceryPalette.Ink,
    tertiary = GroceryPalette.Saffron,
    onTertiary = GroceryPalette.BronzeInk,
    background = GroceryPalette.Void,
    onBackground = GroceryPalette.Ink,
    surface = GroceryPalette.Card,
    onSurface = GroceryPalette.Ink,
    surfaceVariant = GroceryPalette.CardRaised,
    onSurfaceVariant = GroceryPalette.InkDim,
    surfaceContainerLowest = GroceryPalette.Void,
    surfaceContainerLow = GroceryPalette.Well,
    surfaceContainer = GroceryPalette.Card,
    surfaceContainerHigh = GroceryPalette.CardRaised,
    surfaceContainerHighest = GroceryPalette.CardRaised,
    outline = GroceryPalette.InkFaint,
    outlineVariant = GroceryPalette.InkFaint,
    error = GroceryPalette.Crimson,
    onError = GroceryPalette.Ink,
    errorContainer = GroceryPalette.CrimsonSurface,
    onErrorContainer = GroceryPalette.CrimsonInk,
)

private val LocalGroceryColors = staticCompositionLocalOf { GroceryDarkColors }

/**
 * Wraps the Grocery app's UI. Sits inside the host [fyi.teddy.android.ui.theme.TeddyTheme]
 * and overrides it entirely, so Grocery screens look the same wherever they are hosted.
 */
@Composable
fun GroceryTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGroceryColors provides GroceryDarkColors) {
        MaterialTheme(
            colorScheme = GroceryDarkColorScheme,
            content = content,
        )
    }
}

/** Access point for [GroceryColors]; mirrors how [MaterialTheme] exposes its own scheme. */
object GroceryTheme {
    val colors: GroceryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGroceryColors.current

    /**
     * Non-composable palette access for surfaces that cannot read a CompositionLocal,
     * such as the Canvas-backed home-screen widget.
     */
    val staticColors: GroceryColors get() = GroceryDarkColors
}
