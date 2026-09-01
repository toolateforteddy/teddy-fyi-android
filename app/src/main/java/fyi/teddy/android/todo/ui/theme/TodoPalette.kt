package fyi.teddy.android.todo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Raw colour values for the Todo app's "tactical HUD" look: near-black surfaces,
 * indigo structure, neon-teal accents.
 *
 * This palette is deliberately self-contained. The Grocery app keeps its own in
 * [fyi.teddy.android.grocery.ui.theme], and the two must never share constants --
 * the apps are intended to be split into separate modules later, and a shared
 * palette would have to be untangled first.
 *
 * Nothing outside this package should reference these values directly; read them
 * through [TodoTheme.colors] so the mapping stays in one place.
 */
internal object TodoPalette {
    val Void = Color(0xFF000000)
    val Abyss = Color(0xFF0B0B0F)
    val Panel = Color(0xFF121214)
    val PanelRaised = Color(0xFF1E1E1E)
    val Dialog = Color(0xFF121212)

    val NeonTeal = Color(0xFF00F2FE)
    val Indigo = Color(0xFF3700B3)

    // Tactical chassis: the HUD frame used by the hex grid and the home-screen widget.
    val Chassis = Color(0xFF161424)
    val Banner = Color(0xFF221D38)
    val Stone = Color(0xFFBCADA0)
    val HexBacklog = Color(0xFF2A1F45)
    val HexTask = Color(0xFF1F1C33)
    val HexClear = Color(0xFF1B2A26)
    val HexWell = Color(0xFF12101A)
    val GridLine = Color(0xFF1B182B)

    val Ink = Color(0xFFFFFFFF)
    val InkDim = Color(0xFFB0B0B8)
    val InkMuted = Color(0xFF666666)
    val InkFaint = Color(0xFF3A3A42)

    val Gold = Color(0xFFFFD700)
    val Amber = Color(0xFFFFA500)
    val Alert = Color(0xFFFF4B4B)
    val Verdant = Color(0xFF4CAF50)

    // Per-item identity ramp for hexes, badges and priority stripes.
    val SignalCyan = Color(0xFF03DAC5)
    val SignalPink = Color(0xFFE91E63)
    val SignalPurple = Color(0xFF9C27B0)
    val SignalLime = Color(0xFFCDDC39)
    val SignalOrange = Color(0xFFFF9800)
    val SignalYellow = Color(0xFFFFEB3B)
}

/**
 * Swatches offered when a user picks a colour for a Todo space.
 *
 * These are hex strings rather than [Color]s because the choice is persisted in the
 * database and round-trips through sync. They mirror `TodoColors.signalRamp` so a
 * user-chosen space colour still lands on the Todo palette.
 */
val TodoSpaceSwatches: List<String> = listOf(
    "#03DAC5",
    "#E91E63",
    "#9C27B0",
    "#CDDC39",
    "#FF9800",
    "#FFEB3B",
    "#00F2FE",
)

/**
 * ARGB ints for the Todo home-screen widget.
 *
 * The widget draws to a [android.graphics.Canvas], which takes packed ints rather than
 * Compose [Color]s and runs outside any composition, so it cannot read [TodoTheme.colors].
 * Keep these in step with [TodoPalette] -- they are the same look, in the form the
 * canvas needs.
 */
object TodoWidgetPalette {
    val chassis = TodoPalette.Chassis.toArgb()
    val chassisEdge = TodoPalette.Indigo.toArgb()
    val banner = TodoPalette.Banner.toArgb()
    val bannerText = TodoPalette.Stone.toArgb()
    val accent = TodoPalette.SignalCyan.toArgb()
    val ink = TodoPalette.Ink.toArgb()

    /** Hex holding the backlog count. */
    val hexBacklog = TodoPalette.HexBacklog.toArgb()
    /** Hex holding a scheduled task. */
    val hexTask = TodoPalette.HexTask.toArgb()
    /** Hex shown when there is nothing left to do. */
    val hexClear = TodoPalette.HexClear.toArgb()

    /** Per-item identity ramp, matching `TodoColors.signalRamp`. */
    val signalRamp = intArrayOf(
        TodoPalette.SignalCyan.toArgb(),
        TodoPalette.SignalPink.toArgb(),
        TodoPalette.SignalPurple.toArgb(),
        TodoPalette.SignalLime.toArgb(),
        TodoPalette.SignalOrange.toArgb(),
        TodoPalette.SignalYellow.toArgb(),
    )

    /** Stable per-item accent, matching `TodoColors.signalFor`. */
    fun signalFor(index: Int): Int = signalRamp[index.mod(signalRamp.size)]
}
