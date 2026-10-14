package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.SaleTransaction
import com.example.ui.theme.LineSubtle
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType
import com.example.ui.theme.ThermalPaperBg
import com.example.ui.theme.ThermalReceiptFaint
import com.example.ui.theme.ThermalReceiptInk
import com.example.ui.util.Format

@Composable
fun ThermalReceiptModal(
    sale: SaleTransaction,
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onNewSale: () -> Unit
) {
    val context = LocalContext.current
    var paperWidth by remember { mutableStateOf(settings.printerWidth) }

    val formattedDate = remember(sale.timestamp) { Format.dateTime(sale.timestamp) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("thermal_receipt_modal"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(PosSpace.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sale complete",
                            style = PosType.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Print, share or start the next sale",
                            style = PosType.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SoftIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        size = 40.dp,
                        testTag = "receipt_close_button"
                    )
                }

                Spacer(Modifier.height(PosSpace.lg))

                Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
                    SoftPill(
                        label = "58 mm",
                        selected = paperWidth == "58mm",
                        onClick = { paperWidth = "58mm" },
                        testTag = "chip_receipt_58mm"
                    )
                    SoftPill(
                        label = "80 mm",
                        selected = paperWidth == "80mm",
                        onClick = { paperWidth = "80mm" },
                        testTag = "chip_receipt_80mm"
                    )
                }

                Spacer(Modifier.height(PosSpace.xl))

                // The physical-feeling thermal slip.
                Surface(
                    modifier = Modifier
                        .width(if (paperWidth == "58mm") 286.dp else 348.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = ThermalPaperBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PosSpace.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings.businessName.uppercase(),
                            style = PosType.receiptStrong,
                            color = ThermalReceiptInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = settings.address,
                            style = PosType.receipt,
                            color = ThermalReceiptFaint,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "PAN ${settings.panVatNumber} · ${settings.phone}",
                            style = PosType.receipt,
                            color = ThermalReceiptFaint,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(PosSpace.sm))
                        ReceiptDivider()

                        ReceiptLine("INVOICE", sale.invoiceNumber, strong = true)
                        ReceiptLine("DATE", formattedDate)
                        ReceiptLine("CHANNEL", sale.metaInfo.ifBlank { sale.industryMode })
                        ReceiptLine("STATUS", sale.paymentStatus)

                        Spacer(Modifier.height(PosSpace.xxs))
                        ReceiptDivider()

                        receiptItems(sale).forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = line.first,
                                    style = PosType.receipt,
                                    color = ThermalReceiptInk,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                                Spacer(Modifier.width(PosSpace.xs))
                                Text(
                                    text = line.second,
                                    style = PosType.receipt,
                                    color = ThermalReceiptInk,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Spacer(Modifier.height(PosSpace.xxs))
                        ReceiptDivider()
                        ReceiptLine("SUBTOTAL", Format.money(sale.subtotal, 2))
                        if (sale.serviceCharge > 0) {
                            ReceiptLine("SERVICE CHARGE", Format.money(sale.serviceCharge, 2))
                        }
                        if (sale.vatTaxAmount > 0) {
                            ReceiptLine("VAT", Format.money(sale.vatTaxAmount, 2))
                        }
                        Spacer(Modifier.height(PosSpace.xxs))
                        ReceiptDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GRAND TOTAL",
                                style = PosType.receiptStrong.copy(fontSize = 13.sp),
                                color = ThermalReceiptInk
                            )
                            Text(
                                text = Format.money(sale.grandTotal, 2, settings.currencySymbol),
                                style = PosType.moneyTiny,
                                color = ThermalReceiptInk
                            )
                        }

                        ReceiptLine("PAID VIA", PaymentMethod.fromCode(sale.paymentMethod).displayName)
                        if (sale.cashTendered > 0) {
                            ReceiptLine("CASH RECEIVED", Format.money(sale.cashTendered, 2))
                            ReceiptLine("CHANGE RETURNED", Format.money(sale.cashChange, 2))
                        }
                        if (sale.transactionRef.isNotBlank()) {
                            ReceiptLine("REFERENCE", sale.transactionRef)
                        }

                        Spacer(Modifier.height(PosSpace.md))

                        QrCodeCanvas(
                            payload = "https://ird.gov.np/verify?pan=${settings.panVatNumber}&inv=${sale.invoiceNumber}&total=${sale.grandTotal}",
                            sizeDp = 76.dp
                        )

                        Spacer(Modifier.height(PosSpace.sm))
                        Text(
                            text = "Thank you · Visit again",
                            style = PosType.receipt,
                            color = ThermalReceiptInk,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Powered by SajiloPOS",
                            style = PosType.receipt,
                            color = ThermalReceiptFaint,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(PosSpace.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)
                ) {
                    PrimaryButton(
                        text = "Print",
                        icon = Icons.Rounded.Print,
                        onClick = { triggerAndroidPrint(context, sale, settings, formattedDate) },
                        height = 50.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button")
                    )
                    GhostButton(
                        text = "Share",
                        icon = Icons.Rounded.Share,
                        onClick = {
                            val shareText = generatePlainTextReceipt(sale, settings, formattedDate)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Share receipt"))
                        },
                        height = 50.dp,
                        modifier = Modifier.testTag("share_receipt_button")
                    )
                    GhostButton(
                        text = "ESC/POS",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = {
                            val payload = generateEscPosCommandString(sale, settings, formattedDate)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ESC/POS payload", payload))
                            Toast.makeText(context, "ESC/POS bytes copied for your Bluetooth printer", Toast.LENGTH_SHORT).show()
                        },
                        height = 50.dp,
                        modifier = Modifier.testTag("copy_escpos_button")
                    )
                }

                Spacer(Modifier.height(PosSpace.md))

                PrimaryButton(
                    text = "Start new sale",
                    icon = Icons.Rounded.Done,
                    onClick = onNewSale,
                    container = MaterialTheme.colorScheme.onSurface,
                    content = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_sale_button")
                )
            }
        }
    }
}

