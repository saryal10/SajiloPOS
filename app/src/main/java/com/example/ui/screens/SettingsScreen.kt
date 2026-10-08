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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import com.example.data.model.IndustryMode

@Composable
fun SettingsScreen(
    settings: BusinessSettings,
    activeIndustry: IndustryMode,
    onSaveSettings: (BusinessSettings) -> Unit,
    onIndustryChange: (IndustryMode) -> Unit,
    onResetCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var panVatNumber by remember(settings) { mutableStateOf(settings.panVatNumber) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var vatEnabled by remember(settings) { mutableStateOf(settings.vatEnabled) }
    var serviceChargeEnabled by remember(settings) { mutableStateOf(settings.serviceChargeEnabled) }
    var printerWidth by remember(settings) { mutableStateOf(settings.printerWidth) }
    var currencySymbol by remember(settings) { mutableStateOf(settings.currencySymbol) }

    var showHardwareGuideDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Industry Mode Switcher Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Industry Mode Switcher",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Adjusts POS workflows, catalog schemas, and ticket generators",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                IndustryModeOption(
                    mode = IndustryMode.RETAIL,
                    title = "Retail Shop & Grocery",
                    desc = "Standard inventory, barcode scanner, unit pricing (pcs/kg/pkt)",
                    icon = Icons.Default.Store,
                    isSelected = activeIndustry == IndustryMode.RETAIL,
                    onSelect = { onIndustryChange(IndustryMode.RETAIL) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                IndustryModeOption(
                    mode = IndustryMode.RESTAURANT,
                    title = "Restaurant & Cafe",
                    desc = "Table management (Tables 1-12), KOT tickets, 10% Service Charge",
                    icon = Icons.Default.Restaurant,
                    isSelected = activeIndustry == IndustryMode.RESTAURANT,
                    onSelect = { onIndustryChange(IndustryMode.RESTAURANT) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                IndustryModeOption(
                    mode = IndustryMode.TRANSPORT,
                    title = "Public Transport & Bus Service",
                    desc = "Route fares, bus ticket issuance, Student 45% statutory concession",
                    icon = Icons.Default.DirectionsBus,
                    isSelected = activeIndustry == IndustryMode.TRANSPORT,
                    onSelect = { onIndustryChange(IndustryMode.TRANSPORT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Business Information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Business Profile (Receipt Header)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Store / Company Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = panVatNumber,
                    onValueChange = { panVatNumber = it },
                    label = { Text("PAN / VAT Registration Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_pan_vat")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("Currency") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tax & Hardware Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tax & Thermal Printer Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // VAT Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Nepal Value Added Tax (VAT 13%)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Calculates 13% taxable VAT on checkout", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = vatEnabled,
                        onCheckedChange = { vatEnabled = it },
                        modifier = Modifier.testTag("switch_vat_enabled")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Service Charge Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Restaurant Service Charge (10%)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Applied to dine-in restaurant orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = serviceChargeEnabled,
                        onCheckedChange = { serviceChargeEnabled = it },
                        modifier = Modifier.testTag("switch_service_charge")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Thermal Printer Width Radio
                Text(text = "Default Thermal Paper Roll Width:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { printerWidth = "58mm" }
                    ) {
                        RadioButton(selected = printerWidth == "58mm", onClick = { printerWidth = "58mm" })
                        Text("58mm (Compact Mobile Printer)")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { printerWidth = "80mm" }
                    ) {
                        RadioButton(selected = printerWidth == "80mm", onClick = { printerWidth = "80mm" })
                        Text("80mm (Desktop POS)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Settings Action
        Button(
            onClick = {
                val updated = settings.copy(
                    businessName = businessName.trim(),
                    panVatNumber = panVatNumber.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    vatEnabled = vatEnabled,
                    serviceChargeEnabled = serviceChargeEnabled,
                    printerWidth = printerWidth,
                    currencySymbol = currencySymbol.trim(),
                    activeIndustry = activeIndustry
                )
                onSaveSettings(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Business Settings", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Integration Architecture Guide Button
        OutlinedButton(
            onClick = { showHardwareGuideDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("hardware_guide_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.IntegrationInstructions, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hardware & Merchant API Guide")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reload Sample Data Button
        OutlinedButton(
            onClick = { showResetConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_catalog_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reload Sample Nepal Demo Catalog")
        }
    }

    if (showHardwareGuideDialog) {
        HardwareGuideDialog(onDismiss = { showHardwareGuideDialog = false })
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reload Demo Catalog?") },
            text = { Text("This will re-insert sample Nepali retail, restaurant, and transport items into SQLite database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetCatalog()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reload")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IndustryModeOption(
    mode: IndustryMode,
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable { onSelect() }
            .padding(12.dp)
            .testTag("mode_option_${mode.name.lowercase()}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = isSelected, onClick = onSelect)
        }
    }
}

@Composable
private fun HardwareGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hardware & API Setup Guide", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "1. Bluetooth Thermal ESC/POS Printers (Sunmi / 58mm / 80mm):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• In SajiloPOS, receipts format ESC/POS bytes (0x1B, 0x40 to init; 0x1D, 0x56 to cut paper).\n• To connect hardware: Pair Bluetooth printer in Android settings -> Use BluetoothSocket with SPP UUID '00001101-0000-1000-8000-00805F9B34FB' -> Write the raw bytes generated by SajiloPOS.\n• Alternatively, use Android PrintManager which is already integrated in this app!",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "2. Fonepay Merchant Network Integration:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Fonepay EMVCo QR: Call POST /api/merchant/generateQr with Merchant ID, Amount NPR, and Trace ID.\n• Webhook Callback: Fonepay posts { 'status': 205, 'responseCode': 'SUCCESS', 'prn': txnRef } to your server URL.\n• SajiloPOS's verification module polls this callback and updates inventory once status 205 is confirmed.",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "3. eSewa Epay v2 SDK:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• In Nepal, eSewa transactions require secret merchant signature (HMAC-SHA256).\n• Callback endpoint returns refId upon user PIN entry.",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
