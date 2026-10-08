package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessSettings
import com.example.data.model.CartItem
import com.example.data.model.IndustryMode
import com.example.data.model.ProductItem
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.data.model.TransportRoute
import com.example.ui.theme.PosSlate900
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockLowOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    activeIndustry: IndustryMode,
    products: List<ProductItem>,
    cartItems: List<CartItem>,
    settings: BusinessSettings,
    searchQuery: String,
    selectedCategory: String,
    restaurantTables: List<RestaurantTable>,
    selectedTable: RestaurantTable?,
    transportRoutes: List<TransportRoute>,
    selectedRoute: TransportRoute,
    passengerType: String,
    cartSubtotal: Double,
    serviceChargeAmount: Double,
    vatAmount: Double,
    grandTotal: Double,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onIncrementCart: (CartItem) -> Unit,
    onDecrementCart: (CartItem) -> Unit,
    onRemoveCart: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onOpenScanner: () -> Unit,
    onSelectTable: (RestaurantTable) -> Unit,
    onSelectRoute: (TransportRoute) -> Unit,
    onPassengerTypeChange: (String) -> Unit,
    onIssueTransportTicket: (String) -> Unit,
    onOpenCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMobileCartSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 680.dp

        if (isTabletLandscape) {
            // Dual-Pane Tablet Layout: Left 60% Catalog, Right 40% Cart & Quick Pay
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                ) {
                    CatalogSection(
                        activeIndustry = activeIndustry,
                        products = products,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        restaurantTables = restaurantTables,
                        selectedTable = selectedTable,
                        transportRoutes = transportRoutes,
                        selectedRoute = selectedRoute,
                        passengerType = passengerType,
                        currencySymbol = settings.currencySymbol,
                        onSearchChange = onSearchChange,
                        onCategoryChange = onCategoryChange,
                        onAddToCart = onAddToCart,
                        onOpenScanner = onOpenScanner,
                        onSelectTable = onSelectTable,
                        onSelectRoute = onSelectRoute,
                        onPassengerTypeChange = onPassengerTypeChange,
                        onIssueTransportTicket = onIssueTransportTicket
                    )
                }

                // Tablet Right Pane: Dedicated Live Cart
                Surface(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    CartPanel(
                        cartItems = cartItems,
                        settings = settings,
                        activeIndustry = activeIndustry,
                        selectedTable = selectedTable,
                        selectedRoute = selectedRoute,
                        cartSubtotal = cartSubtotal,
                        serviceChargeAmount = serviceChargeAmount,
                        vatAmount = vatAmount,
                        grandTotal = grandTotal,
                        onIncrement = onIncrementCart,
                        onDecrement = onDecrementCart,
                        onRemove = onRemoveCart,
                        onClear = onClearCart,
                        onCheckout = onOpenCheckout
                    )
                }
            }
        } else {
            // Mobile Portrait Layout: Full Catalog + Floating Bottom Cart Bar
            Box(modifier = Modifier.fillMaxSize()) {
                CatalogSection(
                    activeIndustry = activeIndustry,
                    products = products,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    restaurantTables = restaurantTables,
                    selectedTable = selectedTable,
                    transportRoutes = transportRoutes,
                    selectedRoute = selectedRoute,
                    passengerType = passengerType,
                    currencySymbol = settings.currencySymbol,
                    onSearchChange = onSearchChange,
                    onCategoryChange = onCategoryChange,
                    onAddToCart = onAddToCart,
                    onOpenScanner = onOpenScanner,
                    onSelectTable = onSelectTable,
                    onSelectRoute = onSelectRoute,
                    onPassengerTypeChange = onPassengerTypeChange,
                    onIssueTransportTicket = onIssueTransportTicket,
                    modifier = Modifier.padding(bottom = if (cartItems.isNotEmpty()) 74.dp else 0.dp)
                )

                // Mobile Bottom Floating Cart Bar
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showMobileCartSheet = true }
                            .testTag("floating_cart_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                            Text(
                                                text = cartItems.sumOf { it.quantity }.toString(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${cartItems.size} items in cart",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Tap to review & checkout",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${settings.currencySymbol} ${"%.2f".format(grandTotal)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = onOpenCheckout,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("quick_pay_button")
                                ) {
                                    Text("Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Mobile Bottom Sheet for Cart
            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    sheetState = sheetState
                ) {
                    CartPanel(
                        cartItems = cartItems,
                        settings = settings,
                        activeIndustry = activeIndustry,
                        selectedTable = selectedTable,
                        selectedRoute = selectedRoute,
                        cartSubtotal = cartSubtotal,
                        serviceChargeAmount = serviceChargeAmount,
                        vatAmount = vatAmount,
                        grandTotal = grandTotal,
                        onIncrement = onIncrementCart,
                        onDecrement = onDecrementCart,
                        onRemove = onRemoveCart,
                        onClear = onClearCart,
                        onCheckout = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showMobileCartSheet = false
                                onOpenCheckout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSection(
    activeIndustry: IndustryMode,
    products: List<ProductItem>,
    searchQuery: String,
    selectedCategory: String,
    restaurantTables: List<RestaurantTable>,
    selectedTable: RestaurantTable?,
    transportRoutes: List<TransportRoute>,
    selectedRoute: TransportRoute,
    passengerType: String,
    currencySymbol: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onOpenScanner: () -> Unit,
    onSelectTable: (RestaurantTable) -> Unit,
    onSelectRoute: (TransportRoute) -> Unit,
    onPassengerTypeChange: (String) -> Unit,
    onIssueTransportTicket: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search & Scanner Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        when (activeIndustry) {
                            IndustryMode.RETAIL -> "Search products, Wai Wai, milk, barcode..."
                            IndustryMode.RESTAURANT -> "Search momo, chowmein, khaja, drinks..."
                            IndustryMode.TRANSPORT -> "Search routes, destinations..."
                        },
                        fontSize = 13.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )

            if (activeIndustry == IndustryMode.RETAIL) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("open_barcode_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-Industry Context Banners
        when (activeIndustry) {
            IndustryMode.RESTAURANT -> {
                RestaurantTableSelectorBar(
                    tables = restaurantTables,
                    selectedTable = selectedTable,
                    onSelectTable = onSelectTable
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            IndustryMode.TRANSPORT -> {
                TransportRouteControlBar(
                    routes = transportRoutes,
                    selectedRoute = selectedRoute,
                    passengerType = passengerType,
                    onSelectRoute = onSelectRoute,
                    onPassengerTypeChange = onPassengerTypeChange,
                    onIssueTicket = onIssueTransportTicket
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            IndustryMode.RETAIL -> {
                // Category Filter Chips
                val categories = remember(products) {
                    listOf("All") + products.map { it.category }.distinct()
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat, fontSize = 12.sp) },
                            modifier = Modifier.testTag("chip_cat_$cat")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Product Grid
        val filteredProducts = remember(products, selectedCategory) {
            if (selectedCategory == "All") products else products.filter { it.category == selectedCategory }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("product_grid")
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        currencySymbol = currencySymbol,
                        onAdd = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantTableSelectorBar(
    tables: List<RestaurantTable>,
    selectedTable: RestaurantTable?,
    onSelectTable: (RestaurantTable) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableBar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Table Service Management",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (selectedTable != null) {
                    Text(
                        text = "Active: ${selectedTable.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tables) { table ->
                    val isSelected = selectedTable?.id == table.id
                    val statusColor = when (table.status) {
                        TableStatus.AVAILABLE -> Color(0xFF2E7D32)
                        TableStatus.OCCUPIED -> Color(0xFFED6C02)
                        TableStatus.BILLING -> Color(0xFF1976D2)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else statusColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectTable(table) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("table_chip_${table.id}")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = table.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = when (table.status) {
                                    TableStatus.AVAILABLE -> "Available (${table.capacity}p)"
                                    TableStatus.OCCUPIED -> "Occupied • ${table.activeItemCount} items"
                                    TableStatus.BILLING -> "Billing • NPR ${table.currentTabTotal.toInt()}"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportRouteControlBar(
    routes: List<TransportRoute>,
    selectedRoute: TransportRoute,
    passengerType: String,
    onSelectRoute: (TransportRoute) -> Unit,
    onPassengerTypeChange: (String) -> Unit,
    onIssueTicket: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transport Route & Bus Ticketing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = selectedRoute.busNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(routes) { route ->
                    FilterChip(
                        selected = route.id == selectedRoute.id,
                        onClick = { onSelectRoute(route) },
                        label = { Text(route.routeName, fontSize = 11.sp) },
                        modifier = Modifier.testTag("route_chip_${route.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Statutory concession selector (Nepal Transport rule)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Fare Concession:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                listOf("Regular", "Student", "Senior").forEach { type ->
                    val isSel = passengerType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onPassengerTypeChange(type) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("concession_$type")
                    ) {
                        Text(
                            text = when (type) {
                                "Student" -> "Student (-45%)"
                                "Senior" -> "Senior (-50%)"
                                else -> "Regular"
                            },
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Issue Ticket to Intermediate Stop
            Text(text = "Quick Issue Ticket to Stop:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selectedRoute.intermediateStops) { stop ->
                    OutlinedButton(
                        onClick = { onIssueTicket(stop) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_ticket_stop_$stop")
                    ) {
                        Text(text = "+ $stop", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductItem,
    currencySymbol: String,
    onAdd: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStockThreshold && product.industryMode != "TRANSPORT"
    val isOutOfStock = product.stockQuantity == 0 && product.industryMode != "TRANSPORT"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isOutOfStock) { onAdd() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Category & Low Stock Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )

                if (isLowStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOutOfStock) StockCriticalRed else StockLowOrange)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOutOfStock) "OUT OF STOCK" else "LOW: ${product.stockQuantity}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (product.nepaliName.isNotBlank()) {
                Text(
                    text = product.nepaliName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Add button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$currencySymbol ${product.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/${product.unit}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAdd,
                    enabled = !isOutOfStock,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (!isOutOfStock) MaterialTheme.colorScheme.primary else Color.Gray)
                        .testTag("add_product_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    cartItems: List<CartItem>,
    settings: BusinessSettings,
    activeIndustry: IndustryMode,
    selectedTable: RestaurantTable?,
    selectedRoute: TransportRoute,
    cartSubtotal: Double,
    serviceChargeAmount: Double,
    vatAmount: Double,
    grandTotal: Double,
    onIncrement: (CartItem) -> Unit,
    onDecrement: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClear: () -> Unit = {},
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Current Order Cart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (activeIndustry) {
                            IndustryMode.RETAIL -> "Counter Sale"
                            IndustryMode.RESTAURANT -> selectedTable?.name ?: "Dine-In / Takeaway"
                            IndustryMode.TRANSPORT -> "Bus ${selectedRoute.busNumber}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (cartItems.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.testTag("clear_cart_button")) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color(0xFFEF4444))
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        // Cart Line Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cart is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap catalog items to add",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onIncrement = { onIncrement(item) },
                        onDecrement = { onDecrement(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        // Calculations Breakdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Subtotal:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${settings.currencySymbol} ${"%.2f".format(cartSubtotal)}", fontSize = 13.sp)
            }

            if (serviceChargeAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Service Charge (10%):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${settings.currencySymbol} ${"%.2f".format(serviceChargeAmount)}", fontSize = 13.sp)
                }
            }

            if (vatAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Taxable VAT (13%):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${settings.currencySymbol} ${"%.2f".format(vatAmount)}", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grand Total Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grand Total:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${settings.currencySymbol} ${"%.2f".format(grandTotal)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Checkout Button
            Button(
                onClick = onCheckout,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Proceed to Checkout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    currencySymbol: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.passengerType != "Regular") {
                    Text(
                        text = "Concession: ${item.passengerType}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "$currencySymbol ${"%.2f".format(item.unitPriceAfterDiscount)} × ${item.quantity} = $currencySymbol ${"%.2f".format(item.itemTotal)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity Control Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("decrement_${item.product.id}")
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp),
                        tint = if (item.quantity == 1) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("increment_${item.product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
