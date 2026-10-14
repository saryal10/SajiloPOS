package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.SaleTransaction
import com.example.ui.components.EmptyState
import com.example.ui.components.PosCard
import com.example.ui.components.RowChevron
import com.example.ui.components.StatusPill
import com.example.ui.components.PosSearchField
import com.example.ui.components.SectionHeader
import com.example.ui.components.SoftPill
import com.example.ui.components.StatTile
import com.example.ui.components.paymentAccent
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandGreen
import com.example.ui.util.Format
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType

@Composable
fun TransactionsScreen(
    transactions: List<SaleTransaction>,
    settings: BusinessSettings,
    onViewReceipt: (SaleTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) } // null == all

    val symbol = settings.currencySymbol
    val todayStart = remember { Format.startOfToday() }

    val todayTransactions = remember(transactions) {
        transactions.filter { it.timestamp >= todayStart }
    }

    val filteredList = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { sale ->
            val matchesQuery = searchQuery.isBlank() ||
                sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                sale.metaInfo.contains(searchQuery, ignoreCase = true) ||
                sale.transactionRef.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || sale.paymentMethod == selectedFilter
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_list"),
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
                title = "Sales history",
                subtitle = "${transactions.size} receipts stored on this device",
                icon = Icons.Rounded.ReceiptLong
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                StatTile(
                    label = "Today",
                    value = Format.money(todayTransactions.sumOf { it.grandTotal }, 0, symbol),
                    caption = "${todayTransactions.size} orders so far",
                    accent = BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "All time",
                    value = Format.money(transactions.sumOf { it.grandTotal }, 0, symbol),
                    caption = "Lifetime revenue",
                    accent = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column {
                PosSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search invoice, reference or table",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input")
                )
                Spacer(Modifier.height(PosSpace.md))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
                    item {
                        SoftPill(
                            label = "All",
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            testTag = "filter_chip_ALL"
                        )
                    }
                    items(PaymentMethod.entries.toList()) { method ->
                        SoftPill(
                            label = method.shortLabel,
                            selected = selectedFilter == method.code,
                            onClick = { selectedFilter = method.code },
                            leadingIcon = null,
                            testTag = "filter_chip_${method.code}"
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                PosCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Rounded.Receipt,
                        title = if (transactions.isEmpty()) "No sales recorded yet" else "Nothing matches this filter",
                        message = if (transactions.isEmpty()) {
                            "Complete a sale from the Sell tab. Receipts stay on this device, even without internet."
                        } else {
                            "Try a different search term or payment filter."
                        }
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { sale ->
                TransactionRow(
                    sale = sale,
                    currencySymbol = symbol,
                    onClick = { onViewReceipt(sale) }
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(
    sale: SaleTransaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val accent = paymentAccent(sale.paymentMethod)

    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${sale.id}"),
        contentPadding = PaddingValues(PosSpace.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PaymentMethod.fromCode(sale.paymentMethod).shortLabel.take(2).uppercase(),
                    style = PosType.labelMedium,
                    color = accent
                )
            }

            Spacer(Modifier.width(PosSpace.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sale.invoiceNumber,
                    style = PosType.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(Format.relative(sale.timestamp))
                        if (sale.metaInfo.isNotBlank()) append(" · ${sale.metaInfo}")
                    },
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = PaymentMethod.fromCode(sale.paymentMethod).shortLabel,
                        style = PosType.labelSmall,
                        color = accent
                    )
                    if (sale.transactionRef.isNotBlank()) {
                        Spacer(Modifier.width(PosSpace.xs))
                        Text(
                            text = sale.transactionRef,
                            style = PosType.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(PosSpace.sm))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Format.money(sale.grandTotal, 0, currencySymbol),
                    style = PosType.moneySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                if (sale.paymentMethod == "CASH") {
                    StatusPill(text = "Settled", accent = accent)
                } else {
                    StatusPill(text = "Verified", accent = accent)
                }
            }

            RowChevron(tint = MaterialTheme.colorScheme.outline)
        }
    }
}