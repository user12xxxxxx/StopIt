package com.nautesh.stopit

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.stopit.ui.theme.PauseTheme
import com.nautesh.stopit.ui.theme.lobedShape
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** The pause shown before a guarded app: a bottom sheet over the dimmed app (the window is translucent). */
class PauseActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun start(context: Context, pkg: String) {
            context.startActivity(
                Intent(context, PauseActivity::class.java)
                    .putExtra(EXTRA_PACKAGE, pkg)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The pause screen is always dark, so the system bars need light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // The window theme blurs the app behind and adds a thin dim, both from the first frame; the dim keeps the white
        // status bar icons readable over light apps. With blur off (battery saver, some devices), the dim alone is stronger.
        if (!windowManager.isCrossWindowBlurEnabled) window.setDimAmount(0.6f)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return finish()
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("the app")

        // The Pause length screen previews with our own package; then both buttons just close the preview.
        val preview = pkg == packageName
        val prefs = Prefs(this)
        val close = {
            if (preview) {
                finish()
            } else {
                prefs.recordWalkAway()
                goHome()
            }
        }

        // For the after-the-pause screen. Read once here; both are only shown, never updated live.
        val visits = maxOf(prefs.visitsToday(pkg), 1)
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val minutesToday = getSystemService(UsageStatsManager::class.java)
            .queryAndAggregateUsageStats(startOfDay, System.currentTimeMillis())[pkg]
            ?.totalTimeInForeground?.div(60_000)?.toInt() ?: 0

        setContent {
            PauseTheme(amoled = prefs.amoled) {
                PauseScreen(
                    window = window,
                    // Closing a real pause goes home, so the app stays hidden behind the blur until then.
                    closeRevealsApp = preview,
                    appName = label,
                    minSeconds = prefs.minSeconds,
                    maxSeconds = prefs.maxSeconds,
                    message = prefs.message,
                    visits = visits,
                    minutesToday = minutesToday,
                    onClose = close,
                    onOpenFor = { minutes ->
                        if (!preview) {
                            // No limit: allowed until the user leaves the app. A timed visit lasts its time, in and out of the app.
                            val until = if (minutes == null) PauseGate.NO_LIMIT else System.currentTimeMillis() + minutes * 60_000L
                            GuardService.gate?.allow(pkg, until)
                            prefs.recordOpened()
                        }
                        // The app's task is right below ours, so finishing returns to it as it was. Relaunching
                        // would lose a link opened in a custom tab, which lives in the calling app's task.
                        finish()
                    },
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PauseScreen(
    window: Window,
    closeRevealsApp: Boolean,
    appName: String,
    minSeconds: Int,
    maxSeconds: Int,
    message: String,
    visits: Int,
    minutesToday: Int,
    onClose: () -> Unit,
    onOpenFor: (minutes: Int?) -> Unit,
) {
    // "Open anyway" leads to the after-the-pause screen rather than straight into the app.
    var deciding by rememberSaveable { mutableStateOf(false) }
    val sheet = rememberEntrance()
    val colors = MaterialTheme.colorScheme

    // On the way out the sheet drops; when the app behind is about to show, its blur and dim fade out too.
    val scope = rememberCoroutineScope()
    val blurPx = with(LocalDensity.current) { 32.dp.toPx() }
    var leaving by remember { mutableStateOf(false) }
    fun leave(revealApp: Boolean, then: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            coroutineScope {
                // Accelerates off screen; a spring would crawl through its last few pixels.
                launch { sheet.animateTo(0f, tween(250, easing = FastOutLinearInEasing)) }
                if (revealApp) {
                    val dim = window.attributes.dimAmount
                    launch {
                        animate(1f, 0f, animationSpec = tween(250)) { p, _ ->
                            window.attributes = window.attributes.apply {
                                blurBehindRadius = (p * blurPx).toInt()
                                dimAmount = p * dim
                            }
                        }
                    }
                }
            }
            then()
        }
    }
    val close = { leave(closeRevealsApp, onClose) }
    val openFor = { minutes: Int? -> leave(true) { onOpenFor(minutes) } }
    BackHandler(onBack = close)

    // Landscape is short, so the sheet keeps less padding below its content and around its handle.
    val landscape = isLandscape()
    val bottomPadding = if (landscape) 16.dp else 30.dp
    val handleGap = if (landscape) 12.dp else 20.dp
    // Below the status bar, so landscape's after-the-pause screen knows how tall the sheet may grow.
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().padding(top = 4.dp)) {
        // Sheet padding, the handle and its gap, and the gesture bar.
        val fullHeight = maxHeight - 12.dp - bottomPadding - 4.dp - handleGap - WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                // Capped and centred in landscape, as a wide sheet would stretch its buttons.
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1 - sheet.value) * size.height }
                // Keeps the bottom covered while the spring overshoots upward.
                .drawBehind { drawRect(colors.background, Offset(0f, size.height), size.copy(height = 80.dp.toPx())) }
                .background(colors.background, RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(32.dp, 4.dp).alpha(0.5f).background(colors.onSurfaceVariant, RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(handleGap))
            SheetContent(deciding, fullHeight, appName, minSeconds, maxSeconds, message, visits, minutesToday, close, openFor) { deciding = true }
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun SheetContent(
    deciding: Boolean,
    fullHeight: Dp,
    appName: String,
    minSeconds: Int,
    maxSeconds: Int,
    message: String,
    visits: Int,
    minutesToday: Int,
    onClose: () -> Unit,
    onOpenFor: (minutes: Int?) -> Unit,
    onOpenAnyway: () -> Unit,
) {
    SharedTransitionLayout {
        AnimatedContent(
            deciding,
            transitionSpec = { (slideInVertically { it / 4 } + fadeIn()) togetherWith fadeOut() },
            label = "decide",
        ) { decide ->
            // The tapped "Open anyway" button morphs into the "Open for" button on the next screen.
            val openButton = Modifier.sharedBounds(rememberSharedContentState("open"), this@AnimatedContent)
            if (decide) {
                DecideScreen(fullHeight, appName, visits, minutesToday, onClose, onOpenFor, openButton)
            } else {
                CountdownScreen(appName, minSeconds, maxSeconds, message, onClose, onOpenAnyway, openButton)
            }
        }
    }
}

/** Springs from 0 to 1 once, [delayMs] after it first composes. Read `.value` in a graphicsLayer to skip recomposition. */
@Composable
private fun rememberEntrance(delayMs: Long = 0, dampingRatio: Float = 0.8f, stiffness: Float = 380f): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        progress.animateTo(1f, spring(dampingRatio, stiffness))
    }
    return progress
}

/** The sheet's text and buttons rise 24 dp and fade in, 50 ms apart, once the sheet is on its way up. */
@Composable
private fun Modifier.rise(index: Int): Modifier {
    val p = rememberEntrance(140L + index * 50)
    return graphicsLayer {
        translationY = (1 - p.value) * 24.dp.toPx()
        alpha = p.value.coerceIn(0f, 1f)
    }
}

@Composable
private fun CountdownScreen(
    appName: String,
    minSeconds: Int,
    maxSeconds: Int,
    message: String,
    onClose: () -> Unit,
    onOpenAnyway: () -> Unit,
    openButton: Modifier,
) {
    var left by rememberSaveable { mutableIntStateOf(Random.nextInt(minSeconds, maxSeconds + 1)) }
    val total = rememberSaveable { left }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    val done = left == 0
    val colors = MaterialTheme.colorScheme

    // The scalloped shape breathes (4 s in, 4 s out) and slowly turns; the breathing settles when the pause ends.
    val motion = rememberInfiniteTransition(label = "pause")
    val breath by motion.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val spin by motion.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val scale by animateFloatAsState(if (done) 1f else breath, label = "scale")
    // The shape pops in just after the sheet starts rising.
    val pop = rememberEntrance(80, dampingRatio = 0.6f, stiffness = 800f)

    val headline = if (done) "Still want it?" else "Take a breath."
    val body = if (done) "You made it through the pause. Your call now." else message
    val badge = @Composable {
        Box(
            Modifier.size(168.dp).graphicsLayer {
                val s = (0.6f + 0.4f * pop.value) * scale
                scaleX = s
                scaleY = s
                alpha = pop.value.coerceIn(0f, 1f)
            },
            contentAlignment = Alignment.Center,
        ) {
            // Only the shape turns; the count stays upright on top of it.
            Box(Modifier.matchParentSize().rotate(spin).background(colors.primaryContainer, lobedShape(lobes = 9, depth = 0.08f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    left,
                    transitionSpec = {
                        if (targetState == 0) {
                            // The check pops in with a little overshoot.
                            (scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f), initialScale = 0.4f) + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                        } else {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                        }
                    },
                    contentAlignment = Alignment.Center,
                    label = "count",
                ) { n ->
                    if (n == 0) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            Modifier.size(68.dp),
                            tint = colors.onPrimaryContainer,
                        )
                    } else {
                        Text("$n", fontSize = 68.sp, fontWeight = FontWeight.ExtraBold, color = colors.onPrimaryContainer)
                    }
                }
                Text(
                    if (done) "PAUSE DONE" else "SECONDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }
    val closeButton = @Composable { modifier: Modifier ->
        Button(onClick = onClose, modifier = modifier) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = null, Modifier.size(22.dp))
            Text("Close $appName", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 10.dp))
        }
    }
    val openAnyway = @Composable { modifier: Modifier ->
        OutlinedButton(onClick = onOpenAnyway, enabled = done, modifier = modifier) {
            Text(if (done) "Open anyway" else "Open anyway in $left s")
        }
    }

    if (isLandscape()) {
        // The countdown on the left; the words and both actions, side by side, on the right.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            badge()
            Column(Modifier.weight(1f)) {
                Text(headline, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground, modifier = Modifier.rise(0))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp).rise(1),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    closeButton(Modifier.rise(2).weight(1f).height(56.dp))
                    openAnyway(openButton.rise(3).weight(1f).height(56.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        badge()
        Spacer(Modifier.height(18.dp))
        Text(
            headline,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground,
            modifier = Modifier.rise(0),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // Two lines reserved so the sheet keeps its height when the message changes.
            minLines = 2,
            modifier = Modifier.rise(1),
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            closeButton(Modifier.rise(2).fillMaxWidth().height(64.dp))
            openAnyway(openButton.rise(3).fillMaxWidth().height(56.dp))
        }
    }
}

// Visit limits in minutes; null means no limit.
private val limits = listOf(2, 5, 15, null)

private fun limitLabel(minutes: Int?) = if (minutes == null) "No limit" else "$minutes min"

/** "1st", "2nd", "3rd", "11th", "22nd"... */
internal fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) {
        "th"
    } else {
        when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$n$suffix"
}

@Composable
private fun DecideScreen(
    fullHeight: Dp,
    appName: String,
    visits: Int,
    minutesToday: Int,
    onClose: () -> Unit,
    onOpenFor: (minutes: Int?) -> Unit,
    openButton: Modifier,
) {
    var limit by rememberSaveable { mutableStateOf<Int?>(5) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val landscape = isLandscape()
    // Landscape is short, so the menu rows and the bottom button shrink a little to fit above the split button.
    val rowHeight = if (landscape) 48.dp else 56.dp

    val heading = @Composable { size: TextUnit ->
        Text(
            "This is your\n${ordinal(visits)} visit\ntoday.",
            fontSize = size,
            lineHeight = size,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground,
        )
        Text(
            if (minutesToday > 0) {
                "About $minutesToday min in $appName so far today. Set a limit for this visit?"
            } else {
                "Set a limit for this visit?"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
    val menu = @Composable { modifier: Modifier ->
        Box(modifier.fillMaxWidth()) {
            // Qualified: inside the Column, the ColumnScope overload would otherwise be picked.
            androidx.compose.animation.AnimatedVisibility(menuOpen, Modifier.align(Alignment.BottomEnd), enter = EnterTransition.None, exit = ExitTransition.None) {
                Column(
                    Modifier.padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // The current choice already shows on the button, so only the others are listed.
                    val others = limits.filter { it != limit }
                    others.forEachIndexed { i, minutes ->
                        // Staggered from the button upward on open, and from the top down on close.
                        val delay = (others.lastIndex - i) * 50
                        val outDelay = i * 40
                        Surface(
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(tween(180, delay)) +
                                    slideInVertically(tween(220, delay)) { it / 2 } +
                                    scaleIn(tween(220, delay), initialScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f)),
                                exit = fadeOut(tween(120, outDelay)) +
                                    slideOutVertically(tween(160, outDelay)) { it / 2 } +
                                    scaleOut(tween(160, outDelay), targetScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f)),
                            ),
                            onClick = {
                                limit = minutes
                                menuOpen = false
                            },
                            shape = RoundedCornerShape(28.dp),
                            color = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ) {
                            Row(
                                Modifier.height(rowHeight).padding(start = 18.dp, end = 22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val icon = if (minutes == null) R.drawable.ic_infinity else R.drawable.ic_timer
                                Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp))
                                Text(limitLabel(minutes), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    val actions = @Composable {
        // Split button: the leading half opens the app, the trailing half picks how long.
        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(
                onClick = { onOpenFor(limit) },
                shape = RoundedCornerShape(28.dp, 8.dp, 8.dp, 28.dp),
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                modifier = openButton.weight(1f).fillMaxHeight(),
            ) {
                Box(contentAlignment = Alignment.Center) { Text("Open for", fontWeight = FontWeight.Bold) }
            }
            val chevronTurn by animateFloatAsState(if (menuOpen) 180f else 0f, label = "chevron")
            Surface(
                onClick = { menuOpen = !menuOpen },
                // Rounds fully into a FAB while its menu is open.
                shape = animateDpAsState(if (menuOpen) 28.dp else 8.dp, label = "fab").value.let { RoundedCornerShape(it, 28.dp, 28.dp, it) },
                color = colors.tertiary,
                contentColor = colors.onTertiary,
                modifier = Modifier.width(140.dp).fillMaxHeight().semantics { contentDescription = "Visit limit" },
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (limit == null) "∞ No limit" else limitLabel(limit), fontWeight = FontWeight.ExtraBold)
                    Icon(
                        painterResource(R.drawable.ic_chevron_down),
                        contentDescription = null,
                        Modifier.padding(start = 6.dp).size(18.dp).rotate(chevronTurn),
                    )
                }
            }
        }

        Button(onClick = onClose, modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(if (landscape) 56.dp else 64.dp)) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, Modifier.size(22.dp))
            Text("Not now, I'm good", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 10.dp))
        }
    }

    if (landscape) {
        // The sheet grows to the full height: the message on the left, the menu, split button and "Not now" on the right.
        Row(Modifier.fillMaxWidth().height(fullHeight), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) { heading(46.sp) }
            Column(Modifier.width(290.dp).fillMaxHeight()) {
                // The time menu stacks up into whatever space is left above the split button.
                menu(Modifier.weight(1f))
                actions()
            }
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            heading(52.sp)
            // The time menu stacks up into this space above the split button: room for three 56 dp rows.
            menu(Modifier.height(188.dp))
            actions()
        }
    }
}
