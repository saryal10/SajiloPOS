package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessSettings
import com.example.data.model.IndustryMode
import com.example.ui.components.GhostButton
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PosCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SoftPill
import com.example.ui.util.Format
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType

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

    var showGuide by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PosSpace.xl,
                end = PosSpace.xl,
                top = PosSpace.xl,
                bottom = PosSpace.huge
            ),
        verticalArrangement = Arrangement.spacedBy(PosSpace.lg)
    ) {
        SectionHeader(
            title = "Settings",
            subtitle = "Business profile, taxes and hardware",
            icon = Icons.Rounded.Business
        )

        // ---------------------------------------------------------------------
        // Industry mode
        // ---------------------------------------------------------------------
        PosCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "Business type",
                subtitle = "Switches the whole workflow: catalog, tables, tickets"
            )
            Spacer(Modifier.height(PosSpace.lg))
            IndustryOption(
                title = "Retail & grocery",
                description = "Barcode scanning, unit pricing, low stock alerts",
                icon = Icons.Rounded.Store,
                selected = activeIndustry == IndustryMode.RETAIL,
                onSelect = { onIndustryChange(IndustryMode.RETAIL) },
                testTag = "mode_option_retail"
            )
            Spacer(Modifier.height(PosSpace.sm))
            IndustryOption(
                title = "Restaurant & cafe",
                description = "Table service, kitchen items, service charge",
                icon = Icons.Rounded.Restaurant,
                selected = activeIndustry == IndustryMode.RESTAURANT,
                onSelect = { onIndustryChange(IndustryMode.RESTAURANT) },
                testTag = "mode_option_restaurant"
            )
            Spacer(Modifier.height(PosSpace.sm))
            IndustryOption(
                title = "Public transport",
                description = "Route fares, bus tickets, statutory concessions",
                icon = Icons.Rounded.DirectionsBus,
                selected = activeIndustry == IndustryMode.TRANSPORT,
                onSelect = { onIndustryChange(IndustryMode.TRANSPORT) },
                testTag = "mode_option_transport"
            )
        }

        // ---------------------------------------------------------------------
        // Business profile
        // ---------------------------------------------------------------------
        PosCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "Receipt header",
                subtitle = "Printed on every invoice and QR bill",
                icon = Icons.Rounded.AccountBalance
            )
            Spacer(Modifier.height(PosSpace.lg))
            SettingsField(
                value = businessName,
                onValueChange = { businessName = it },
                label = "Shop / company name",
                modifier = Modifier.testTag("settings_business_name")
            )
            Spacer(Modifier.height(PosSpace.sm))
            SettingsField(
                value = panVatNumber,
                onValueChange = { panVatNumber = it },
                label = "PAN / VAT number",
                numeric = true,
                modifier = Modifier.testTag("settings_pan_vat")
            )
            Spacer(Modifier.height(PosSpace.sm))
            SettingsField(
                value = address,
                onValueChange = { address = it },
                label = "Address"
            )
            Spacer(Modifier.height(PosSpace.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                SettingsField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    numeric = true,
                    modifier = Modifier.weight(1.5f)
                )
                SettingsField(
                    value = currencySymbol,
                    onValueChange = { currencySymbol = it },
                    label = "Currency",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---------------------------------------------------------------------
        // Taxes & hardware
        // ---------------------------------------------------------------------
        PosCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "Taxes & printing",
                subtitle = "VAT, service charge and paper roll",
                icon = Icons.Rounded.Print
            )

            Spacer(Modifier.height(PosSpace.lg))

            ToggleRow(
                title = "VAT ${Format.percent(settings.vatRatePercent)}",
                description = "Nepal taxable VAT added at checkout",
                checked = vatEnabled,
                onCheckedChange = { vatEnabled = it },
                testTag = "switch_vat_enabled"
            )

            Spacer(Modifier.height(PosSpace.lg))
            HairlineDivider()
            Spacer(Modifier.height(PosSpace.lg))

            ToggleRow(
                title = "Service charge ${Format.percent(settings.serviceChargePercent)}",
                description = "Applied to restaurant dine-in orders",
                checked = serviceChargeEnabled,
                onCheckedChange = { serviceChargeEnabled = it },
                testTag = "switch_service_charge"
            )

            Spacer(Modifier.height(PosSpace.lg))
            HairlineDivider()
            Spacer(Modifier.height(PosSpace.lg))

            Text(
                text = "Thermal paper roll",
                style = PosType.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(PosSpace.md))
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
                SoftPill(
                    label = "58 mm · pocket",
                    selected = printerWidth == "58mm",
                    onClick = { printerWidth = "58mm" },
                    testTag = "printer_width_58mm"
                )
                SoftPill(
                    label = "80 mm · desktop",
                    selected = printerWidth == "80mm",
                    onClick = { printerWidth = "80mm" },
                    testTag = "printer_width_80mm"
                )
            }
        }

        PrimaryButton(
            text = "Save settings",
            onClick = {
                onSaveSettings(
                    settings.copy(
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
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_settings_button")
        )

        GhostButton(
            text = "Hardware & payment API guide",
            icon = Icons.Rounded.HeadsetMic,
            onClick = { showGuide = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hardware_guide_button")
        )

        GhostButton(
            text = "Reload the Nepal demo catalog",
            icon = Icons.Rounded.Refresh,
            tint = MaterialTheme.colorScheme.error,
            onClick = { showResetConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_catalog_button")
        )

        Text(
            text = "SajiloPOS · offline-first POS for Nepal · v1.0",
            style = PosType.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PosSpace.sm),
            textAlign = TextAlign.Center
        )
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Hardware & API guide", style = PosType.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PosSpace.md)) {
                    GuideBlock(
                        icon = Icons.Rounded.Print,
                        title = "Bluetooth thermal printers",
                        body = "Pair any 58 mm or 80 mm ESC/POS printer (Sunmi, Xprinter, Rongta) in Android settings. SajiloPOS formats the receipt, and the print dialog can also send the job over Android PrintManager."
                    )
                    GuideBlock(
                        icon = Icons.Rounded.Bolt,
                        title = "Fonepay / eSewa / Khalti QR",
                        body = "A dynamic QR is generated for the exact amount. The verification step polls the gateway callback (Fonepay responds with status 205) before the sale is settled and stock is decremented."
                    )
                    GuideBlock(
                        icon = Icons.Rounded.AccountBalance,
                        title = "IRD compliance",
                        body = "Every receipt carries your PAN/VAT number, an invoice sequence number and a verification QR, keeping the bill audit-ready year round."
                    )
                    GuideBlock(
                        icon = Icons.Rounded.Refresh,
                        title = "Works without internet",
                        body = "Sales, stock movements and reports are stored in a local SQLite database, so load shedding never stops your counter."
                    )
                }
            },
            confirmButton = {
                PrimaryButton(text = "Got it", onClick = { showGuide = false }, height = 46.dp)
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reload demo catalog?", style = PosType.titleMedium) },
            text = {
                Text(
                    text = "This re-inserts the sample retail, restaurant and transport products into the local database.",
                    style = PosType.bodyMedium
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = "Reload",
                    onClick = {
                        onResetCatalog()
                        showResetConfirm = false
                    },
                    container = MaterialTheme.colorScheme.error,
                    height = 44.dp,
                    testTag = "confirm_reset_catalog"
                )
            },
            dismissButton = {
                GhostButton(text = "Cancel", onClick = { showResetConfirm = false }, height = 44.dp)
            }
        )
    }
}

@Composable
private fun IndustryOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onSelect() }
            .padding(PosSpace.md)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(PosSpace.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PosType.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = PosType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text(
                    text = "✓",
                    style = PosType.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PosType.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = PosType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(PosSpace.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = PosType.bodySmall) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = PosType.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun GuideBlock(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(PosSpace.sm))
        Column {
            Text(text = title, style = PosType.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(text = body, style = PosType.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
