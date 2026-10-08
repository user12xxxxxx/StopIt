package com.nautesh.stopit.ui.theme

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

// Colours follow the wallpaper (Material You); minSdk 31 always has dynamic colour.
@Composable
fun StopItTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (dark) {
        dynamicDarkColorScheme(context)
    } else {
        val light = dynamicLightColorScheme(context)
        // A tinted page so rows and cards sit on lighter surfaces. Not primaryContainer: in monochrome or
        // vibrant palettes it can be dark, which loses the dark text.
        light.copy(background = light.surfaceContainerHighest)
    }
    MaterialTheme(colorScheme = colors, shapes = StopItShapes) {
        // Text drawn straight on the page (outside a Surface) would otherwise default to black, unreadable in dark.
        CompositionLocalProvider(
            LocalSemanticColors provides if (dark) DarkSemantic else LightSemantic,
            LocalContentColor provides colors.onBackground,
            content = content,
        )
    }
}

// The pause screen is always dark, so it stands apart from the app it covers.
@Composable
fun PauseTheme(amoled: Boolean = false, content: @Composable () -> Unit) {
    val dark = dynamicDarkColorScheme(LocalContext.current)
    val colors = if (amoled) dark.copy(background = Color.Black, surface = Color.Black) else dark
    MaterialTheme(colorScheme = colors, shapes = StopItShapes, content = content)
}
