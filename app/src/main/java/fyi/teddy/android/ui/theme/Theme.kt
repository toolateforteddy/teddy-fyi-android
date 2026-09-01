package fyi.teddy.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour slots for the Teddy host shell.
 *
 * This app is really two apps sharing a launcher. The shell owns login, home, debug
 * and the odds and ends; the Todo and Grocery apps each override it with their own
 * theme ([fyi.teddy.android.todo.ui.theme.TodoTheme] /
 * [fyi.teddy.android.grocery.ui.theme.GroceryTheme]) as soon as you navigate into them.
 */
@Immutable
data class TeddyColors(
    /** Top of the full-screen background gradient. */
    val screenTop: Color,
    /** Bottom of the full-screen background gradient. */
    val screenBottom: Color,
    /** Card and app-bar background. */
    val panel: Color,
    /** A row or chip inside a panel. */
    val panelRaised: Color,
    /** Recessed panel, e.g. behind a scrolling log. */
    val panelSunken: Color,
    /** Structural borders and dividers. */
    val outline: Color,
    /** The one accent that means "active / live / on". */
    val accent: Color,
    /** Primary text. */
    val onSurface: Color,
    /** Secondary text and inactive icons. */
    val onSurfaceMuted: Color,
    /** Tertiary text, placeholders, disabled controls. */
    val onSurfaceFaint: Color,
    /** Healthy / synced. */
    val success: Color,
    /** Pending / needs attention. */
    val warning: Color,
    /** Failed / destructive. */
    val danger: Color,
    /** Fill behind an error callout. */
    val dangerSurface: Color,
    /** Text on a [dangerSurface] fill. */
    val dangerInk: Color,
    /** Text and icons drawn on top of a status fill. */
    val onStatus: Color,
)

private val TeddyDarkColors = TeddyColors(
    screenTop = TeddyPalette.NightTop,
    screenBottom = TeddyPalette.NightBottom,
    panel = TeddyPalette.Panel,
    panelRaised = TeddyPalette.PanelRaised,
    panelSunken = TeddyPalette.PanelSunken,
    outline = TeddyPalette.Indigo,
    accent = TeddyPalette.Teal,
    onSurface = TeddyPalette.Ink,
    onSurfaceMuted = TeddyPalette.InkDim,
    onSurfaceFaint = TeddyPalette.InkFaint,
    success = TeddyPalette.Verdant,
    warning = TeddyPalette.Saffron,
    danger = TeddyPalette.Crimson,
    dangerSurface = TeddyPalette.CrimsonSurface,
    dangerInk = TeddyPalette.CrimsonInk,
    onStatus = TeddyPalette.Ink,
)

private val TeddyDarkColorScheme = darkColorScheme(
    primary = TeddyPalette.Teal,
    onPrimary = TeddyPalette.NightBottom,
    secondary = TeddyPalette.Indigo,
    onSecondary = TeddyPalette.Ink,
    tertiary = TeddyPalette.Saffron,
    onTertiary = TeddyPalette.NightBottom,
    background = TeddyPalette.NightBottom,
    onBackground = TeddyPalette.Ink,
    surface = TeddyPalette.Panel,
    onSurface = TeddyPalette.Ink,
    surfaceVariant = TeddyPalette.PanelRaised,
    onSurfaceVariant = TeddyPalette.InkDim,
    surfaceContainer = TeddyPalette.Panel,
    surfaceContainerHigh = TeddyPalette.PanelRaised,
    outline = TeddyPalette.Indigo,
    outlineVariant = TeddyPalette.InkFaint,
    error = TeddyPalette.Crimson,
    onError = TeddyPalette.Ink,
)

private val LocalTeddyColors = staticCompositionLocalOf { TeddyDarkColors }

/**
 * Wraps the whole app. Todo and Grocery screens replace this with their own theme;
 * everything else inherits the shell look.
 */
@Composable
fun TeddyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTeddyColors provides TeddyDarkColors) {
        MaterialTheme(
            colorScheme = TeddyDarkColorScheme,
            content = content,
        )
    }
}

/** Access point for [TeddyColors]; mirrors how [MaterialTheme] exposes its own scheme. */
object TeddyTheme {
    val colors: TeddyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTeddyColors.current
}
