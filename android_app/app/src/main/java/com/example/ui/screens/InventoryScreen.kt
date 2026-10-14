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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessSettings
import com.example.data.model.IndustryMode
import com.example.data.model.ProductItem
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.GhostButton
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PosCard
import com.example.ui.components.PosSearchField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SoftIconButton
import com.example.ui.components.SoftPill
import com.example.ui.components.StatTile
import com.example.ui.components.StatusPill
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LineSubtle
import com.example.ui.theme.BrandGreenSoft
import com.example.ui.theme.accentFor
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.util.Format
import com.example.ui.theme.PosSpace
import com.example.ui.theme.PosType

@Composable
fun InventoryScreen(
    products: List<ProductItem>,
    lowStockProducts: List<ProductItem>,
    settings: BusinessSettings,
    activeIndustry: IndustryMode,
    inventoryValue: Double,
    onSaveProduct: (ProductItem) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onAdjustStock: (Long, Int) -> Unit,
    onScanBarcode: () -> Unit = {},
    intakeBarcode: String? = null,
    onIntakeConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var lowStockOnly by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ProductItem?>(null) }

    // Inventory mirrors the active business type, so a retail shop never sees
    // restaurant or transport items mixed into its stock list.
    val scopedProducts = remember(products, activeIndustry) {
        products.filter { it.industryMode == activeIndustry.name }
    }

    val categories = remember(scopedProducts) {
        listOf("All") + scopedProducts.map { it.category }.distinct()
    }

    val filteredList = remember(scopedProducts, searchQuery, lowStockOnly, selectedCategory) {
        scopedProducts.filter { product ->
            val matchesQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.nepaliName.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
            val matchesLow = !lowStockOnly || product.stockQuantity <= product.minStockThreshold
            matchesQuery && matchesCategory && matchesLow
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PosSpace.xl,
                end = PosSpace.xl,
                top = PosSpace.xl,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(PosSpace.lg)
        ) {
            item {
                SectionHeader(
                    title = "Inventory",
                    subtitle = "${scopedProducts.size} ${activeIndustry.title.lowercase()} products",
                    icon = Icons.Rounded.Inventory2
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                    StatTile(
                        label = "Stock value",
                        value = Format.money(inventoryValue, 0, settings.currencySymbol),
                        caption = "Valued at cost price",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Low stock",
                        value = lowStockProducts.size.toString(),
                        caption = if (lowStockProducts.isEmpty()) "Everything is healthy" else "Tap the alert to filter",
                        accent = if (lowStockProducts.isEmpty()) SuccessGreen else WarningOrange,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { lowStockOnly = !lowStockOnly }
                            .testTag("low_stock_stat_tile")
                    )
                }
            }

            if (lowStockProducts.isNotEmpty()) {
                item {
                    PosCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { lowStockOnly = !lowStockOnly }
                            .testTag("low_stock_alert_banner"),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(PosSpace.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lowStockProducts.size} products below threshold",
                                    style = PosType.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (lowStockOnly) "Showing low stock only · tap to show all" else "Tap to see only the items to reorder",
                                    style = PosType.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    PosSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search by name, barcode or Nepali name",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_field")
                    )
                    Spacer(Modifier.height(PosSpace.md))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PosSpace.xs)) {
                        items(categories) { category ->
                            SoftPill(
                                label = category,
                                selected = category == selectedCategory,
                                onClick = { selectedCategory = category },
                                testTag = "chip_inv_cat_$category"
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    PosCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.Inventory2,
                            title = "No products here",
                            message = "Adjust your filters, or add a new product with the + button below."
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { product ->
                    InventoryRow(
                        product = product,
                        currencySymbol = settings.currencySymbol,
                        onEdit = {
                            editingProduct = product
                            showEditor = true
                        },
                        onDelete = { pendingDelete = product },
                        onAdjustStock = { delta -> onAdjustStock(product.id, delta) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingProduct = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(PosSpace.xl)
                .testTag("add_inventory_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add product")
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            initialProduct = editingProduct,
            activeIndustry = activeIndustry,
            intakeBarcode = intakeBarcode,
            onScanBarcode = onScanBarcode,
            onIntakeConsumed = onIntakeConsumed,
            onDismiss = { showEditor = false },
            onSave = { product ->
                onSaveProduct(product)
                showEditor = false
            }
        )
    }

    pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${product.name}?", style = PosType.titleMedium) },
            text = {
                Text(
                    text = "This deletes the product from the catalog. Past sales and receipts are not affected.",
                    style = PosType.bodyMedium
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = "Remove",
                    onClick = {
                        onDeleteProduct(product.id)
                        pendingDelete = null
                    },
                    container = MaterialTheme.colorScheme.error,
                    height = 44.dp,
                    testTag = "confirm_delete_product"
                )
            },
            dismissButton = {
                GhostButton(
                    text = "Cancel",
                    onClick = { pendingDelete = null },
                    height = 44.dp
                )
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun InventoryRow(
    product: ProductItem,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    val isStockManaged = product.industryMode != "TRANSPORT"
    val isOut = isStockManaged && product.stockQuantity <= 0
    val isLow = isStockManaged && product.stockQuantity in 1..product.minStockThreshold
    val stockAccent = when {
        !isStockManaged -> MaterialTheme.colorScheme.primary
        isOut -> DangerRed
        isLow -> WarningOrange
        else -> SuccessGreen
    }

    PosCard(modifier = Modifier
        .fillMaxWidth()
        .testTag("inventory_item_${product.id}")) {
        Row(verticalAlignment = Alignment.Top) {
            AvatarBadge(
                label = product.name,
                accent = if (isOut) DangerRed else if (isLow) WarningOrange else accentFor(product.category).strong,
                size = 42.dp
            )
            Spacer(Modifier.width(PosSpace.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.category.uppercase(),
                    style = PosType.overline,
                    color = accentFor(product.category).strong,
                    maxLines = 1
                )
                Text(
                    text = product.name,
                    style = PosType.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.nepaliName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = product.nepaliName,
                        style = PosType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (product.barcode.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "#${product.barcode}",
                        style = PosType.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(PosSpace.sm))
            SoftIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "Edit ${product.name}",
                onClick = onEdit,
                size = 38.dp,
                testTag = "edit_inventory_${product.id}"
            )
            Spacer(Modifier.width(PosSpace.xs))
            SoftIconButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete ${product.name}",
                onClick = onDelete,
                tint = DangerRed,
                container = MaterialTheme.colorScheme.errorContainer,
                size = 38.dp,
                testTag = "delete_inventory_${product.id}"
            )
        }

        Spacer(Modifier.height(PosSpace.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = if (isStockManaged) "${product.stockQuantity} ${product.unit} in stock" else "Unlimited (tickets)",
                        accent = stockAccent
                    )
                    if (isLow && isStockManaged) {
                        Spacer(Modifier.width(PosSpace.xs))
                        Text(
                            text = "min ${product.minStockThreshold}",
                            style = PosType.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(PosSpace.xs))
                Text(
                    text = "${Format.money(product.price, 0, currencySymbol)} · cost ${Format.money(product.costPrice, 0, currencySymbol)}",
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(PosSpace.sm))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Format.money(product.price, 0, currencySymbol),
                    style = PosType.moneySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val margin = if (product.price > 0) {
                    (product.price - product.costPrice) / product.price * 100.0
                } else {
                    0.0
                }
                Text(
                    text = "${Format.percent(margin)} margin",
                    style = PosType.labelSmall,
                    color = if (margin >= 0) SuccessGreen else DangerRed
                )
            }
        }

        if (isStockManaged) {
            Spacer(Modifier.height(PosSpace.md))
            HairlineDivider()
            Spacer(Modifier.height(PosSpace.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick adjust",
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                listOf(-1, 1, 5, 10).forEach { delta ->
                    SoftPill(
                        label = if (delta > 0) "+$delta" else "$delta",
                        selected = delta > 0,
                        onClick = { onAdjustStock(delta) },
                        testTag = "stock_adjust_${product.id}_$delta"
                    )
                    Spacer(Modifier.width(PosSpace.xs))
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initialProduct: ProductItem?,
    activeIndustry: IndustryMode,
    intakeBarcode: String? = null,
    onScanBarcode: () -> Unit = {},
    onIntakeConsumed: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var nepaliName by remember { mutableStateOf(initialProduct?.nepaliName ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
    var price by remember { mutableStateOf(initialProduct?.price?.let { formatEditable(it) } ?: "") }
    var cost by remember { mutableStateOf(initialProduct?.costPrice?.let { formatEditable(it) } ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "20") }
    var minStock by remember { mutableStateOf(initialProduct?.minStockThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }

    val priceValue = price.toDoubleOrNull()
    val canSave = name.isNotBlank() && priceValue != null && priceValue > 0.0

    // A barcode captured by the scanner lands straight in the SKU field.
    LaunchedEffect(intakeBarcode) {
        if (intakeBarcode != null) {
            barcode = intakeBarcode
            onIntakeConsumed()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialProduct == null) "New product" else "Edit product",
                style = PosType.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                EditorField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Product name",
                    modifier = Modifier.testTag("input_product_name")
                )
                EditorField(
                    value = nepaliName,
                    onValueChange = { nepaliName = it },
                    label = "Nepali name (वाइ वाइ)"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                    EditorField(
                        value = category,
                        onValueChange = { category = it },
                        label = "Category",
                        modifier = Modifier.weight(1f)
                    )
                    EditorField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = "Unit",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                    EditorField(
                        value = price,
                        onValueChange = { price = it.filterNumeric() },
                        label = "Selling price",
                        numeric = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_price")
                    )
                    EditorField(
                        value = cost,
                        onValueChange = { cost = it.filterNumeric() },
                        label = "Cost price",
                        numeric = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PosSpace.sm)) {
                    EditorField(
                        value = stock,
                        onValueChange = { stock = it.filterNumeric() },
                        label = "Quantity",
                        numeric = true,
                        modifier = Modifier.weight(1f)
                    )
                    EditorField(
                        value = minStock,
                        onValueChange = { minStock = it.filterNumeric() },
                        label = "Low stock at",
                        numeric = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                EditorField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = "Barcode / SKU",
                    numeric = true,
                    trailingIcon = {
                        IconButton(
                            onClick = onScanBarcode,
                            modifier = Modifier.testTag("scan_barcode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Scan barcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save product",
                onClick = {
                    onSave(
                        ProductItem(
                            id = initialProduct?.id ?: 0L,
                            name = name.trim(),
                            nepaliName = nepaliName.trim(),
                            barcode = barcode.trim().ifBlank { "SKU-${System.currentTimeMillis() % 100000}" },
                            category = category.trim().ifBlank { "General" },
                            price = priceValue ?: 0.0,
                            costPrice = cost.toDoubleOrNull() ?: 0.0,
                            stockQuantity = stock.toIntOrNull() ?: 0,
                            minStockThreshold = minStock.toIntOrNull() ?: 5,
                            unit = unit.trim().ifBlank { "pcs" },
                            industryMode = initialProduct?.industryMode ?: activeIndustry.name
                        )
                    )
                },
                enabled = canSave,
                height = 48.dp,
                modifier = Modifier.testTag("save_product_button")
            )
        },
        dismissButton = {
            GhostButton(text = "Cancel", onClick = onDismiss, height = 48.dp)
        }
    )
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = PosType.bodySmall) },
        trailingIcon = trailingIcon,
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

private fun String.filterNumeric(): String = filter { it.isDigit() || it == '.' }

private fun formatEditable(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()