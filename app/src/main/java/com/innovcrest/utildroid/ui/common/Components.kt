package com.innovcrest.utildroid.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.theme.Type
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Pastel container + ink pairs for icon-less app tiles. */
private val tilePalette = listOf(
    Color(0xFFFFDAD5) to Color(0xFF8C1D18),
    Color(0xFFD6E3FF) to Color(0xFF284777),
    Color(0xFFBCEBEE) to Color(0xFF00504F),
    Color(0xFFB7F2C3) to Color(0xFF005321),
    Color(0xFFFFDDB8) to Color(0xFF6B3D00),
    Color(0xFFEBDCFF) to Color(0xFF4F2E86),
)

/** App icon from the catalog, or a scalloped letter tile when there is none. */
@Composable
fun AppIcon(app: CatalogApp, size: Dp, modifier: Modifier = Modifier) {
    if (app.iconUrl.isBlank()) {
        LetterTile(app, size, modifier)
        return
    }
    SubcomposeAsyncImage(
        model = app.iconUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)),
        loading = { LetterTile(app, size) },
        error = { LetterTile(app, size) },
    )
}

@Composable
private fun LetterTile(app: CatalogApp, size: Dp, modifier: Modifier = Modifier) {
    val seed = app.id.hashCode()
    val (container, ink) = tilePalette[Math.floorMod(seed, tilePalette.size)]
    val shape = StoreShapes.Tiles[Math.floorMod(seed / tilePalette.size, StoreShapes.Tiles.size)]
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
    Box(modifier.size(size).background(container, shape), contentAlignment = Alignment.Center) {
        Text(app.name.take(1).uppercase(), color = ink, fontSize = fontSize, fontWeight = Type.Display)
    }
}

/** The store mark: tiles-plus glyph on a six-lobed cookie. */
@Composable
fun StoreGlyph(size: Dp, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(container, StoreShapes.Cookie6), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.DashboardCustomize, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

enum class PillStyle { Filled, Tonal, Neutral, Tertiary }

/** Fully rounded text button in one of the store's four fills. */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Filled,
    enabled: Boolean = true,
    height: Dp = 40.dp,
    shape: Shape = CircleShape,
    icon: ImageVector? = null,
    fontSize: TextUnit = 14.sp,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        !enabled -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        style == PillStyle.Filled -> colors.primary to colors.onPrimary
        style == PillStyle.Tonal -> colors.secondaryContainer to colors.onSecondaryContainer
        style == PillStyle.Tertiary -> colors.tertiary to colors.onTertiary
        else -> colors.surfaceContainerHigh to colors.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = modifier.height(height),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, fontSize = fontSize, fontWeight = Type.Emphasis, maxLines = 1)
        }
    }
}

fun downloadProgress(item: AppUi): Float? =
    ((item.status as? AppStatus.Busy)?.phase as? InstallPhase.Downloading)?.progress

/**
 * Download progress: a travelling wave for the done part, a flat track for the rest,
 * and a stop dot at the far end.
 */
