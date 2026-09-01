package fyi.teddy.android.todo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour slots for the Todo app.
 *
 * Everything the Todo UI paints goes through one of these names, so a screen never
 * has to know that "overdue" happens to be `#FF4B4B`. Material's own [androidx.compose.material3.ColorScheme]
 * covers the generic slots (primary/surface/error/...); this covers the ones Material
 * has no name for.
 */
@Immutable
data class TodoColors(
    /** Full-bleed screen background, behind scaffolds and bars. */
    val screen: Color,
    /** Background of a single todo row. */
    val row: Color,
    /** Raised chrome: input bar, date picker, menus. */
    val panel: Color,
    /** A row inside a dialog or reorder list. */
    val panelRaised: Color,
    /** Dialog container. */
    val dialog: Color,
    /** The one accent that means "active / selected / on". */
    val accent: Color,
    /** Hairlines and inactive outlines. */
    val outline: Color,
    /** Primary text. */
    val onSurface: Color,
    /** Secondary text and inactive icons. */
    val onSurfaceMuted: Color,
    /** Tertiary text, placeholders, disabled arrows. */
    val onSurfaceFaint: Color,
    /** Completed-item text. */
    val onSurfaceDone: Color,
    /** Highest priority marker. */
    val priorityHigh: Color,
    /** Middle priority marker, also the recurring-task glyph. */
    val priorityMedium: Color,
    /** Overdue dates and destructive actions. */
    val danger: Color,
    /** Completion confirmation. */
    val success: Color,
    /** Frame around the tactical panels on the home screen. */
    val chassis: Color,
    /** Banner strip at the top of a tactical panel. */
    val chassisBanner: Color,
    /** Text on a [chassisBanner]. */
    val chassisLabel: Color,
    /** Fill at the centre of an empty hex cell. */
    val hexWell: Color,
    /** Faint honeycomb guide lines behind the hex grid. */
    val gridLine: Color,
    /** Per-item identity ramp, indexed by item position. */
    val signalRamp: List<Color>,
) {
    /** Stable per-item accent so the same todo keeps its colour across the grid and widget. */
    fun signalFor(index: Int): Color = signalRamp[index.mod(signalRamp.size)]
}

private val TodoDarkColors = TodoColors(
    screen = TodoPalette.Void,
    row = TodoPalette.Abyss,
    panel = TodoPalette.Panel,
    panelRaised = TodoPalette.PanelRaised,
    dialog = TodoPalette.Dialog,
    accent = TodoPalette.NeonTeal,
    outline = TodoPalette.Indigo,
    onSurface = TodoPalette.Ink,
    onSurfaceMuted = TodoPalette.InkDim,
    onSurfaceFaint = TodoPalette.InkFaint,
    onSurfaceDone = TodoPalette.InkMuted,
    priorityHigh = TodoPalette.Gold,
    priorityMedium = TodoPalette.Amber,
    danger = TodoPalette.Alert,
    success = TodoPalette.Verdant,
    chassis = TodoPalette.Chassis,
    chassisBanner = TodoPalette.Banner,
    chassisLabel = TodoPalette.Stone,
    hexWell = TodoPalette.HexWell,
    gridLine = TodoPalette.GridLine,
    signalRamp = listOf(
        TodoPalette.SignalCyan,
        TodoPalette.SignalPink,
        TodoPalette.SignalPurple,
        TodoPalette.SignalLime,
        TodoPalette.SignalOrange,
        TodoPalette.SignalYellow,
    ),
)

private val TodoDarkColorScheme = darkColorScheme(
    primary = TodoPalette.NeonTeal,
    onPrimary = TodoPalette.Void,
    secondary = TodoPalette.Indigo,
    onSecondary = TodoPalette.Ink,
    tertiary = TodoPalette.Amber,
    onTertiary = TodoPalette.Void,
    background = TodoPalette.Void,
    onBackground = TodoPalette.Ink,
    surface = TodoPalette.Abyss,
    onSurface = TodoPalette.Ink,
    surfaceVariant = TodoPalette.Panel,
    onSurfaceVariant = TodoPalette.InkDim,
    surfaceContainerLowest = TodoPalette.Void,
    surfaceContainerLow = TodoPalette.Abyss,
    surfaceContainer = TodoPalette.Panel,
    surfaceContainerHigh = TodoPalette.PanelRaised,
    surfaceContainerHighest = TodoPalette.PanelRaised,
    outline = TodoPalette.Indigo,
    outlineVariant = TodoPalette.InkFaint,
    error = TodoPalette.Alert,
    onError = TodoPalette.Void,
    errorContainer = TodoPalette.AlertSurface,
    onErrorContainer = TodoPalette.Alert,
)

private val LocalTodoColors = staticCompositionLocalOf { TodoDarkColors }

/**
 * Wraps the Todo app's UI. Sits inside the host [fyi.teddy.android.ui.theme.TeddyTheme]
 * and overrides it entirely, so Todo screens look the same wherever they are hosted.
 */
@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTodoColors provides TodoDarkColors) {
        MaterialTheme(
            colorScheme = TodoDarkColorScheme,
            content = content,
        )
    }
}

/** Access point for [TodoColors]; mirrors how [MaterialTheme] exposes its own scheme. */
object TodoTheme {
    val colors: TodoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTodoColors.current

    /**
     * Non-composable palette access for surfaces that cannot read a CompositionLocal,
     * such as the Canvas-backed home-screen widget.
     */
    val staticColors: TodoColors get() = TodoDarkColors
}
