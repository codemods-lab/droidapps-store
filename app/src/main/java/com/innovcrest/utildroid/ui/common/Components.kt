package com.innovcrest.utildroid.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.theme.Brand
import java.util.Locale

private val tilePalette = listOf(
    Color(0xFFE5484D), Color(0xFF3E63DD), Color(0xFF12A594),
    Color(0xFFF76B15), Color(0xFF8E4EC6), Color(0xFF0090FF),
)

/** App icon from the catalog, or a coloured letter tile when there is none. */
@Composable
fun AppIcon(app: CatalogApp, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.25f)
    if (app.iconUrl.isBlank()) {
        LetterTile(app, size, shape, modifier)
    } else {
        SubcomposeAsyncImage(
            model = app.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
            loading = { LetterTile(app, size, shape) },
            error = { LetterTile(app, size, shape) },
        )
    }
}

@Composable
private fun LetterTile(app: CatalogApp, size: Dp, shape: Shape, modifier: Modifier = Modifier) {
    val color = tilePalette[Math.floorMod(app.id.hashCode(), tilePalette.size)]
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
    Box(
        modifier.size(size).clip(shape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            app.name.take(1).uppercase(),
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** The single primary action, with download progress drawn inside the button. */
@Composable
fun PrimaryActionButton(
    item: AppUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val status = item.status
    val progress = ((status as? AppStatus.Busy)?.phase as? InstallPhase.Downloading)?.progress

    data class Look(val label: String, val filled: Boolean, val enabled: Boolean = true)

    val look = when (status) {
        AppStatus.NotInstalled -> Look("Install", filled = true)
        is AppStatus.UpdateAvailable -> Look("Update", filled = true)
        is AppStatus.Installed -> Look("Open", filled = false)
        AppStatus.Unavailable -> Look("No release", filled = false, enabled = false)
        is AppStatus.Busy -> when (val p = status.phase) {
            is InstallPhase.Downloading ->
                Look(if (large) "Downloading ${(p.progress * 100).toInt()}% · tap to cancel" else "Cancel", filled = false)
            InstallPhase.Verifying -> Look("Verifying…", filled = false, enabled = false)
            InstallPhase.Installing -> Look("Installing…", filled = false, enabled = false)
            InstallPhase.WaitingForUser -> Look("Confirm on screen", filled = false, enabled = false)
            is InstallPhase.Failed -> Look("Retry", filled = true)
        }
    }

    val height = if (large) 52.dp else 36.dp
    val container = when {
        progress != null && large -> colors.secondaryContainer
        look.filled -> Brand.Accent
        else -> Color.Transparent
    }
    val content = if (look.filled) Brand.OnAccent else colors.onSurface
    val border = if (look.filled || (progress != null && large)) null else BorderStroke(1.dp, colors.outline)

    Surface(
        onClick = onClick,
        enabled = look.enabled,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier.height(height).widthIn(min = if (large) 0.dp else 88.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (progress != null && large) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(Brand.Accent)
                        .align(Alignment.CenterStart),
                )
            }
            Text(
                look.label,
                fontSize = if (large) 15.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (look.enabled) content else colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** Thin progress bar shown under a card while its APK downloads. */
@Composable
fun DownloadBar(item: AppUi, modifier: Modifier = Modifier) {
    val progress = ((item.status as? AppStatus.Busy)?.phase as? InstallPhase.Downloading)?.progress ?: return
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        color = Brand.Accent,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        drawStopIndicator = {},
    )
}

/** Third line on a card: version change, progress or error. */
fun statusLine(item: AppUi): String {
    val app = item.app
    return when (val s = item.status) {
        is AppStatus.UpdateAvailable -> "${s.installedVersionName} → ${app.versionName} · ${formatSize(app.sizeBytes)}"
        is AppStatus.Busy -> when (val p = s.phase) {
            is InstallPhase.Downloading -> "Downloading · ${(p.progress * 100).toInt()}% of ${formatSize(app.sizeBytes)}"
            InstallPhase.Verifying -> "Checking checksum and signature"
            InstallPhase.Installing -> "Installing"
            InstallPhase.WaitingForUser -> "Waiting for your confirmation"
            is InstallPhase.Failed -> p.message
        }
        AppStatus.Unavailable -> "No release published yet"
        is AppStatus.Installed -> "${s.versionName} · installed"
        AppStatus.NotInstalled -> "${app.versionName} · ${formatSize(app.sizeBytes)}"
    }
}

fun isFailure(item: AppUi) = (item.status as? AppStatus.Busy)?.phase is InstallPhase.Failed

fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes < 1_000_000 -> "${bytes / 1000} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
}

fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0) return "never"
    val minutes = (now - millis) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
    }
}

fun androidVersionName(sdk: Int): String = when (sdk) {
    0 -> "—"
    26 -> "Android 8.0+"
    27 -> "Android 8.1+"
    28 -> "Android 9+"
    29 -> "Android 10+"
    30 -> "Android 11+"
    31 -> "Android 12+"
    32 -> "Android 12L+"
    33 -> "Android 13+"
    34 -> "Android 14+"
    35 -> "Android 15+"
    36 -> "Android 16+"
    else -> "API $sdk+"
}

fun openInstallPermissionSettings(context: Context) {
    val intent = Intent(
        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/** Rounded white card with the hairline border used across the store. */
@Composable
fun StoreCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surface, border = border, modifier = modifier) {
            Box(Modifier.padding(14.dp), content = content)
        }
    } else {
        Surface(shape = shape, color = MaterialTheme.colorScheme.surface, border = border, modifier = modifier) {
            Box(Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
