package com.nautesh.stopit

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** Haptic for every switch: a firmer click turning on, a softer one turning off. */
fun HapticFeedback.toggle(on: Boolean) =
    performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
