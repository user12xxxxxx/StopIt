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

/** The gear's bottom sheet: a short menu, with "About app" opening in place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    var about by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (about) AboutPage(onBack = { about = false }) else Menu(onAbout = { about = true })
        }
    }
}

@Composable
private fun Menu(onAbout: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onAbout,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) { ButtonContent(R.drawable.ic_info, "About app") }
    // ponytail: Appearance has no page yet; it does nothing until its options are decided.
    Button(
        onClick = {},
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

@Composable
private fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = colors.surfaceContainerHigh),
        ) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
        Text("About app", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
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
