package com.nautesh.stopit

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

@Composable
fun PauseSettingsScreen(prefs: Prefs, onPreview: () -> Unit) {
    var range by remember { mutableStateOf(prefs.minSeconds.toFloat()..prefs.maxSeconds.toFloat()) }
    var message by remember { mutableStateOf(prefs.message) }
    var amoled by remember { mutableStateOf(prefs.amoled) }
    val min = range.start.roundToInt()
    val max = range.endInclusive.roundToInt()
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = NavBarClearance + 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text("Pause length", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Each pause picks a random length in this range, so you never get used to it.",
                    color = colors.onSurfaceVariant,
                )
            }

            Surface(color = colors.tertiaryContainer, shape = RoundedCornerShape(36.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$min–$max sec",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onTertiaryContainer,
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
                            thumbColor = colors.tertiary,
                            activeTrackColor = colors.tertiary,
                            inactiveTrackColor = colors.tertiary.copy(alpha = 0.24f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1 s", style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer)
                        Text("30 s", style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Custom message: first row of the settings group, a white field with a pencil, as in the mockup.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp))
                        .background(colors.surfaceContainerLow)
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
                                        .background(colors.surface, RoundedCornerShape(12.dp))
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
                        .background(colors.surfaceContainerLow)
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
                WipRow("Hold to continue", "Press and hold to open the app", RoundedCornerShape(6.dp))
                WipRow("Breathing guide", "Shape grows and shrinks with breath", RoundedCornerShape(6.dp))
                WipRow("Strict mode", "No skipping the pause", RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp))
            }
        }

        Button(
            onClick = onPreview,
            shape = RoundedCornerShape(28.dp, 28.dp, 8.dp, 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = NavBarClearance)
                .fillMaxWidth()
                .height(60.dp),
        ) { Text("Preview pause screen", fontWeight = FontWeight.Bold) }
    }
}

/** A planned setting: shown greyed out with a "Coming soon" tag, and can't be switched on yet. */
@Composable
private fun WipRow(title: String, detail: String, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow)
            .alpha(0.55f)
            .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Coming soon",
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