@Composable
fun WavyProgress(progress: Float, modifier: Modifier = Modifier) {
    val active = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.secondaryContainer
    val fraction = progress.coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "wavePhase",
    )
    val wave = remember { Path() }

    Canvas(modifier.fillMaxWidth().height(12.dp).progressSemantics(fraction)) {
        val stroke = 4.dp.toPx()
        val amplitude = 3.dp.toPx()
        val wavelength = 33.dp.toPx()
        val gap = 10.dp.toPx()
        val step = 1.5.dp.toPx()
        val cy = size.height / 2f
        val start = stroke / 2f
        val trackEnd = size.width - 10.dp.toPx()
        val waveEnd = start + (size.width - stroke) * fraction
        fun waveY(x: Float) = cy + amplitude * sin(2.0 * PI * (x / wavelength - phase)).toFloat()

        if (waveEnd > start) {
            wave.rewind()
            var x = start
            wave.moveTo(x, waveY(x))
            while (x < waveEnd) {
                x = min(x + step, waveEnd)
                wave.lineTo(x, waveY(x))
            }
            drawPath(wave, active, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        val trackStart = waveEnd + gap
        if (trackStart < trackEnd) {
            drawLine(track, Offset(trackStart, cy), Offset(trackEnd, cy), stroke, StrokeCap.Round)
        }
        drawCircle(active, radius = 2.dp.toPx(), center = Offset(size.width - 3.dp.toPx(), cy))
    }
}

/** The one action on a list row: Install / Update / Open / Cancel / Retry. */
@Composable
fun RowAction(item: AppUi, onClick: () -> Unit) {
    when (val status = item.status) {
        AppStatus.NotInstalled -> PillButton("Install", onClick)
        is AppStatus.UpdateAvailable -> PillButton("Update", onClick)
        is AppStatus.Installed -> PillButton("Open", onClick, style = PillStyle.Tonal)
        AppStatus.Unavailable -> PillButton("No release", onClick, style = PillStyle.Neutral, enabled = false)
        is AppStatus.Busy -> when (status.phase) {
            is InstallPhase.Downloading -> Surface(
                onClick = onClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Cancel download", modifier = Modifier.size(20.dp))
                }
            }
            is InstallPhase.Failed -> PillButton("Retry", onClick)
            else -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * App row used in Browse and Updates. [compact] drops the summary and shows the
 * version change next to the download percentage.
 */
@Composable
fun AppRow(
    item: AppUi,
    shape: Shape,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val phase = (item.status as? AppStatus.Busy)?.phase
    val statusColor = when (phase) {
        null -> colors.onSurfaceVariant
        is InstallPhase.Failed -> colors.error
        else -> colors.primary
    }
    Surface(onClick = onClick, shape = shape, color = colors.surface, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppIcon(item.app, if (compact) 48.dp else 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.app.name,
                        fontSize = if (compact) 16.sp else 17.sp,
                        fontWeight = Type.Emphasis,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!compact && item.app.summary.isNotBlank()) {
                        Text(
                            item.app.summary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        statusLine(item, compact),
                        fontSize = if (compact) 13.sp else 12.sp,
                        fontWeight = if (phase != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = statusColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RowAction(item, onAction)
            }
            downloadProgress(item)?.let { WavyProgress(it) }
        }
    }
}

/** Teal call-out with a cookie badge and one action. */
@Composable
fun StoreBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = StoreShapes.Card,
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Box(Modifier.size(48.dp).background(colors.tertiary, StoreShapes.Cookie8), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onTertiary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = Type.Emphasis)
                Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = colors.onTertiaryContainer.copy(alpha = 0.78f))
            }
            PillButton(action, onAction, style = PillStyle.Tertiary, enabled = actionEnabled)
        }
    }
}

/** Large left-aligned screen title with an optional line under it. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 44.sp, lineHeight = 52.sp, fontWeight = Type.Display, letterSpacing = (-0.5).sp)
        if (subtitle != null) {
            Text(subtitle, fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = Type.Emphasis,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String?, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
    ) {
        Text(title, fontSize = 20.sp, fontWeight = Type.Display)
        if (body != null) Text(body, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) PillButton(action, onAction, style = PillStyle.Tonal, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Third line on a row: version change, progress or error. */
fun statusLine(item: AppUi, compact: Boolean = false): String {
    val app = item.app
    return when (val s = item.status) {
        is AppStatus.UpdateAvailable -> "${s.installedVersionName} → ${app.versionName} · ${formatSize(app.sizeBytes)}"
        is AppStatus.Busy -> when (val p = s.phase) {
            is InstallPhase.Downloading -> {
                val percent = (p.progress * 100).toInt()
                val from = item.installed?.versionName.orEmpty()
                when {
                    compact && from.isNotBlank() -> "$from → ${app.versionName} · $percent%"
                    compact -> "${app.versionName} · $percent%"
                    else -> "Downloading · $percent% of ${formatSize(app.sizeBytes)}"
                }
            }
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
