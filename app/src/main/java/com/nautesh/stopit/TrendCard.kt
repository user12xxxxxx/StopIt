package com.nautesh.stopit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.nautesh.stopit.ui.theme.LocalSemanticColors

/** Home's trend chart: walked away (green) and opened anyway (red) over the last week or four weeks. */
@Composable
fun TrendCard(history: Map<LocalDate, DayCount>) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val points = if (monthly) lastWeeks(history, today) else lastDays(history, today)
    val labels = points.map { (day, _) ->
        if (monthly) "${day.dayOfMonth} ${day.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
        else day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
    var picked by rememberSaveable(monthly) { mutableStateOf(points.lastIndex) }
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticColors.current

    Surface(color = colors.surfaceContainerLow, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (monthly) "Last 4 weeks" else "Last 7 days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                RangeButton("Week", !monthly, RoundedCornerShape(16.dp, 6.dp, 6.dp, 16.dp)) { monthly = false }
                RangeButton("Month", monthly, RoundedCornerShape(6.dp, 16.dp, 16.dp, 6.dp), Modifier.padding(start = 2.dp)) { monthly = true }
            }

            // The readout doubles as the legend: each value sits beside its line's swatch.
            val (_, sel) = points[picked]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val style = MaterialTheme.typography.bodySmall
                Text("${labels[picked]} ·", style = style, color = colors.onSurfaceVariant)
                Swatch(semantic.walkedAwayLine)
                Text("${sel.walkAways} walked away ·", style = style, color = colors.onSurfaceVariant)
                Swatch(semantic.openedLine)
                Text("${sel.opened} opened", style = style, color = colors.onSurfaceVariant)
            }

            // Lines draw in left to right whenever the range changes; the markers glide between picked points.
            val reveal = remember(monthly) { Animatable(0f) }
            LaunchedEffect(monthly) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            val pick = remember(monthly) { Animatable(picked.toFloat()) }
            LaunchedEffect(picked) { pick.animateTo(picked.toFloat(), spring(dampingRatio = 0.8f, stiffness = 400f)) }

            Box(Modifier.fillMaxWidth().height(88.dp)) {
                val top = points.maxOf { maxOf(it.second.walkAways, it.second.opened) }.coerceAtLeast(1)
                Canvas(Modifier.fillMaxSize()) {
                    val step = size.width / points.size
                    // Points sit mid-column; 6dp headroom keeps the markers inside the box.
                    fun at(i: Int, v: Int) = Offset(step * (i + 0.5f), size.height - 6.dp.toPx() - v / top.toFloat() * (size.height - 12.dp.toPx()))
                    drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    for ((color, value) in listOf<Pair<Color, (DayCount) -> Int>>(semantic.walkedAwayLine to { it.walkAways }, semantic.openedLine to { it.opened })) {
                        val path = Path()
                        points.forEachIndexed { i, (_, c) ->
                            val o = at(i, value(c))
                            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                        }
                        clipRect(right = size.width * reveal.value) {
                            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        // Marker on the picked point, ringed in the card colour; between points it rides the line.
                        val f = pick.value.coerceIn(0f, points.lastIndex.toFloat())
                        val i0 = f.toInt()
                        val i1 = minOf(i0 + 1, points.lastIndex)
                        val m = lerp(at(i0, value(points[i0].second)), at(i1, value(points[i1].second)), f - i0)
                        drawCircle(colors.surfaceContainerLow, 7.dp.toPx(), m)
                        drawCircle(color, 5.dp.toPx(), m)
                    }
                }
                // One tap column per point.
                Row(Modifier.fillMaxSize()) {
                    points.forEachIndexed { i, (_, c) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (i == picked) colors.onSurface.copy(alpha = 0.06f) else Color.Transparent)
                                .clickable { picked = i }
                                .semantics { contentDescription = "${labels[i]}: ${c.walkAways} walked away, ${c.opened} opened anyway" },
                        )
                    }
                }
            }
            Row {
                labels.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(width = 12.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
}

@Composable
private fun RangeButton(label: String, selected: Boolean, shape: RoundedCornerShape, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .height(32.dp)
            .clip(shape)
            .background(animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerHigh, label = "range").value)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurface, label = "rangeText").value) }
}
