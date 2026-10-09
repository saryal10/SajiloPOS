package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenSoft
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InkFaint
import com.example.ui.theme.LineSubtle
import com.example.ui.theme.PosGradients
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType
import com.example.ui.theme.SuccessGreen

/**
 * Metric + hero building blocks inspired by modern health, wallet and fintech apps:
 * ring gauges, sparklines, delta badges, quick action tiles, goal bars and
 * avatar list rows.
 */

// -----------------------------------------------------------------------------
// Ring gauge — the "system health" hero visual
// -----------------------------------------------------------------------------

@Composable
fun RingGauge(
    progress: Float,
    value: String,
    label: String,
    status: String,
    statusAccent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 172.dp,
    strokeWidth: Dp = 15.dp,
    accent: Color = BrandGreen,
    ticks: Boolean = true,
    onHero: Boolean = false
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ring"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx * 2
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Dotted outer ring for texture
            if (ticks) {
                val radius = this.size.minDimension / 2f - 1f
                val dotRadius = 1.6f * density
                val count = 44
                for (i in 0 until count) {
                    val angle = Math.toRadians((i * (360.0 / count)) - 90.0)
                    val cx = this.size.width / 2f + (radius * kotlin.math.cos(angle)).toFloat()
                    val cy = this.size.height / 2f + (radius * kotlin.math.sin(angle)).toFloat()
                    drawCircle(
                        color = accent.copy(alpha = if (i / count <= animated) 0.28f else 0.12f),
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Track
            drawArc(
                color = accent.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress
            if (animated > 0.001f) {
                drawArc(
                    brush = PosGradients.spectrum,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        val primaryText = if (onHero) Color.White else MaterialTheme.colorScheme.onSurface
        val secondaryText = if (onHero) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = PosType.moneyMedium,
                color = primaryText,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = PosType.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(PosSpace.xs))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (onHero) Color.White.copy(alpha = 0.22f) else statusAccent.copy(alpha = 0.14f)
                    )
                    .padding(horizontal = PosSpace.sm, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusAccent)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = status,
                    style = PosType.labelSmall,
                    color = if (onHero) Color.White else statusAccent
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Sparkline + stat card with delta
// -----------------------------------------------------------------------------

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = BrandGreen,
    strokeWidth: Dp = 2.dp,
    fill: Boolean = true
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max()
        val min = values.min()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        fun pointY(v: Float) = size.height - ((v - min) / span) * (size.height - 4f) - 2f

        val line = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = pointY(value)
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        if (fill) {
            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.34f), color.copy(alpha = 0.02f))
                )
            )
        }

        drawPath(
            path = line,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )

        // Emphasise the latest reading
        drawCircle(color = color, radius = 3.2f * density, center = Offset(size.width, pointY(values.last())))
    }
}

/** Green/red change indicator, e.g. "+12.4% vs previous period". */
@Composable
fun DeltaBadge(
    deltaPercent: Double?,
    modifier: Modifier = Modifier,
    compareLabel: String = "vs previous",
    onDark: Boolean = false
) {
    val hasValue = deltaPercent != null
    val isUp = (deltaPercent ?: 0.0) >= 0.0
    val isFlat = !hasValue || kotlin.math.abs(deltaPercent ?: 0.0) < 0.5
    val accent = when {
        onDark -> Color.White
        isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
        isUp -> SuccessGreen
        else -> DangerRed
    }
    val arrow = when {
        isFlat -> "→"
        isUp -> "↑"
        else -> "↓"
    }
    val text = if (hasValue && !isFlat) {
        "$arrow ${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(deltaPercent!!))}%"
    } else {
        "$arrow 0.0%"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (onDark) Color.White.copy(alpha = 0.22f)
                else accent.copy(alpha = 0.11f)
            )
            .padding(horizontal = PosSpace.sm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = PosType.labelSmall, color = accent)
        if (hasValue) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = compareLabel,
                style = PosType.labelSmall,
                color = if (onDark) Color.White.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Richer metric tile: value, delta, caption and sparkline. */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    delta: Double? = null,
    spark: List<Float> = emptyList(),
    icon: ImageVector? = null,
    accent: Color = BrandGreen,
    container: Color = MaterialTheme.colorScheme.surface
) {
    PosCard(
        modifier = modifier,
        containerColor = container,
        contentPadding = PaddingValues(PosSpace.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(PosSpace.xs))
            }
            Overline(label, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(PosSpace.xs))
        Text(
            text = value,
            style = PosType.moneyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (delta != null || caption != null) {
            Spacer(Modifier.height(PosSpace.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (delta != null) {
                    DeltaBadge(deltaPercent = delta)
                }
                if (caption != null) {
                    Spacer(Modifier.width(PosSpace.xs))
                    Text(
                        text = caption,
                        style = PosType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (spark.size >= 2 && spark.any { it > 0f }) {
            Spacer(Modifier.height(PosSpace.sm))
            Sparkline(
                values = spark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                color = accent
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Quick action tile
// -----------------------------------------------------------------------------

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    PosCard(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.28f else 0.12f), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() },
        borderColor = androidx.compose.ui.graphics.Color.Transparent,
        contentPadding = PaddingValues(PosSpace.md)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else InkFaint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.height(PosSpace.sm))
        Text(
            text = title,
            style = PosType.titleSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = subtitle,
            style = PosType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// Gradient hero surface
// -----------------------------------------------------------------------------

@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(26.dp),
    gradient: Brush? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineSubtle)
    ) {
        Column(
            modifier = Modifier
                .background(gradient ?: PosGradients.page())
                .padding(PosSpace.xl),
            content = content
        )
    }
}

// -----------------------------------------------------------------------------
// Goal progress bar with target marker
// -----------------------------------------------------------------------------

@Composable
fun GoalBar(
    current: Double,
    target: Double,
    modifier: Modifier = Modifier,
    accent: Color = BrandGreen,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp
) {
    val fraction = if (target > 0) (current / target).coerceIn(0.0, 1.0).toFloat() else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.75f), accent))
                    )
            )
        }
        // Marker showing 100% of the target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + 10.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height + 10.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Avatar badge + list row chevron
// -----------------------------------------------------------------------------

@Composable
fun AvatarBadge(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    circular: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(if (circular) CircleShape else RoundedCornerShape(size / 3))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.take(2).uppercase(),
            style = PosType.labelMedium,
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
fun RowChevron(tint: Color = MaterialTheme.colorScheme.outline) {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp)
    )
}

/** "See all" affordance used in section headers. */
@Composable
fun SeeAllButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = PosSpace.xs, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = PosType.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(2.dp))
        RowChevron(tint = MaterialTheme.colorScheme.primary)
    }
}

/** Coloured left rule used to highlight critical rows. */
@Composable
fun AccentRule(
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .width(3.dp)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

/** Status accent for inventory rows. */
fun stockAccent(stock: Int, threshold: Int, tracked: Boolean = true): Color = when {
    !tracked -> BrandGreen
    stock <= 0 -> DangerRed
    stock <= threshold -> AccentAmber
    else -> SuccessGreen
}