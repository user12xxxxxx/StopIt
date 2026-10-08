package com.nautesh.stopit

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nautesh.stopit.ui.theme.LocalSemanticColors

private class InstalledApp(val pkg: String, val label: String, val icon: ImageBitmap, val category: Int)

// Category filters from the mockup; null means all apps.
private val filters = listOf(
    "All" to null,
    "Social" to ApplicationInfo.CATEGORY_SOCIAL,
    "Video" to ApplicationInfo.CATEGORY_VIDEO,
    "Games" to ApplicationInfo.CATEGORY_GAME,
)

private fun appCount(n: Int) = if (n == 1) "1 app" else "$n apps"

private fun categoryName(category: Int) = filters.firstOrNull { it.second == category }?.first.orEmpty()

private fun loadApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    @Suppress("DEPRECATION") // The flags-object overload needs API 33.
    val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    return launchable
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map {
            InstalledApp(
                pkg = it.packageName,
                label = pm.getApplicationLabel(it).toString(),
                icon = pm.getApplicationIcon(it).toBitmap(96, 96).asImageBitmap(),
                category = it.category,
            )
        }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun AppsScreen(prefs: Prefs) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps(context) } }

    var saved by remember { mutableStateOf(prefs.guarded) }
    var selected by remember { mutableStateOf(saved) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Int?>(null) }

    // The saved set can name apps that aren't installed (the defaults), so count installed ones only.
    val count = apps?.count { it.pkg in selected } ?: 0
    val shown = apps.orEmpty().filter {
        (filter == null || it.category == filter) && it.label.contains(query.trim(), ignoreCase = true)
    }
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 32.dp, bottom = NavBarClearance + 80.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text("Choose apps", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${appCount(count)} ${if (count == 1) "gets" else "get"} a pause before opening",
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerLow,
                        unfocusedContainerColor = colors.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
            item {
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { (name, category) ->
                        FilterChip(
                            selected = filter == category,
                            onClick = { filter = category },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.secondaryContainer),
                        )
                    }
                }
            }
            if (apps == null) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
            }
            itemsIndexed(shown, key = { _, app -> app.pkg }) { index, app ->
                val on = app.pkg in selected
                val shape = when {
                    shown.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp)
                    index == shown.lastIndex -> RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (on) colors.surfaceContainerLow else colors.surface)
                        .toggleable(value = on, role = Role.Switch) {
                            haptics.toggle(it)
                            selected = if (it) selected + app.pkg else selected - app.pkg
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(app.icon, contentDescription = null, Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium)
                        categoryName(app.category).takeIf { it.isNotEmpty() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    Switch(checked = on, onCheckedChange = null)
                }
            }
        }

        // Always shown: light peach with unsaved changes, light green once saved.
        val dirty = selected != saved
        Button(
            onClick = {
                prefs.guarded = selected
                saved = selected
            },
            shape = RoundedCornerShape(28.dp, 28.dp, 8.dp, 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = animateColorAsState(if (dirty) colors.primaryContainer else LocalSemanticColors.current.walkedAway, label = "save").value,
                contentColor = animateColorAsState(if (dirty) colors.onPrimaryContainer else LocalSemanticColors.current.onWalkedAway, label = "onSave").value,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = NavBarClearance)
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Text("${if (dirty) "Save" else "Saved"} ${appCount(count)}", fontWeight = FontWeight.Bold)
        }
    }
}
