package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.DailyPoint
import com.example.data.analytics.PaymentSlice
import com.example.data.analytics.RestockUrgency
import com.example.data.model.PaymentMethod
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.ESewaGreen
import com.example.ui.theme.FonepayRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.KhaltiPurple
import com.example.ui.theme.LineSubtle
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType
import com.example.ui.theme.WarningOrange
import com.example.ui.util.Format

/** Brand-consistent colour for each Nepal payment rail. */
fun paymentAccent(methodCode: String): Color = when (PaymentMethod.fromCode(methodCode)) {
    PaymentMethod.CASH -> BrandGreen
    PaymentMethod.ESEWA -> ESewaGreen
    PaymentMethod.FONEPAY -> FonepayRed
    PaymentMethod.KHALTI -> KhaltiPurple
}

/**
 * Column based bar chart — no canvas text measurement required, so labels stay
 * perfectly aligned and the whole chart is accessible to TalkBack.
 */
@Composable
fun SalesTrendChart(
    points: List<DailyPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 156.dp
) {
    if (points.isEmpty()) return
    val maxTotal = points.maxOf { it.total }.coerceAtLeast(1.0)
    val hasData = points.any { it.total > 0.0 }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.spacedBy(PosSpace.xs),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { point ->
                val fraction = if (hasData) (point.total / maxTotal).toFloat() else 0.04f
                val isPeak = point.total == maxTotal && hasData
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Value label only when there is room for it (short windows).
                    if (points.size <= 8 && point.total > 0.0) {
                        Text(
                            text = Format.compact(point.total),
                            style = PosType.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(
                                if (isPeak) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(PosSpace.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LineSubtle)
        )
        Spacer(Modifier.height(PosSpace.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)
        ) {
            val labelStep = if (points.size > 10) (points.size / 5).coerceAtLeast(1) else 1
            points.forEachIndexed { index, point ->
                Text(
                    text = if (index % labelStep == 0 || index == points.lastIndex) point.label else "",
                    style = PosType.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(PosSpace.xs))
        Text(
            text = "Peak ${Format.money(points.maxOf { it.total }, 0, currencySymbol)}",
            style = PosType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Donut chart with a centred total, paired with the legend rendered by the caller. */
@Composable
fun PaymentDonut(
    slices: List<PaymentSlice>,
    modifier: Modifier = Modifier,
    diameter: Dp = 148.dp,
    centerValue: String = "",
    centerLabel: String = "Revenue"
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val stroke = diameter * 0.16f

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = slice.share * 360f
                drawArc(
                    color = paymentAccent(slice.method),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = PosType.moneyTiny,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = centerLabel,
                style = PosType.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Thin rounded progress bar used for market-share style indicators. */
@Composable
fun ShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** Legend row: colour dot, rail name, share and amount. */
@Composable
fun PaymentLegendRow(
    slice: PaymentSlice,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val accent = paymentAccent(slice.method)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
        Spacer(Modifier.width(PosSpace.sm))
        Text(
            text = PaymentMethod.fromCode(slice.method).shortLabel,
            style = PosType.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = Format.percent(slice.share * 100.0),
            style = PosType.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(PosSpace.md))
        Text(
            text = Format.money(slice.total, 0, currencySymbol),
            style = PosType.moneyTiny,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Severity colours for predictive restocking rows. */
@Composable
fun urgencyAccent(urgency: RestockUrgency): Color = when (urgency) {
    RestockUrgency.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
    RestockUrgency.STOCKOUT_SOON -> WarningOrange
    RestockUrgency.WATCH -> AccentAmber
    RestockUrgency.PLENTY -> InfoBlue
}