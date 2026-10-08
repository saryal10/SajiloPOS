package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.InsightRange
import com.example.data.analytics.RestockSuggestion
import com.example.data.analytics.RestockUrgency
import com.example.data.model.BusinessSettings
import com.example.ui.components.EmptyState
import com.example.ui.components.PaymentDonut
import com.example.ui.components.PaymentLegendRow
import com.example.ui.components.PosCard
import com.example.ui.components.SalesTrendChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShareBar
import com.example.ui.components.SoftPill
import com.example.ui.components.StatTile
import com.example.ui.components.StatusPill
import com.example.ui.components.urgencyAccent
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.PosSpace
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.util.Format
import com.example.ui.viewmodel.InsightsSnapshot
import com.example.ui.theme.PosType
import kotlin.math.roundToInt

/**
 * Insights — Phase 2 of the requirement doc.
 * Sales trends, payment mix, margin and predictive restock recommendations
 * computed entirely on-device from the local SQLite ledger.
 */
@Composable
fun InsightsScreen(
    snapshot: InsightsSnapshot,
    selectedRange: InsightRange,
    restockSuggestions: List<RestockSuggestion>,
    inventoryValue: Double,
    settings: BusinessSettings,
    onRangeChange: (InsightRange) -> Unit,
    onRestock: (RestockSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = settings.currencySymbol

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insights_list"),
        contentPadding = PaddingValues(
            start = PosSpace.xl,
            end = PosSpace.xl,
            top = PosSpace.xl,
            bottom = PosSpace.huge
        ),
        verticalArrangement = Arrangement.spacedBy(PosSpace.lg)
    ) {
        item {
            SectionHeader(
                title = "Business Insights",
                subtitle = "Live numbers from your offline ledger",
                icon = Icons.Rounded.AutoGraph
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
                items(InsightRange.entries.toList()) { range ->
                    SoftPill(
                        label = range.label,
                        selected = range == selectedRange,
                        onClick = { onRangeChange(range) },
                        testTag = "insight_range_${range.name.lowercase()}"
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // Headline metrics
        // ---------------------------------------------------------------------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                StatTile(
                    label = "Revenue",
                    value = Format.money(snapshot.revenue, 0, symbol),
                    caption = "${snapshot.orders} orders · avg ${Format.money(snapshot.averageTicket, 0, symbol)}",
                    icon = Icons.Rounded.TrendingUp,
                    accent = BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Gross profit",
                    value = Format.money(snapshot.grossProfit, 0, symbol),
                    caption = "${Format.percent(snapshot.marginPercent, 1)} margin",
                    icon = Icons.Rounded.Bolt,
                    accent = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                StatTile(
                    label = "Inventory value",
                    value = Format.money(inventoryValue, 0, symbol),
                    caption = "At cost price",
                    icon = Icons.Rounded.Inventory2,
                    accent = InfoBlue,
                    container = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Needs restock",
                    value = restockSuggestions.count { it.suggestedQty > 0 }.toString(),
                    caption = "items to reorder",
                    icon = Icons.Rounded.Warning,
                    accent = if (restockSuggestions.isEmpty()) SuccessGreen else WarningOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---------------------------------------------------------------------
        // Sales trend
        // ---------------------------------------------------------------------
        item {
            PosCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Sales trend",
                    subtitle = selectedRange.label,
                    icon = Icons.Rounded.TrendingUp
                )
                Spacer(Modifier.height(PosSpace.lg))
                if (snapshot.orders == 0) {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No sales in this window",
                        message = "Complete a sale from the Sell tab and it will appear here instantly — even offline."
                    )
                } else {
                    SalesTrendChart(points = snapshot.series, currencySymbol = symbol)
                }
            }
        }

        // ---------------------------------------------------------------------
        // Payment mix
        // ---------------------------------------------------------------------
        item {
            PosCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Payment mix",
                    subtitle = "How customers paid",
                    icon = Icons.Rounded.Bolt
                )
                Spacer(Modifier.height(PosSpace.lg))
                if (snapshot.paymentMix.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Bolt,
                        title = "Nothing to split yet",
                        message = "Cash, eSewa, Fonepay and Khalti rails appear here once you ring up a sale."
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PaymentDonut(
                            slices = snapshot.paymentMix,
                            centerValue = Format.compact(snapshot.revenue),
                            centerLabel = "Total"
                        )
                        Spacer(Modifier.width(PosSpace.lg))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PosSpace.sm)
                        ) {
                            snapshot.paymentMix.forEach { slice ->
                                PaymentLegendRow(slice = slice, currencySymbol = symbol)
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Top sellers
        // ---------------------------------------------------------------------
        if (snapshot.topItems.isNotEmpty()) {
            item {
                PosCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Top sellers",
                        subtitle = "By revenue in this window",
                        icon = Icons.Rounded.TrendingUp
                    )
                    Spacer(Modifier.height(PosSpace.md))
                    val maxRevenue = snapshot.topItems.maxOf { it.revenue }.coerceAtLeast(1.0)
                    snapshot.topItems.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.height(PosSpace.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = PosType.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(PosSpace.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = PosType.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(5.dp))
                                ShareBar(
                                    fraction = (item.revenue / maxRevenue).toFloat(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(PosSpace.sm))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Format.money(item.revenue, 0, symbol),
                                    style = PosType.moneyTiny,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.units} sold",
                                    style = PosType.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Predictive restocking
        // ---------------------------------------------------------------------
        item {
            SectionHeader(
                title = "Smart restock",
                subtitle = "Based on the last 14 days of sales velocity",
                icon = Icons.Rounded.AddShoppingCart
            )
        }

        if (restockSuggestions.isEmpty()) {
            item {
                PosCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Rounded.AddShoppingCart,
                        title = "Shelves look healthy",
                        message = "No item is projected to run out inside the supplier lead time. Keep selling!"
                    )
                }
            }
        } else {
            item {
                PosCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        restockSuggestions.forEachIndexed { index, suggestion ->
                            if (index > 0) {
                                Spacer(Modifier.height(PosSpace.md))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Spacer(Modifier.height(PosSpace.md))
                            }
                            RestockRow(
                                suggestion = suggestion,
                                currencySymbol = symbol,
                                onRestock = { onRestock(suggestion) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestockRow(
    suggestion: RestockSuggestion,
    currencySymbol: String,
    onRestock: () -> Unit
) {
    val accent = urgencyAccent(suggestion.urgency)
    val coverLabel = when {
        suggestion.daysOfCover == Double.MAX_VALUE -> "No recent sales"
        suggestion.daysOfCover >= 100 -> "${suggestion.daysOfCover.roundToInt()} days left"
        else -> "${suggestion.daysOfCover.roundToInt()}d left"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (suggestion.urgency == RestockUrgency.OUT_OF_STOCK) Icons.Rounded.Warning else Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(Modifier.width(PosSpace.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name,
                style = PosType.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$coverLabel · sells ${Format.number(suggestion.avgDailySales, 1)}/day",
                style = PosType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(PosSpace.xs))

        Column(horizontalAlignment = Alignment.End) {
            StatusPill(
                text = if (suggestion.suggestedQty > 0) "Order ${suggestion.suggestedQty} ${suggestion.unit}" else "Healthy",
                accent = accent
            )
            if (suggestion.suggestedQty > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Format.money(suggestion.stockValue, 0, currencySymbol) + " on shelf",
                    style = PosType.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(PosSpace.sm))

        SoftPill(
            label = "Restock",
            selected = true,
            onClick = onRestock,
            leadingIcon = Icons.Rounded.AddShoppingCart,
            testTag = "restock_action_${suggestion.productId}"
        )
    }
}
