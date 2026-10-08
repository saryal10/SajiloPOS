package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessSettings
import com.example.data.model.SaleTransaction
import com.example.ui.theme.ESewaGreen
import com.example.ui.theme.FonepayRed
import com.example.ui.theme.KhaltiPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    transactions: List<SaleTransaction>,
    settings: BusinessSettings,
    onViewReceipt: (SaleTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "CASH", "FONEPAY", "ESEWA", "KHALTI"

    val totalRevenue = remember(transactions) {
        transactions.sumOf { it.grandTotal }
    }

    val cashRevenue = remember(transactions) {
        transactions.filter { it.paymentMethod == "CASH" }.sumOf { it.grandTotal }
    }

    val digitalRevenue = remember(transactions) {
        transactions.filter { it.paymentMethod != "CASH" }.sumOf { it.grandTotal }
    }

    val filteredList = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { t ->
            val matchesQuery = searchQuery.isBlank() ||
                t.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                t.metaInfo.contains(searchQuery, ignoreCase = true) ||
                t.transactionRef.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == "All" || t.paymentMethod == selectedFilter
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Daily Sales Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Total Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${settings.currencySymbol} ${"%.0f".format(totalRevenue)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(text = "${transactions.size} orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Digital Wallets", fontSize = 11.sp, color = Color(0xFF1B5E20))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${settings.currencySymbol} ${"%.0f".format(digitalRevenue)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(text = "Fonepay/eSewa", fontSize = 10.sp, color = Color(0xFF2E7D32))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Cash Register", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${settings.currencySymbol} ${"%.0f".format(cashRevenue)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = "Cash Drawer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Invoice # (e.g. INV-2026...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transactions_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Payment Method Filter Tabs
        val filterOptions = listOf("All", "CASH", "FONEPAY", "ESEWA", "KHALTI")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = when (filter) {
                                "All" -> "All Transactions"
                                "CASH" -> "Cash Only"
                                "FONEPAY" -> "Fonepay QR"
                                "ESEWA" -> "eSewa"
                                "KHALTI" -> "Khalti"
                                else -> filter
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_$filter")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transactions List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No sales recorded yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Complete a sale in POS to view transactions here",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("transactions_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { sale ->
                    TransactionItemCard(
                        sale = sale,
                        currencySymbol = settings.currencySymbol,
                        onClick = { onViewReceipt(sale) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(
    sale: SaleTransaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dateStr = remember(sale.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(sale.timestamp))
    }

    val methodColor = when (sale.paymentMethod) {
        "CASH" -> Color(0xFF2E7D32)
        "FONEPAY" -> FonepayRed
        "ESEWA" -> ESewaGreen
        "KHALTI" -> KhaltiPurple
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${sale.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(methodColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sale.paymentMethod,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$dateStr • ${sale.industryMode}${if (sale.metaInfo.isNotBlank()) " • " + sale.metaInfo else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sale.transactionRef.isNotBlank()) {
                    Text(
                        text = "Ref: ${sale.transactionRef}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol ${"%.2f".format(sale.grandTotal)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Receipt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Receipt",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
