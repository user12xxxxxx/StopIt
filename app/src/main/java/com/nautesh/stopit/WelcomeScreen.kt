package com.nautesh.stopit

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.nautesh.stopit.ui.theme.LocalSemanticColors
import com.nautesh.stopit.ui.theme.LocalAppColors

/** Shown instead of the tabs until both special permissions are granted. */
@Composable
fun WelcomeScreen(usageGranted: Boolean, overlayGranted: Boolean, onGrantUsage: () -> Unit, onGrantOverlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    val landscape = isLandscape()
    val hero = @Composable { modifier: Modifier ->
        Box(
            modifier
                .size(if (landscape) 150.dp else 200.dp)
                // Same pale tone as the permission rows below it.
                .background(app.cardOff, lobedShape(lobes = 4, depth = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_hand), contentDescription = null, Modifier.size(if (landscape) 64.dp else 84.dp), tint = app.screenAccent)
        }
        Text(
            "Pause before\nyou scroll.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = if (landscape) 20.dp else 28.dp),
        )
    }
    val details = @Composable { modifier: Modifier ->
        Text(
            "StopIt shows a short, random pause screen before the apps you pick. " +
                "Just enough time to ask: do I really want this?",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = modifier,
        )
        Column(Modifier.padding(top = if (landscape) 20.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

    if (landscape) {
        // Two columns: the shape and headline on the left, the explanation and permissions on the right.
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(Modifier.width(360.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                hero(Modifier.align(Alignment.CenterHorizontally))
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                details(Modifier)
            }
        }
    } else {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            hero(Modifier.align(Alignment.CenterHorizontally))
            details(Modifier.padding(top = 12.dp))
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
    val app = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.row)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(app.cardMark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp), tint = colors.onSurface)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, Modifier.size(18.dp), tint = LocalSemanticColors.current.done)
                Text("Done", color = LocalSemanticColors.current.done, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = onGrant) { Text("Allow") }
        }
    }
}
