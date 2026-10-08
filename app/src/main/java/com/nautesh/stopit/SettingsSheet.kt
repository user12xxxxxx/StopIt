package com.nautesh.stopit

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val GITHUB_URL = "https://github.com/user12xxxxxx/StopIt"

/** The gear's bottom sheet: a short menu, with "About app" and "Appearance" opening in place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(theme: ThemeMode, onTheme: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    var page by remember { mutableStateOf(Page.Menu) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                Page.Menu -> Menu(onAbout = { page = Page.About }, onAppearance = { page = Page.Appearance })
                Page.About -> AboutPage(onBack = { page = Page.Menu })
                Page.Appearance -> AppearancePage(theme, onTheme, onBack = { page = Page.Menu })
            }
        }
    }
}

private enum class Page { Menu, About, Appearance }

@Composable
private fun Menu(onAbout: () -> Unit, onAppearance: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onAbout,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) { ButtonContent(R.drawable.ic_info, "About app") }
    Button(
        onClick = onAppearance,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) { ButtonContent(R.drawable.ic_palette, "Appearance") }
}

@Composable
private fun ButtonContent(@DrawableRes icon: Int, label: String) {
    Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp))
    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
}

/** A sub-page's title row, with a back button to the menu. */
@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun AppearancePage(theme: ThemeMode, onTheme: (ThemeMode) -> Unit, onBack: () -> Unit) {
    PageHeader("Appearance", onBack)
    Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = mode == theme,
                onClick = { onTheme(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
            ) { Text(mode.label) }
        }
    }
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    PageHeader("About app", onBack)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        InfoRow(R.drawable.ic_info, "Version", "StopIt $version", RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp))
        InfoRow(
            R.drawable.ic_github,
            "Source code on GitHub",
            GITHUB_URL.ifEmpty { "Coming soon" },
            RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp),
            onClick = GITHUB_URL.takeIf { it.isNotEmpty() }?.let { url ->
                { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
        )
    }
}

@Composable
private fun InfoRow(@DrawableRes icon: Int, title: String, detail: String, shape: Shape, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp), tint = colors.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth(), content = content)
    } else {
        Surface(shape = shape, color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth(), content = content)
    }
}
