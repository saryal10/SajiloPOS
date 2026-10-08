package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessSettings
import com.example.data.model.IndustryMode
import com.example.data.model.ProductItem
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockLowOrange
import com.example.ui.theme.StockNormalGreen

@Composable
fun InventoryScreen(
    products: List<ProductItem>,
    lowStockProducts: List<ProductItem>,
    settings: BusinessSettings,
    activeIndustry: IndustryMode,
    onSaveProduct: (ProductItem) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onAdjustStock: (Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredList = remember(products, searchQuery, filterLowStockOnly, selectedCategory) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.nepaliName.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || p.category == selectedCategory
            val matchesLowStock = !filterLowStockOnly || (p.stockQuantity <= p.minStockThreshold)
            matchesQuery && matchesCat && matchesLowStock
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header & Low Stock Alert Banner
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filterLowStockOnly = !filterLowStockOnly }
                        .testTag("low_stock_alert_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filterLowStockOnly) Color(0xFF7F1D1D) else Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = if (filterLowStockOnly) Color.White else StockLowOrange
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert: ${lowStockProducts.size} items below threshold!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (filterLowStockOnly) Color.White else Color(0xFF92400E)
                            )
                            Text(
                                text = if (filterLowStockOnly) "Showing low stock items only (Tap to show all)" else "Tap to filter low stock items",
                                fontSize = 11.sp,
                                color = if (filterLowStockOnly) Color(0xFFFCA5A5) else Color(0xFFB45309)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search & Category Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, barcode, or Nepali name...") },
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
                    .testTag("inventory_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inventory List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No inventory items found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { product ->
                        InventoryItemCard(
                            product = product,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDelete = { onDeleteProduct(product.id) },
                            onAdjustStock = { delta -> onAdjustStock(product.id, delta) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Item
        FloatingActionButton(
            onClick = {
                editingProduct = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_inventory_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    if (showAddEditDialog) {
        AddEditProductDialog(
            initialProduct = editingProduct,
            activeIndustry = activeIndustry,
            onDismiss = { showAddEditDialog = false },
            onSave = { product ->
                onSaveProduct(product)
                showAddEditDialog = false
            }
        )
    }
}

@Composable
private fun InventoryItemCard(
    product: ProductItem,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    val isLow = product.stockQuantity <= product.minStockThreshold
    val isOut = product.stockQuantity == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (product.nepaliName.isNotBlank()) {
                        Text(
                            text = product.nepaliName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "Barcode: ${product.barcode}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Actions
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock & Price Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isOut -> StockCriticalRed
                                    isLow -> StockLowOrange
                                    else -> StockNormalGreen
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Stock: ${product.stockQuantity} ${product.unit}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isLow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Min: ${product.minStockThreshold}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Price Info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol ${product.price.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.costPrice > 0) {
                        Text(
                            text = "Cost: $currencySymbol ${product.costPrice.toInt()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stock Adjust Buttons (+1, +5, +10, -1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onAdjustStock(-1) },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("-1", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onAdjustStock(1) },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("+1", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onAdjustStock(5) },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("+5", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onAdjustStock(10) },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("+10 Restock", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AddEditProductDialog(
    initialProduct: ProductItem?,
    activeIndustry: IndustryMode,
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var nepaliName by remember { mutableStateOf(initialProduct?.nepaliName ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
    var priceStr by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var costStr by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "20") }
    var minStockStr by remember { mutableStateOf(initialProduct?.minStockThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("add_product_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (initialProduct == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product / Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nepaliName,
                    onValueChange = { nepaliName = it },
                    label = { Text("Nepali Name (e.g. वाइ वाइ / म:म)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/plate)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Selling Price (NPR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_price")
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost Price (NPR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Low Stock Alert At") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && priceStr.toDoubleOrNull() != null) {
                                val item = ProductItem(
                                    id = initialProduct?.id ?: 0L,
                                    name = name.trim(),
                                    nepaliName = nepaliName.trim(),
                                    barcode = if (barcode.isBlank()) "SKU-${System.currentTimeMillis() % 100000}" else barcode.trim(),
                                    category = if (category.isBlank()) "General" else category.trim(),
                                    price = priceStr.toDoubleOrNull() ?: 0.0,
                                    costPrice = costStr.toDoubleOrNull() ?: 0.0,
                                    stockQuantity = stockStr.toIntOrNull() ?: 10,
                                    minStockThreshold = minStockStr.toIntOrNull() ?: 5,
                                    unit = if (unit.isBlank()) "pcs" else unit.trim(),
                                    industryMode = activeIndustry.name
                                )
                                onSave(item)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}
