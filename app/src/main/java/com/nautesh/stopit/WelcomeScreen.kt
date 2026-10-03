package com.nautesh.stopit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nautesh.stopit.ui.theme.lobedShape

// Mockup's "Done" green; not part of the theme because it's only used here.
private val DoneGreen = Color(0xFF2F6A3E)

/** Shown instead of the tabs until both special permissions are granted. */
@Composable
fun WelcomeScreen(usageGranted: Boolean, overlayGranted: Boolean, onGrantUsage: () -> Unit, onGrantOverlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(200.dp)
                // Same pale tone as the permission rows below it.
                .background(colors.surfaceContainerHigh, lobedShape(lobes = 4, depth = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_hand), contentDescription = null, Modifier.size(84.dp), tint = colors.primary)
        }

        Text(
            "Pause before\nyou scroll.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            "stopIt shows a short, random pause screen before the apps you pick. " +
                "Just enough time to ask: do I really want this?",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PermissionCard(
                icon = R.drawable.ic_chart,
                title = "Usage access",
                detail = "Detects when a guarded app opens",
                granted = usageGranted,
                shape = RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp),
                onGrant = onGrantUsage,
            )
            PermissionCard(
                icon = R.drawable.ic_layers,
                title = "Display over apps",
                detail = "Shows the pause screen on top",
                granted = overlayGranted,
                shape = RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp),
                onGrant = onGrantOverlay,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    granted: Boolean,
    shape: Shape,
    onGrant: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(colors.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp), tint = colors.onTertiaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, Modifier.size(18.dp), tint = DoneGreen)
                Text("Done", color = DoneGreen, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
            ) { Text("Allow") }
        }
    }
}
