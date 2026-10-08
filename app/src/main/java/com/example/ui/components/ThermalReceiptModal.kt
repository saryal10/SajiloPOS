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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessSettings
import com.example.data.model.SaleTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThermalReceiptModal(
    sale: SaleTransaction,
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onNewSale: () -> Unit
) {
    val context = LocalContext.current
    var selectedWidth by remember { mutableStateOf(settings.printerWidth) } // "58mm" or "80mm"
    var showRawEscPos by remember { mutableStateOf(false) }

    val formattedDate = remember(sale.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(sale.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("thermal_receipt_modal"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header & Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thermal Receipt Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("receipt_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Width Selector (58mm vs 80mm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = selectedWidth == "58mm",
                        onClick = { selectedWidth = "58mm" },
                        label = { Text("58mm Roll (Compact)") },
                        modifier = Modifier.testTag("chip_receipt_58mm")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedWidth == "80mm",
                        onClick = { selectedWidth = "80mm" },
                        label = { Text("80mm Roll (Standard)") },
                        modifier = Modifier.testTag("chip_receipt_80mm")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Realistic Thermal Paper Slip
                Card(
                    modifier = Modifier
                        .width(if (selectedWidth == "58mm") 280.dp else 340.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDFD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Business Header
                        Text(
                            text = settings.businessName.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = settings.address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "PAN/VAT: ${settings.panVatNumber} | Tel: ${settings.phone}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )

                        Text(
                            text = "================================",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Invoice Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("INVOICE: ${sale.invoiceNumber}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DATE: $formattedDate", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF475569))
                        }
                        if (sale.metaInfo.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("REF/MODE: ${sale.metaInfo}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF475569))
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ITEM / DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("TOTAL (NPR)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Itemized Content
                        Text(
                            text = sale.itemsSummary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Totals Breakdown
                        ReceiptLine(label = "SUBTOTAL:", value = "NPR ${"%.2f".format(sale.subtotal)}")
                        if (sale.serviceCharge > 0) {
                            ReceiptLine(label = "SERVICE CHARGE (10%):", value = "NPR ${"%.2f".format(sale.serviceCharge)}")
                        }
                        if (sale.vatTaxAmount > 0) {
                            ReceiptLine(label = "TAXABLE VAT (13%):", value = "NPR ${"%.2f".format(sale.vatTaxAmount)}")
                        }

                        Text(
                            text = "================================",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GRAND TOTAL:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${settings.currencySymbol} ${"%.2f".format(sale.grandTotal)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptLine(label = "PAID VIA:", value = "${sale.paymentMethod} (${sale.paymentStatus})")

                        if (sale.cashTendered > 0) {
                            ReceiptLine(label = "CASH TENDERED:", value = "NPR ${"%.2f".format(sale.cashTendered)}")
                            ReceiptLine(label = "CHANGE RETURNED:", value = "NPR ${"%.2f".format(sale.cashChange)}")
                        }

                        ReceiptLine(label = "TXN REF:", value = sale.transactionRef)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Nepal IRD Compliance Mock QR
                        QrCodeCanvas(
                            payload = "ird.gov.np/verify?pan=${settings.panVatNumber}&inv=${sale.invoiceNumber}&total=${sale.grandTotal}",
                            sizeDp = 80.dp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "** THANK YOU! VISIT AGAIN **\nPowered by SajiloPOS Nepal",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Print, Copy ESC/POS, Share, New Sale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            triggerAndroidPrint(context, sale, settings, formattedDate)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val payload = generateEscPosCommandString(sale, settings, formattedDate)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ESC/POS Payload", payload))
                            Toast.makeText(context, "ESC/POS command payload copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_escpos_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ESC/POS", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = generatePlainTextReceipt(sale, settings, formattedDate)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Receipt Voucher"))
                        },
                        modifier = Modifier.testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onNewSale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_sale_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start New Sale", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
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
        if (printManager != null) {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter(sale.invoiceNumber)
                    printManager.print(
                        "Receipt-${sale.invoiceNumber}",
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                            .build()
                    )
                }
            }

            val html = """
                <html>
                <head>
                    <style>
                        body { font-family: monospace; font-size: 11px; padding: 10px; width: 280px; margin: auto; }
                        .center { text-align: center; }
                        .line { border-top: 1px dashed #000; margin: 6px 0; }
                        .flex { display: flex; justify-content: space-between; }
                        .bold { font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="center bold">${settings.businessName}</div>
                    <div class="center">${settings.address}</div>
                    <div class="center">PAN/VAT: ${settings.panVatNumber} | Tel: ${settings.phone}</div>
                    <div class="line"></div>
                    <div class="flex"><span>INVOICE: ${sale.invoiceNumber}</span></div>
                    <div class="flex"><span>DATE: $formattedDate</span></div>
                    <div class="flex"><span>MODE: ${sale.industryMode}</span></div>
                    <div class="line"></div>
                    <pre style="font-size:10px;">${sale.itemsSummary}</pre>
                    <div class="line"></div>
                    <div class="flex"><span>Subtotal:</span><span>NPR ${"%.2f".format(sale.subtotal)}</span></div>
                    ${if (sale.serviceCharge > 0) "<div class='flex'><span>Service Charge:</span><span>NPR ${"%.2f".format(sale.serviceCharge)}</span></div>" else ""}
                    ${if (sale.vatTaxAmount > 0) "<div class='flex'><span>VAT (13%):</span><span>NPR ${"%.2f".format(sale.vatTaxAmount)}</span></div>" else ""}
                    <div class="line"></div>
                    <div class="flex bold" style="font-size:13px;"><span>GRAND TOTAL:</span><span>NPR ${"%.2f".format(sale.grandTotal)}</span></div>
                    <div class="flex"><span>Paid Via:</span><span>${sale.paymentMethod}</span></div>
                    <div class="flex"><span>Ref:</span><span>${sale.transactionRef}</span></div>
                    <div class="line"></div>
                    <div class="center">Thank you! Visit Again.</div>
                    <div class="center" style="font-size:9px;">SajiloPOS System Nepal</div>
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } else {
            Toast.makeText(context, "Print service not available on device", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generates raw ESC/POS command hexadecimal sequence for Bluetooth Thermal Printers
 * (Compatible with Sunmi, Xprinter, Rongta, POS-58, POS-80)
 */
private fun generateEscPosCommandString(
    sale: SaleTransaction,
    settings: BusinessSettings,
    dateStr: String
): String {
    return buildString {
        append("/* ESC/POS RAW HEX STREAM FOR BLUETOOTH THERMAL PRINTER */\n")
        append("1B 40 ") // Initialize printer
        append("1B 61 01 ") // Center align
        append("1B 21 30 ") // Double height/width font
        append("[${settings.businessName}]\n")
        append("1B 21 00 ") // Normal font
        append("[${settings.address}]\n")
        append("[PAN: ${settings.panVatNumber} | ${settings.phone}]\n")
        append("1B 61 00 ") // Left align
        append("--------------------------------\n")
        append("INV: ${sale.invoiceNumber} | $dateStr\n")
        append("--------------------------------\n")
        append(sale.itemsSummary.replace("\n", "\n"))
        append("\n--------------------------------\n")
        append("TOTAL: NPR ${"%.2f".format(sale.grandTotal)}\n")
        append("METHOD: ${sale.paymentMethod} (${sale.paymentStatus})\n")
        append("REF: ${sale.transactionRef}\n")
        append("1B 61 01 ") // Center align
        append("Thank you for choosing us!\n")
        append("1D 56 41 03 ") // Paper Cut command (GS V 65 3)
    }
}

private fun generatePlainTextReceipt(
    sale: SaleTransaction,
    settings: BusinessSettings,
    dateStr: String
): String {
    return """
        *${settings.businessName}*
        ${settings.address}
        PAN/VAT: ${settings.panVatNumber} | Tel: ${settings.phone}
        ==============================
        Invoice: ${sale.invoiceNumber}
        Date: $dateStr
        Ref/Mode: ${sale.metaInfo}
        ------------------------------
        ${sale.itemsSummary}
        ------------------------------
        Subtotal: NPR ${"%.2f".format(sale.subtotal)}
        ${if (sale.serviceCharge > 0) "Service Charge (10%): NPR ${"%.2f".format(sale.serviceCharge)}\n" else ""}${if (sale.vatTaxAmount > 0) "VAT (13%): NPR ${"%.2f".format(sale.vatTaxAmount)}\n" else ""}Total: NPR ${"%.2f".format(sale.grandTotal)}
        Paid Via: ${sale.paymentMethod} (${sale.paymentStatus})
        Ref: ${sale.transactionRef}
        ==============================
        Thank you! Visit again.
        Powered by SajiloPOS
    """.trimIndent()
}
