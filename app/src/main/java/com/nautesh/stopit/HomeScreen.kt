package com.nautesh.stopit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import android.graphics.fonts.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.stopit.ui.theme.lobedShape
import com.nautesh.stopit.ui.theme.LocalSemanticColors
import com.nautesh.stopit.ui.theme.LocalAppColors

@Composable
fun HomeScreen(prefs: Prefs, theme: ThemeMode, onTheme: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    var guarding by remember { mutableStateOf(prefs.guarding) }
    var settingsOpen by remember { mutableStateOf(false) }
    if (settingsOpen) SettingsSheet(theme, onTheme, onDismiss = { settingsOpen = false })
    // The saved set can name apps that aren't installed (the defaults), so count installed ones only.
    val appCount = prefs.guarded.count { context.packageManager.getLaunchIntentForPackage(it) != null }
    val colors = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    val haptics = LocalHapticFeedback.current
    // One 0..1 progress drives the whole on/off morph so colours, blob and type move together.
    val on by animateFloatAsState(if (guarding) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "guard")
    val cardColor = lerp(app.cardOff, app.cardOn, on)
    val onCard = lerp(app.cardOffText, app.cardOnText, on)

    // The blob turns slowly while the guard is on and stops where it is when switched off.
    val spin = remember { Animatable(0f) }
    LaunchedEffect(guarding) {
        while (guarding) spin.animateTo(spin.value + 360f, tween(40_000, easing = LinearEasing))
    }

    val header = @Composable {
        Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "StopIt",
                fontSize = 52.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = app.screenAccent,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { settingsOpen = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = app.field),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", tint = colors.onSurface)
            }
        }
    }

    val guardCard = @Composable {
        Surface(
            color = cardColor,
            contentColor = onCard,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 196.dp)) {
                Box(
                    Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 56.dp, y = (-56).dp)
                        .rotate(spin.value)
                        // Faint and tonal when off; a solid accent when on.
                        .background(lerp(onCard.copy(alpha = 0.06f), app.cardMark, on), lobedShape(lobes = 12, depth = 0.06f)),
                )
                // Tighter in landscape so today's counts fit below the card.
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(if (isLandscape()) 12.dp else 28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (guarding) "Guard is on" else "Guard is off",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (guarding) FontWeight.SemiBold else FontWeight.Normal,
                            color = lerp(app.cardOffText, app.cardOnBadgeText, on),
                            modifier = Modifier
                                .background(lerp(app.cardOffBadge, app.cardOnBadge, on), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        Box(Modifier.weight(1f))
                        Switch(
                            checked = guarding,
                            onCheckedChange = {
                                haptics.toggle(it)
                                guarding = it
                                prefs.guarding = it
                                if (it) GuardService.start(context) else GuardService.stop(context)
                            },
                            thumbContent = if (guarding) {
                                { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = app.switchOnTrack,
                                checkedThumbColor = app.switchOnThumb,
                                checkedIconColor = app.switchOnTrack,
                            ),
                        )
                    }
                    Column {
                        // Off: the count drops to 0 and the text goes regular weight; the range stays visible.
                        val shown = rollUp(if (guarding) appCount else 0)
                        // Off is thin and narrow, on is heavy and wide, morphing through the font's axes.
                        Text(
                            "${if (shown == 1) "1 app" else "$shown apps"} paused",
                            fontSize = 40.sp,
                            lineHeight = 44.sp,
                            fontFamily = flexFamily(on),
                            maxLines = 1,
                        )
                        Text(
                            "Random pause of ${prefs.minSeconds}–${prefs.maxSeconds} seconds before each one opens",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    val stats = @Composable {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Light tints of the chart's line colours: green is the good outcome, red the one to bring down.
            StatCard(
                value = prefs.walkAwaysToday,
                label = "Walked away today",
                color = LocalSemanticColors.current.walkedAway,
                contentColor = LocalSemanticColors.current.onWalkedAway,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatCard(
                value = prefs.openedToday,
                label = "Opened anyway today",
                color = LocalSemanticColors.current.opened,
                contentColor = LocalSemanticColors.current.onOpened,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }

    if (isLandscape()) {
        // Two panes: the guard and today's counts on the left, the title and the trend on the right.
        Row(
            Modifier.padding(top = 16.dp, end = 16.dp, bottom = NavBarClearance),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.width(340.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                guardCard()
                stats()
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                header()
                TrendCard(prefs.history)
            }
        }
    } else {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = NavBarClearance + 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header()
            guardCard()
            stats()
            TrendCard(prefs.history)
        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, color: Color, contentColor: Color, modifier: Modifier) {
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
        modifier = modifier,
    ) {
        // Smaller in landscape so both cards fit under the guard card.
        val landscape = isLandscape()
        Column(Modifier.padding(horizontal = 18.dp, vertical = if (landscape) 10.dp else 16.dp)) {
            val size = if (landscape) 32.sp else 40.sp
            Text("${rollUp(value)}", fontSize = size, lineHeight = size, fontWeight = FontWeight.ExtraBold)
            Text(label, style = if (landscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/** Counts up from 0 when first shown, then eases to each new [value]. */
@Composable
private fun rollUp(value: Int): Int {
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    return animateIntAsState(target, tween(700), label = "rollUp").value
}

/**
 * Roboto Flex from thin and narrow ([on] = 0) to heavy and wide (1). Narrowing comes mostly from XTRA
 * (counter width): Android leaves 'wdth' nearly flat for this font. Each step shares the loaded font
 * file, so building one per animation frame is cheap; [on] is rounded to 40 steps to bound the cache.
 */
@Composable
private fun flexFamily(on: Float): FontFamily {
    val resources = LocalContext.current.resources
    val base = remember { Font.Builder(resources, R.font.roboto_flex).build() }
    val cache = remember { HashMap<Int, FontFamily>() }
    val step = (on * 40).roundToInt()
    return cache.getOrPut(step) {
        val t = step / 40f
        val axes = "'wght' ${250 + 650 * t}, 'wdth' ${25 + 75 * t}, 'XTRA' ${323 + 145 * t}"
        val font = Font.Builder(base).setFontVariationSettings(axes).build()
        FontFamily(Typeface(android.graphics.Typeface.CustomFallbackBuilder(android.graphics.fonts.FontFamily.Builder(font).build()).build()))
    }
}
