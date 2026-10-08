package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessSettings
import com.example.data.model.PaymentMethod
import com.example.ui.theme.ESewaGreen
import com.example.ui.theme.FonepayRed
import com.example.ui.theme.KhaltiPurple
import com.example.ui.theme.PosSlate900
import com.example.ui.viewmodel.PaymentVerificationState

@Composable
fun PaymentModal(
    grandTotal: Double,
    subtotal: Double,
    vatAmount: Double,
    serviceChargeAmount: Double,
    settings: BusinessSettings,
    selectedMethod: PaymentMethod,
    cashTendered: String,
    verificationState: PaymentVerificationState,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCashTenderedChange: (String) -> Unit,
    onVerifyDigitalPayment: () -> Unit,
    onCompleteCashSale: () -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        PaymentMethod.CASH,
        PaymentMethod.FONEPAY,
        PaymentMethod.ESEWA,
        PaymentMethod.KHALTI
    )
    val selectedIndex = methods.indexOf(selectedMethod).coerceAtLeast(0)

    val tenderedNum = cashTendered.toDoubleOrNull() ?: 0.0
    val changeDue = (tenderedNum - grandTotal).coerceAtLeast(0.0)

    val brandColor = when (selectedMethod) {
        PaymentMethod.CASH -> Color(0xFF2E7D32)
        PaymentMethod.FONEPAY -> FonepayRed
        PaymentMethod.ESEWA -> ESewaGreen
        PaymentMethod.KHALTI -> KhaltiPurple
    }

    Dialog(onDismissRequest = {
        if (!verificationState.isVerifying) onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("payment_modal"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Checkout & Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = settings.businessName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !verificationState.isVerifying,
                        modifier = Modifier.testTag("payment_modal_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Amount Due:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${settings.currencySymbol} ${"%.2f".format(grandTotal)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (vatAmount > 0 || serviceChargeAmount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal: ${settings.currencySymbol}${"%.2f".format(subtotal)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (serviceChargeAmount > 0) {
                                    Text(
                                        text = "SC(10%): ${settings.currencySymbol}${"%.2f".format(serviceChargeAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (vatAmount > 0) {
                                    Text(
                                        text = "VAT(13%): ${settings.currencySymbol}${"%.2f".format(vatAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector Tabs
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    methods.forEachIndexed { index, method ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { onSelectMethod(method) },
                            text = {
                                Text(
                                    text = when (method) {
                                        PaymentMethod.CASH -> "Cash"
                                        PaymentMethod.FONEPAY -> "Fonepay"
                                        PaymentMethod.ESEWA -> "eSewa"
                                        PaymentMethod.KHALTI -> "Khalti"
                                    },
                                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_payment_${method.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Content
                when (selectedMethod) {
                    PaymentMethod.CASH -> {
                        CashPaymentContent(
                            grandTotal = grandTotal,
                            cashTendered = cashTendered,
                            changeDue = changeDue,
                            currencySymbol = settings.currencySymbol,
                            onCashTenderedChange = onCashTenderedChange,
                            onCompleteCashSale = onCompleteCashSale
                        )
                    }
                    else -> {
                        DigitalWalletPaymentContent(
                            method = selectedMethod,
                            grandTotal = grandTotal,
                            settings = settings,
                            brandColor = brandColor,
                            verificationState = verificationState,
                            onVerify = onVerifyDigitalPayment
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashPaymentContent(
    grandTotal: Double,
    cashTendered: String,
    changeDue: Double,
    currencySymbol: String,
    onCashTenderedChange: (String) -> Unit,
    onCompleteCashSale: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter Cash Received from Customer:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cashTendered,
            onValueChange = onCashTenderedChange,
            label = { Text("Tendered Cash Amount ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(Icons.Default.Money, contentDescription = null, tint = Color(0xFF2E7D32))
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cash_tendered_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Nepali Rupee Notes Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickNotes = listOf(
                "Exact" to grandTotal.toInt(),
                "+50" to ((grandTotal / 50).toInt() + 1) * 50,
                "+100" to ((grandTotal / 100).toInt() + 1) * 100,
                "+500" to 500,
                "+1000" to 1000
            )

            quickNotes.forEach { (label, amt) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onCashTenderedChange(amt.toString()) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (label == "Exact") "Exact" else "रू $amt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Change Due Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (changeDue > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change Due to Customer:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$currencySymbol ${"%.2f".format(changeDue)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onCompleteCashSale,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_cash_sale_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Cash Sale & Print Receipt", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DigitalWalletPaymentContent(
    method: PaymentMethod,
    grandTotal: Double,
    settings: BusinessSettings,
    brandColor: Color,
    verificationState: PaymentVerificationState,
    onVerify: () -> Unit
) {
    val qrPayload = "nepalqr://${method.code.lowercase()}?merchant=${settings.panVatNumber}&amount=${"%.2f".format(grandTotal)}&name=${settings.businessName.replace(" ", "_")}"
    val badgeLabel = when (method) {
        PaymentMethod.ESEWA -> "eSewa"
        PaymentMethod.FONEPAY -> "fonepay"
        PaymentMethod.KHALTI -> "khalti"
        else -> "QR"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Merchant QR Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(brandColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${method.displayName} • Instant QR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic QR Code Canvas
                QrCodeCanvas(
                    payload = qrPayload,
                    sizeDp = 180.dp,
                    centerBadgeText = badgeLabel,
                    centerBadgeColor = brandColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Scan to Pay: ${settings.currencySymbol} ${"%.2f".format(grandTotal)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = brandColor
                )
                Text(
                    text = "Merchant PAN: ${settings.panVatNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verification Status Live Progress
        AnimatedVisibility(visible = verificationState.isVerifying || verificationState.isSuccess) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (verificationState.isSuccess) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (verificationState.isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = brandColor
                        )
                    } else if (verificationState.isSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = verificationState.currentStep,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (verificationState.isSuccess) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                        )
                        if (verificationState.transactionRef.isNotBlank()) {
                            Text(
                                text = "Ref: ${verificationState.transactionRef}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Verify Status Action Button
        Button(
            onClick = onVerify,
            enabled = !verificationState.isVerifying && !verificationState.isSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("verify_payment_status_button"),
            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (verificationState.isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Polling Payment Status...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Payment Status (Simulate 205 OK)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
