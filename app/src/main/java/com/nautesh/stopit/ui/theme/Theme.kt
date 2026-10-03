package com.nautesh.stopit.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
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


// Colours follow the wallpaper (Material You); minSdk 31 always has dynamic colour.
// ponytail: light only, because the fixed greens/reds (stat cards, chart, Saved) assume a light page; add dark with dark variants of those.
@Composable
fun StopItTheme(content: @Composable () -> Unit) {
    val light = dynamicLightColorScheme(LocalContext.current)
    MaterialTheme(
        // A tinted page so rows and cards sit on lighter surfaces. Not primaryContainer: in monochrome or
        // vibrant palettes it can be dark, which loses the dark text.
        colorScheme = light.copy(background = light.surfaceContainerHighest),
        shapes = StopItShapes,
        content = content,
    )
}

// The pause screen is always dark, so it stands apart from the app it covers.
@Composable
fun PauseTheme(amoled: Boolean = false, content: @Composable () -> Unit) {
    val dark = dynamicDarkColorScheme(LocalContext.current)
    val colors = if (amoled) dark.copy(background = Color.Black, surface = Color.Black) else dark
    MaterialTheme(colorScheme = colors, shapes = StopItShapes, content = content)
}
