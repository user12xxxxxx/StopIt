package com.nautesh.stopit

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nautesh.stopit.ui.theme.StopItTheme

/**
 * Space a screen leaves at the bottom for the floating navbar: 72dp bar + 20dp inset + 8dp gap, above the
 * gesture bar. Screens draw behind the gesture bar, so this includes its height.
 */
val NavBarClearance: Dp
    @Composable get() = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_home),
    Apps("Apps", R.drawable.ic_apps),
    Pause("Pause", R.drawable.ic_timer),
}

class MainActivity : ComponentActivity() {
    private var usageGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        checkPermissions() // Before the first frame, so Welcome doesn't flash for granted users.
        setContent {
            var theme by remember { mutableStateOf(prefs.theme) }
            val dark = when (theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // System bar icons follow the app's theme, which can differ from the system's.
            LaunchedEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            StopItTheme(dark) {
                var tab by rememberSaveable { mutableStateOf(Tab.Home) }
                BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        // Not the bottom: lists scroll on behind the gesture bar.
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                ) {
                    if (!usageGranted || !overlayGranted) {
                        WelcomeScreen(
                            usageGranted = usageGranted,
                            overlayGranted = overlayGranted,
                            onGrantUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            onGrantOverlay = {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            },
                        )
                        return@Box
                    }
                    AnimatedContent(
                        tab,
                        // Slides a little toward the side of the tab that was picked, matching the navbar order.
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (fadeIn() + slideInHorizontally { dir * it / 8 }) togetherWith fadeOut()
                        },
                        label = "tab",
                    ) { shown ->
                        when (shown) {
                            // Re-read counters each time the user comes back; they change while we're in the background.
                            Tab.Home -> key(resumeCount) {
                                HomeScreen(prefs, theme) {
                                    prefs.theme = it
                                    theme = it
                                }
                            }
                            Tab.Apps -> AppsScreen(prefs)
                            Tab.Pause -> PauseSettingsScreen(prefs) { PauseActivity.start(this@MainActivity, packageName) }
                        }
                    }
                    NavBar(tab, onSelect = { tab = it }, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Both are granted in system Settings, so re-check whenever the user comes back.
        checkPermissions()
        resumeCount++
        // Starts the guard right after onboarding, and revives it if the system killed it.
        if (usageGranted && overlayGranted && prefs.guarding) GuardService.start(this)
    }

    private fun checkPermissions() {
        val appOps = getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION") // Deprecated as of API 37 but still the standard check for usage access.
        usageGranted = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) ==
            AppOpsManager.MODE_ALLOWED
        overlayGranted = Settings.canDrawOverlays(this)
    }
}

@Composable
private fun NavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    // Home has no floating action above the bar, so it is fully round; other tabs flatten the top to meet theirs.
    val topCorner by animateDpAsState(if (current == Tab.Home) 32.dp else 8.dp, label = "navTop")
    Surface(
        modifier = modifier.navigationBarsPadding().padding(20.dp).fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(topCorner, topCorner, 32.dp, 32.dp),
        color = colors.surfaceContainer,
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            // One pill that slides to the selected tab, behind the tab row.
            val slot = maxWidth / Tab.entries.size
            val pillX by animateDpAsState(slot * current.ordinal, spring(dampingRatio = 0.75f, stiffness = 500f), label = "pill")
            Box(
                Modifier
                    .offset(x = pillX + 4.dp)
                    .size(slot - 8.dp, 48.dp)
                    .background(colors.primaryContainer, RoundedCornerShape(24.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tab.entries.forEach { tab ->
                    val selected = tab == current
                    val content by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant, label = "tabContent")
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .selectable(selected = selected, role = Role.Tab) {
                                // Tick only when the tab actually changes.
                                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(tab)
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(tab.icon), contentDescription = null, Modifier.size(22.dp), tint = content)
                        Text(tab.label, color = content, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