/** Splits the stored plaintext summary into label / amount pairs for the slip. */
private fun receiptItems(sale: SaleTransaction): List<Pair<String, String>> =
    sale.itemsSummary
        .lines()
        .filter { it.isNotBlank() }
        .map { line ->
            val amount = line.substringAfterLast("@ NPR ", "")
            val label = line.substringBeforeLast("@ NPR ")
            if (amount.isBlank()) label to "" else label to amount
        }

@Composable
private fun ReceiptDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 4.dp)
            .background(ThermalReceiptFaint.copy(alpha = 0.4f))
    )
}

@Composable
private fun ReceiptLine(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PosType.receipt,
            color = ThermalReceiptFaint
        )
        Spacer(Modifier.width(PosSpace.xs))
        Text(
            text = value,
            style = if (strong) PosType.receiptStrong else PosType.receipt,
            color = ThermalReceiptInk,
            textAlign = TextAlign.End
        )
    }
}

private fun triggerAndroidPrint(
    context: Context,
    sale: SaleTransaction,
    settings: BusinessSettings,
    formattedDate: String
) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val adapter = webView.createPrintDocumentAdapter("Receipt-${sale.invoiceNumber}")
                printManager.print(
                    "Receipt-${sale.invoiceNumber}",
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                        .build()
                )
            }
        }

        val itemRows = sale.itemsSummary
            .lines()
            .filter { it.isNotBlank() }
            .joinToString("") { line ->
                val amount = line.substringAfterLast("@ NPR ", "")
                val label = line.substringBeforeLast("@ NPR ")
                "<div class='row'><span>$label</span><span>${if (amount.isBlank()) "" else "NPR $amount"}</span></div>"
            }

        val html = """
            <html><head><style>
                body { font-family: monospace; font-size: 11px; padding: 10px; margin: 0; color:#111; }
                .center { text-align: center; }
                .row { display: flex; justify-content: space-between; gap: 12px; }
                .bold { font-weight: bold; }
                hr { border: 0; border-top: 1px dashed #999; margin: 6px 0; }
            </style></head><body>
                <div class="center bold">${settings.businessName}</div>
                <div class="center">${settings.address}</div>
                <div class="center">PAN ${settings.panVatNumber} · ${settings.phone}</div>
                <hr>
                <div class="row"><span>INVOICE</span><span class="bold">${sale.invoiceNumber}</span></div>
                <div class="row"><span>DATE</span><span>$formattedDate</span></div>
                <div class="row"><span>CHANNEL</span><span>${sale.metaInfo}</span></div>
                <hr>
                $itemRows
                <hr>
                <div class="row"><span>SUBTOTAL</span><span>NPR ${"%.2f".format(sale.subtotal)}</span></div>
                ${if (sale.serviceCharge > 0) "<div class='row'><span>SERVICE CHARGE</span><span>NPR ${"%.2f".format(sale.serviceCharge)}</span></div>" else ""}
                ${if (sale.vatTaxAmount > 0) "<div class='row'><span>VAT</span><span>NPR ${"%.2f".format(sale.vatTaxAmount)}</span></div>" else ""}
                <hr>
                <div class="row bold" style="font-size:14px;"><span>GRAND TOTAL</span><span>NPR ${"%.2f".format(sale.grandTotal)}</span></div>
                <div class="row"><span>PAID VIA</span><span>${PaymentMethod.fromCode(sale.paymentMethod).displayName}</span></div>
                <div class="row"><span>REF</span><span>${sale.transactionRef}</span></div>
                <hr>
                <div class="center">Thank you · Visit again</div>
                <div class="center">Powered by SajiloPOS</div>
            </body></html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * ESC/POS byte plan for Bluetooth thermal printers
 * (Sunmi, Xprinter, Rongta, POS-58 / POS-80).
 */
private fun generateEscPosCommandString(
    sale: SaleTransaction,
    settings: BusinessSettings,
    dateStr: String
): String = buildString {
    appendLine("/* ESC/POS stream for 58mm / 80mm Bluetooth thermal printers */")
    appendLine("1B 40            initialize")
    appendLine("1B 61 01         center align")
    appendLine("[${settings.businessName}]")
    appendLine("[${settings.address}]")
    appendLine("[PAN ${settings.panVatNumber} | ${settings.phone}]")
    appendLine("1B 61 00         left align")
    appendLine("--------------------------------")
    appendLine("INV ${sale.invoiceNumber} | $dateStr")
    appendLine("--------------------------------")
    append(sale.itemsSummary)
    appendLine()
    appendLine("--------------------------------")
    appendLine("TOTAL NPR ${"%.2f".format(sale.grandTotal)}")
    appendLine("VAT NPR ${"%.2f".format(sale.vatTaxAmount)}")
    appendLine("PAID ${sale.paymentMethod} (${sale.paymentStatus})")
    appendLine("REF ${sale.transactionRef}")
    appendLine("1B 61 01         center align")
    appendLine("Thank you · Visit again")
    appendLine("1D 56 41 03      cut paper")
}

private fun generatePlainTextReceipt(
    sale: SaleTransaction,
    settings: BusinessSettings,
    dateStr: String
): String = buildString {
    appendLine("*${settings.businessName}*")
    appendLine(settings.address)
    appendLine("PAN/VAT: ${settings.panVatNumber} | Tel: ${settings.phone}")
    appendLine("==============================")
    appendLine("Invoice: ${sale.invoiceNumber}")
    appendLine("Date: $dateStr")
    appendLine("Channel: ${sale.metaInfo.ifBlank { sale.industryMode }}")
    appendLine("------------------------------")
    appendLine(sale.itemsSummary)
    appendLine("------------------------------")
    appendLine("Subtotal: NPR ${"%.2f".format(sale.subtotal)}")
    if (sale.serviceCharge > 0) appendLine("Service charge: NPR ${"%.2f".format(sale.serviceCharge)}")
    if (sale.vatTaxAmount > 0) appendLine("VAT: NPR ${"%.2f".format(sale.vatTaxAmount)}")
    appendLine("TOTAL: NPR ${"%.2f".format(sale.grandTotal)}")
    appendLine("Paid via: ${PaymentMethod.fromCode(sale.paymentMethod).displayName}")
    if (sale.transactionRef.isNotBlank()) appendLine("Ref: ${sale.transactionRef}")
    appendLine("==============================")
    appendLine("Thank you! Visit again.")
    append("Powered by SajiloPOS")
}