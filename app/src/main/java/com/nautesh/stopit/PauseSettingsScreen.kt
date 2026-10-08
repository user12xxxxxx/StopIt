package com.nautesh.stopit

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.nautesh.stopit.ui.theme.LocalAppColors

@Composable
fun PauseSettingsScreen(prefs: Prefs, onPreview: () -> Unit) {
    var range by remember { mutableStateOf(prefs.minSeconds.toFloat()..prefs.maxSeconds.toFloat()) }
    var message by remember { mutableStateOf(prefs.message) }
    var amoled by remember { mutableStateOf(prefs.amoled) }
    val min = range.start.roundToInt()
    val max = range.endInclusive.roundToInt()
    val colors = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    val haptics = LocalHapticFeedback.current
    val landscape = isLandscape()

    val title = @Composable {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text("Pause length", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "Each pause picks a random length in this range, so you never get used to it.",
                color = colors.onSurfaceVariant,
            )
        }
    }

    val rangeCard = @Composable {
        Surface(color = app.cardOn, shape = RoundedCornerShape(36.dp)) {
            // Tighter in landscape, where the left pane has little height.
            Column(Modifier.padding(if (landscape) 16.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$min–$max sec",
                    style = if (landscape) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = app.cardOnText,
                )
                RangeSlider(
                    value = range,
                    onValueChange = {
                        // A tick each time either end lands on a new whole second.
                        if (it.start.roundToInt() != min || it.endInclusive.roundToInt() != max) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                        range = it
                    },
                    onValueChangeFinished = {
                        prefs.minSeconds = min
                        prefs.maxSeconds = max
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = app.cardOnText,
                        activeTrackColor = app.cardOnText,
                        inactiveTrackColor = app.cardOnText.copy(alpha = 0.24f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    modifier = Modifier.padding(top = if (landscape) 8.dp else 16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 s", style = MaterialTheme.typography.labelMedium, color = app.cardOnText)
                    Text("30 s", style = MaterialTheme.typography.labelMedium, color = app.cardOnText)
                }
            }
        }
    }

    val settings = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Custom message: first row of the settings group, a white field with a pencil, as in the mockup.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp))
                    .background(app.row)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Custom message", style = MaterialTheme.typography.titleMedium)
                    BasicTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            prefs.message = it
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .semantics { contentDescription = "Message shown on the pause screen" },
                        decorationBox = { field ->
                            Box(
                                Modifier
                                    .background(colors.surfaceContainerHighest, RoundedCornerShape(12.dp))
                                    .height(40.dp)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) { field() }
                        },
                    )
                }
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    Modifier.padding(bottom = 10.dp).size(20.dp),
                    tint = colors.outline,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(app.row)
                    .toggleable(value = amoled, role = Role.Switch) {
                        haptics.toggle(it)
                        amoled = it
                        prefs.amoled = it
                    }
                    .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AMOLED black", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pure black background on the pause screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Switch(checked = amoled, onCheckedChange = null)
            }
            // Extensions unlock what a normal app can't do: seeing and removing other apps' Recents cards needs the shell
            // user (Shizuku) or root. Each enables every such feature at once.
            WipRow(
                "Enable Shizuku extension",
                "Close also clears the app from Recents, and swiping an app away ends its timer",
                RoundedCornerShape(6.dp),
                tag = "Needs Shizuku",
            )
            WipRow(
                "Enable root extension",
                "The same, through root instead of Shizuku",
                RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp),
                tag = "Needs root",
            )
        }
    }

    val previewButton = @Composable { modifier: Modifier, shape: Shape ->
        Button(
            onClick = onPreview,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = app.fab,
                contentColor = app.fabIcon,
            ),
            modifier = modifier,
        ) { Text("Preview pause screen", fontWeight = FontWeight.Bold) }
    }

    if (landscape) {
        // Two panes: the range and Preview stay put on the left; the settings scroll on the right.
        Row(
            // No bottom padding here: the scrolling panes run under the gesture bar, which floats over them.
            Modifier.padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.width(340.dp).fillMaxHeight().padding(bottom = NavBarClearance), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                title()
                rangeCard()
                Spacer(Modifier.weight(1f))
                previewButton(Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(28.dp))
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)).verticalScroll(rememberScrollState()).padding(bottom = NavBarClearance)) {
                settings()
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = NavBarClearance + 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                title()
                rangeCard()
                settings()
            }
            previewButton(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = NavBarClearance)
                    .fillMaxWidth()
                    .height(60.dp),
                RoundedCornerShape(28.dp, 28.dp, 8.dp, 8.dp),
            )
        }
    }
}

/** A setting that can't be switched on yet: greyed out, with a tag naming the access it needs. */
@Composable
private fun WipRow(title: String, detail: String, shape: Shape, tag: String) {
    val colors = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.row)
            .alpha(0.55f)
            .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    tag,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Switch(checked = false, onCheckedChange = null, enabled = false)
    }
}
