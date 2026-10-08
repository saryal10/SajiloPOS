package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessSettings
import com.example.data.model.PaymentMethod
import com.example.ui.theme.PosSpace
import com.example.ui.util.Format
import com.example.ui.theme.PosType
import com.example.ui.theme.SuccessGreen
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
    val symbol = settings.currencySymbol
    val methods = listOf(
        PaymentMethod.CASH,
        PaymentMethod.FONEPAY,
        PaymentMethod.ESEWA,
        PaymentMethod.KHALTI
    )

    val tendered = cashTendered.toDoubleOrNull() ?: 0.0
    val changeDue = (tendered - grandTotal).coerceAtLeast(0.0)
    val accent = paymentAccent(selectedMethod.code)
    val locked = verificationState.isVerifying

    Dialog(onDismissRequest = { if (!locked) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_modal"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(PosSpace.xl)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Checkout",
                            style = PosType.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = settings.businessName,
                            style = PosType.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    SoftIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        size = 40.dp,
                        testTag = "payment_modal_close"
                    )
                }

                Spacer(Modifier.height(PosSpace.xl))

                // Amount due — the single most important element on this screen.
                PosCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = MaterialTheme.colorScheme.primaryContainer,
                    contentPadding = PaddingValues(PosSpace.lg)
                ) {
                    Text(
                        text = "AMOUNT DUE",
                        style = PosType.overline,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(PosSpace.xs))
                    Text(
                        text = Format.money(grandTotal, 2, symbol),
                        style = PosType.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(PosSpace.md))
                    HairlineDivider()
                    Spacer(Modifier.height(PosSpace.md))
                    BreakdownLine("Subtotal", Format.money(subtotal, 2, symbol))
                    if (serviceChargeAmount > 0) {
                        Spacer(Modifier.height(PosSpace.xxs))
                        BreakdownLine("Service charge", Format.money(serviceChargeAmount, 2, symbol))
                    }
                    if (vatAmount > 0) {
                        Spacer(Modifier.height(PosSpace.xxs))
                        BreakdownLine(
                            "VAT (${Format.percent(settings.vatRatePercent)})",
                            Format.money(vatAmount, 2, symbol)
                        )
                    }
                }

                Spacer(Modifier.height(PosSpace.xl))

                Text(
                    text = "Payment method",
                    style = PosType.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(PosSpace.md))

                LazyMethodGrid(
                    methods = methods,
                    selected = selectedMethod,
                    enabled = !locked,
                    onSelect = onSelectMethod
                )

                Spacer(Modifier.height(PosSpace.xl))

                when (selectedMethod) {
                    PaymentMethod.CASH -> CashPaymentPanel(
                        grandTotal = grandTotal,
                        cashTendered = cashTendered,
                        changeDue = changeDue,
                        currencySymbol = symbol,
                        onCashTenderedChange = onCashTenderedChange,
                        onCompleteCashSale = onCompleteCashSale
                    )

                    else -> DigitalWalletPanel(
                        method = selectedMethod,
                        grandTotal = grandTotal,
                        settings = settings,
                        accent = accent,
                        verificationState = verificationState,
                        onVerify = onVerifyDigitalPayment
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyMethodGrid(
    methods: List<PaymentMethod>,
    selected: PaymentMethod,
    enabled: Boolean,
    onSelect: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
        methods.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                row.forEach { method ->
                    val isSelected = method == selected
                    val accent = paymentAccent(method.code)
                    PosCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = enabled) { onSelect(method) }
                            .testTag("tab_payment_${method.name.lowercase()}"),
                        containerColor = if (isSelected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                        borderColor = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                        contentPadding = PaddingValues(PosSpace.md)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method.shortLabel.take(1),
                                    style = PosType.labelMedium,
                                    color = accent
                                )
                            }
                            Spacer(Modifier.width(PosSpace.sm))
                            Text(
                                text = method.shortLabel,
                                style = PosType.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = PosType.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = PosType.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CashPaymentPanel(
    grandTotal: Double,
    cashTendered: String,
    changeDue: Double,
    currencySymbol: String,
    onCashTenderedChange: (String) -> Unit,
    onCompleteCashSale: () -> Unit
) {
    val tenderedValue = cashTendered.toDoubleOrNull() ?: 0.0
    val isShort = tenderedValue > 0 && tenderedValue < grandTotal

    Column {
        Text(
            text = "Cash received",
            style = PosType.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(PosSpace.md))

        CashInput(
            value = cashTendered,
            onValueChange = onCashTenderedChange,
            currencySymbol = currencySymbol
        )

        Spacer(Modifier.height(PosSpace.md))

        // Quick tender chips tuned to Nepali denominations.
        val chips = remember(grandTotal) {
            listOf(
                "Exact" to grandTotal,
                "+50" to (((grandTotal / 50).toInt() + 1) * 50).toDouble(),
                "+100" to (((grandTotal / 100).toInt() + 1) * 100).toDouble(),
                "+500" to 500.0,
                "+1000" to 1000.0
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
            chips.forEach { (label, amount) ->
                SoftPill(
                    label = if (label == "Exact") "Exact" else Format.money(amount, 0, currencySymbol),
                    selected = false,
                    onClick = { onCashTenderedChange(Format.plain(amount, 2)) },
                    modifier = Modifier.weight(1f),
                    testTag = "cash_chip_$label"
                )
            }
        }

        Spacer(Modifier.height(PosSpace.lg))

        PosCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (isShort) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            borderColor = if (isShort) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.outlineVariant,
            contentPadding = PaddingValues(PosSpace.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Change to return",
                        style = PosType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Format.money(changeDue, 2, currencySymbol),
                        style = PosType.moneyLarge,
                        color = if (isShort) MaterialTheme.colorScheme.error else SuccessGreen
                    )
                }
                if (isShort) {
                    Text(
                        text = "Short by ${Format.money(grandTotal - tenderedValue, 2, currencySymbol)}",
                        style = PosType.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(Modifier.height(PosSpace.lg))

        PrimaryButton(
            text = "Complete cash sale",
            icon = Icons.Rounded.CheckCircle,
            onClick = onCompleteCashSale,
            enabled = !isShort,
            container = SuccessGreen,
            content = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_cash_sale_button")
        )
    }
}

@Composable
private fun CashInput(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text("Amount in NPR", style = PosType.bodySmall) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Money,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = PosType.moneyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_tendered_input")
    )
}

@Composable
private fun DigitalWalletPanel(
    method: PaymentMethod,
    grandTotal: Double,
    settings: BusinessSettings,
    accent: Color,
    verificationState: PaymentVerificationState,
    onVerify: () -> Unit
) {
    val payload = buildString {
        append("nepalqr://")
        append(method.name.lowercase())
        append("?pan=").append(settings.panVatNumber)
        append("&amt=").append(String.format(java.util.Locale.US, "%.2f", grandTotal))
        append("&name=").append(settings.businessName.replace(" ", "_"))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        PosCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(PosSpace.xl)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QrCodeCanvas(
                    payload = payload,
                    sizeDp = 176.dp,
                    centerBadgeText = method.shortLabel,
                    centerBadgeColor = accent
                )
                Spacer(Modifier.height(PosSpace.lg))
                Text(
                    text = "Scan to pay",
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PosSpace.xxs))
                Text(
                    text = Format.money(grandTotal, 2, settings.currencySymbol),
                    style = PosType.moneyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(PosSpace.xs))
                Text(
                    text = "PAN ${settings.panVatNumber}",
                    style = PosType.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(PosSpace.lg))

        AnimatedVisibility(visible = verificationState.isVerifying || verificationState.isSuccess) {
            PosCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (verificationState.isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                borderColor = if (verificationState.isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                contentPadding = PaddingValues(PosSpace.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (verificationState.isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen
                        )
                    }
                    Spacer(Modifier.width(PosSpace.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = verificationState.currentStep,
                            style = PosType.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (verificationState.transactionRef.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ref ${verificationState.transactionRef}",
                                style = PosType.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(PosSpace.lg))

        PrimaryButton(
            text = if (verificationState.isVerifying) "Verifying payment…" else "I have paid · verify",
            icon = if (verificationState.isVerifying) null else Icons.Rounded.Sync,
            onClick = onVerify,
            enabled = !verificationState.isVerifying && !verificationState.isSuccess,
            container = accent,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verify_payment_status_button")
        )

        Spacer(Modifier.height(PosSpace.sm))

        Text(
            text = "The sale is settled and stock is updated only after the gateway confirms the payment.",
            style = PosType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}