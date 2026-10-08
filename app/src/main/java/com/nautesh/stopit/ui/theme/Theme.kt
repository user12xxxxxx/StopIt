package com.nautesh.stopit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The mockup's scalloped and clover shapes: a circle whose radius waves [lobes] times.
 * [depth] is how far each dip cuts in, as a fraction of the radius.
 */
// ponytail: hand-rolled until material3's MaterialShapes (1.5 alpha) is in use.
fun lobedShape(lobes: Int, depth: Float): Shape = GenericShape { size, _ ->
    val radius = min(size.width, size.height) / 2
    val steps = 360
    for (i in 0 until steps) {
        val t = 2 * PI * i / steps
        val r = radius * (1 - depth + depth * cos(lobes * t))
        val x = size.width / 2 + (r * cos(t - PI / 2)).toFloat()
        val y = size.height / 2 + (r * sin(t - PI / 2)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val StopItShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
)


/** The fixed semantic colours (walked away is green, opened anyway is red), which don't follow the wallpaper. */
class SemanticColors(
    /** Tint and text for "Walked away" and "Saved". */
    val walkedAway: Color,
    val onWalkedAway: Color,
    /** Tint and text for "Opened anyway". */
    val opened: Color,
    val onOpened: Color,
    /**
     * Trend chart lines. The light pair was checked with the dataviz palette validator against the card colour:
     * distinguishable for red-green colour-blind viewers through lightness. The coral is under 3:1 on the card, so
     * the readout row always names both values next to their swatches.
     */
    val walkedAwayLine: Color,
    val openedLine: Color,
    /** Welcome's "Done". */
    val done: Color,
)

private val LightSemantic = SemanticColors(
    walkedAway = Color(0xFFC6ECCB),
    onWalkedAway = Color(0xFF0F3A1A),
    opened = Color(0xFFFFDAD4),
    onOpened = Color(0xFF410001),
    walkedAwayLine = Color(0xFF1E6E40),
    openedLine = Color(0xFFEB6F5C),
    done = Color(0xFF2F6A3E),
)

private val DarkSemantic = SemanticColors(
    walkedAway = Color(0xFF1F4D2B),
    onWalkedAway = Color(0xFFC6ECCB),
    opened = Color(0xFF6B2A21),
    onOpened = Color(0xFFFFDAD4),
    walkedAwayLine = Color(0xFF7DD99A),
    openedLine = Color(0xFFFF8A75),
    done = Color(0xFF8FD6A0),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

/**
 * Where each Material role goes on screen: the component tokens of the stopIt colour system
 * (https://claude.ai/artifact/CnUxxagu8gFFVHVcsHe5qB), as aliases of the current scheme's roles. Screens colour
 * through these, so a different scheme (light, dark, another wallpaper) changes only the roles. Use a role directly
 * only where no token fits, and then as a fill with its on- colour.
 */
@Immutable
data class AppColors(
    /** The background of every screen, and its headlines, supporting text and large accent text (titles, digits). */
    val screen: Color,
    val screenText: Color,
    val screenTextMuted: Color,
    val screenAccent: Color,
    /** A card or hero that is off, with its text and badges. */
    val cardOff: Color,
    val cardOffText: Color,
    val cardOffBadge: Color,
    /** A card or hero that is on, with its text, and badges and small buttons on it (inverted). */
    val cardOn: Color,
    val cardOnText: Color,
    val cardOnBadge: Color,
    val cardOnBadgeText: Color,
    /** Decorative shapes and chips inside a card; text on them in on-surface. */
    val cardMark: Color,
    /** List and settings rows; supporting text in on-surface-variant. */
    val row: Color,
    val rowText: Color,
    /** The floating navbar or rail, and the pill behind the current tab. */
    val nav: Color,
    val navText: Color,
    val navActive: Color,
    val navActiveIcon: Color,
    /** Floating actions (Save, Preview), menu items and the menu trigger; the trigger while its menu is open. */
    val fab: Color,
    val fabIcon: Color,
    val fabOpen: Color,
    val fabOpenIcon: Color,
    /** Bottom sheets, and inputs and secondary buttons on sheets and screens (text in on-surface). */
    val sheet: Color,
    val sheetText: Color,
    val field: Color,
    /** Segments and other single-choice toggles. */
    val toggleOff: Color,
    val toggleOffText: Color,
    val toggleOn: Color,
    val toggleOnText: Color,
    /** A checked switch on [cardOn]. On a light row keep the default (primary track): this one is about 1.2:1 there. */
    val switchOnTrack: Color,
    val switchOnThumb: Color,
    /** A live counter's face while it runs: the pause countdown. */
    val running: Color,
    val runningText: Color,
)

fun appColors(s: ColorScheme) = AppColors(
    screen = s.primaryContainer,
    screenText = s.onPrimaryContainer,
    screenTextMuted = s.onSurfaceVariant,
    screenAccent = s.primary,
    cardOff = s.surfaceContainerLow,
    cardOffText = s.onSurfaceVariant,
    cardOffBadge = s.surfaceContainerHigh,
    cardOn = s.primary,
    cardOnText = s.onPrimary,
    cardOnBadge = s.onPrimary,
    cardOnBadgeText = s.primary,
    cardMark = s.errorContainer,
    row = s.surfaceContainer,
    rowText = s.onSurface,
    nav = s.tertiaryContainer,
    navText = s.onTertiaryContainer,
    navActive = s.onPrimary,
    navActiveIcon = s.primary,
    fab = s.tertiaryContainer,
    fabIcon = s.onTertiaryContainer,
    fabOpen = s.primary,
    fabOpenIcon = s.onPrimary,
    sheet = s.primaryContainer,
    sheetText = s.onPrimaryContainer,
    field = s.surfaceContainerLow,
    toggleOff = s.surfaceContainerLow,
    toggleOffText = s.onSurfaceVariant,
    toggleOn = s.primary,
    toggleOnText = s.onPrimary,
    switchOnTrack = s.onPrimary,
    switchOnThumb = s.primary,
    running = s.tertiaryContainer,
    runningText = s.onTertiaryContainer,
)

val LocalAppColors = staticCompositionLocalOf { appColors(lightColorScheme()) }

/** The scheme with its background and on-background set to the screen's, so Material defaults match [AppColors.screen]. */
private fun ColorScheme.onScreen() = copy(background = primaryContainer, onBackground = onPrimaryContainer)

// Colours follow the wallpaper (Material You); minSdk 31 always has dynamic colour.
@Composable
fun StopItTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).onScreen()
    MaterialTheme(colorScheme = colors, shapes = StopItShapes) {
        // Text drawn straight on the screen (outside a Surface) would otherwise default to black, unreadable in dark.
        CompositionLocalProvider(
            LocalAppColors provides appColors(colors),
            LocalSemanticColors provides if (dark) DarkSemantic else LightSemantic,
            LocalContentColor provides colors.onBackground,
            content = content,
        )
    }
}

// The pause sheet is always dark, so it stands apart from the app it covers.
@Composable
fun PauseTheme(amoled: Boolean = false, content: @Composable () -> Unit) {
    val colors = dynamicDarkColorScheme(LocalContext.current).onScreen()
    // AMOLED: a pure black sheet, with the neutral text that reads on it.
    val app = appColors(colors).let { if (amoled) it.copy(sheet = Color.Black, sheetText = colors.onSurface) else it }
    MaterialTheme(colorScheme = colors, shapes = StopItShapes) {
        CompositionLocalProvider(LocalAppColors provides app, content = content)
    }
}
