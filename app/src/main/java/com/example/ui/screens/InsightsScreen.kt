package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.InsightRange
import com.example.data.analytics.RestockSuggestion
import com.example.data.model.BusinessSettings
import com.example.ui.components.AvatarBadge
import com.example.ui.components.DeltaBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.GoalBar
import com.example.ui.components.HairlineDivider
import com.example.ui.components.HeroSurface
import com.example.ui.components.PaymentDonut
import com.example.ui.components.PaymentLegendRow
import com.example.ui.components.PosCard
import com.example.ui.components.RingGauge
import com.example.ui.components.RowChevron
import com.example.ui.components.SalesTrendChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SeeAllButton
import com.example.ui.components.ShareBar
import com.example.ui.components.SoftPill
import com.example.ui.components.StatCard
import com.example.ui.components.StatusPill
import com.example.ui.components.urgencyAccent
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenSoft
import com.example.ui.theme.InfoBlue
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PosGradients
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.util.Format
import com.example.ui.viewmodel.InsightsSnapshot
import kotlin.math.roundToInt

/**
 * Insights — Phase 2 of the requirement doc.
 * Sales trends, payment mix, margin and predictive restock recommendations,
 * computed entirely on-device from the local SQLite ledger.
 */
@Composable
fun InsightsScreen(
    snapshot: InsightsSnapshot,
    selectedRange: InsightRange,
    restockSuggestions: List<RestockSuggestion>,
    inventoryValue: Double,
    catalogCount: Int = 0,
    settings: BusinessSettings,
    onRangeChange: (InsightRange) -> Unit,
    onRestock: (RestockSuggestion) -> Unit,
    onOpenInventory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val symbol = settings.currencySymbol
    val hasGoal = snapshot.dailyGoal > 0.0

    // Ring status follows the merchant's own average day, not an arbitrary target.
    val progress = snapshot.goalProgress
    val (status, statusAccent) = when {
        !hasGoal -> "SELL TO START" to MaterialTheme.colorScheme.onSurfaceVariant
        progress >= 1f -> "GOAL REACHED" to SuccessGreen
        progress >= 0.7f -> "ON TRACK" to BrandGreen
        progress >= 0.4f -> "BUILDING" to WarningOrange
        else -> "BEHIND PACE" to WarningOrange
    }

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
                title = "Business insights",
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
        // Hero: today vs the shop's own average day
        // ---------------------------------------------------------------------
        item {
            HeroSurface(
                modifier = Modifier.fillMaxWidth(),
                gradient = PosGradients.brand
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingGauge(
                        progress = progress,
                        value = Format.money(snapshot.todayRevenue, 0, symbol),
                        label = if (hasGoal) {
                            "of ${Format.money(snapshot.dailyGoal, 0, symbol)} average day"
                        } else {
                            "today · no history yet"
                        },
                        status = status,
                        statusAccent = statusAccent,
                        accent = Color.White,
                        onHero = true
                    )

                    Spacer(Modifier.width(PosSpace.xl))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today",
                            style = PosType.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${snapshot.orders} orders in ${selectedRange.label.lowercase()}",
                            style = PosType.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )

                        Spacer(Modifier.height(PosSpace.md))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = Format.money(snapshot.revenue, 0, symbol),
                                style = PosType.moneyMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.width(PosSpace.xs))
                            Text(
                                text = selectedRange.label.lowercase(),
                                style = PosType.bodySmall,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Spacer(Modifier.height(PosSpace.sm))
                        DeltaBadge(
                            deltaPercent = snapshot.revenueDeltaPercent,
                            compareLabel = "vs prev ${selectedRange.days}d",
                            onDark = true
                        )

                        if (hasGoal) {
                            Spacer(Modifier.height(PosSpace.md))
                            GoalBar(
                                current = snapshot.todayRevenue,
                                target = snapshot.dailyGoal,
                                accent = if (progress >= 1f) Color.White else Color.White.copy(alpha = 0.9f),
                                track = Color.White.copy(alpha = 0.28f)
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // KPI row
        // ---------------------------------------------------------------------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                StatCard(
                    label = "Revenue",
                    value = Format.money(snapshot.revenue, 0, symbol),
                    delta = snapshot.revenueDeltaPercent,
                    spark = snapshot.trend,
                    icon = Icons.Rounded.TrendingUp,
                    accent = BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Gross profit",
                    value = Format.money(snapshot.grossProfit, 0, symbol),
                    caption = "${Format.percent(snapshot.marginPercent, 1)} margin",
                    spark = snapshot.trend.map { it * (snapshot.marginPercent / 100.0).toFloat() },
                    icon = Icons.Rounded.Bolt,
                    accent = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                StatCard(
                    label = "Orders",
                    value = snapshot.orders.toString(),
                    delta = snapshot.ordersDeltaPercent,
                    caption = "avg ${Format.money(snapshot.averageTicket, 0, symbol)}",
                    icon = Icons.Rounded.ReceiptLong,
                    accent = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Stock value",
                    value = Format.money(inventoryValue, 0, symbol),
                    caption = "$catalogCount products",
                    icon = Icons.Rounded.Inventory2,
                    accent = InfoBlue,
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
                    subtitle = if (hasGoal) "Tap a bar to inspect a day" else "Daily revenue, VAT inclusive",
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
        // Best sellers
        // ---------------------------------------------------------------------
        if (snapshot.topItems.isNotEmpty()) {
            item {
                PosCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Best selling items",
                        subtitle = "By revenue in this window"
                    )
                    Spacer(Modifier.height(PosSpace.md))
                    val maxRevenue = snapshot.topItems.maxOf { it.revenue }.coerceAtLeast(1.0)
                    snapshot.topItems.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.height(PosSpace.sm))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenInventory() }
                                .padding(vertical = PosSpace.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(
                                label = item.name,
                                accent = if (index == 0) BrandGreen else BrandGreenSoft,
                                size = 38.dp
                            )
                            Spacer(Modifier.width(PosSpace.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = PosType.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (index == 0) {
                                        Spacer(Modifier.width(PosSpace.xs))
                                        StatusPill(text = "Top seller", accent = BrandGreen)
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                ShareBar(
                                    fraction = (item.revenue / maxRevenue).toFloat(),
                                    color = BrandGreen
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${item.units} sold · ${Format.percent(item.revenue / snapshot.revenue.coerceAtLeast(1.0) * 100.0)} of revenue",
                                    style = PosType.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(PosSpace.sm))
                            Text(
                                text = Format.money(item.revenue, 0, symbol),
                                style = PosType.moneyTiny,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            RowChevron()
                        }
                    }
                    Spacer(Modifier.height(PosSpace.sm))
                    HairlineDivider()
                    Spacer(Modifier.height(PosSpace.xs))
                    SeeAllButton(
                        text = "See full inventory",
                        onClick = onOpenInventory,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // Predictive restocking
        // ---------------------------------------------------------------------
        item {
            SectionHeader(
                title = "Smart restock",
                subtitle = "Predicted from the last 14 days of sales velocity",
                icon = Icons.Rounded.AddShoppingCart,
                action = {
                    val needs = restockSuggestions.count { it.suggestedQty > 0 }
                    if (needs > 0) {
                        StatusPill(text = "$needs to order", accent = WarningOrange)
                    } else {
                        StatusPill(text = "All healthy", accent = SuccessGreen)
                    }
                }
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
        AvatarBadge(
            label = suggestion.name,
            accent = accent,
            size = 40.dp,
            circular = true
        )

        Spacer(Modifier.width(PosSpace.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name,
                style = PosType.titleSmall,
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