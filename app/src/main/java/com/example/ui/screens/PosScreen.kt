package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessSettings
import com.example.data.model.CartItem
import com.example.data.model.IndustryMode
import com.example.data.model.ProductItem
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.data.model.TransportRoute
import com.example.ui.components.HairlineDivider
import com.example.ui.components.Overline
import com.example.ui.components.PosCard
import com.example.ui.components.PosSearchField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SoftIconButton
import com.example.ui.components.SoftPill
import com.example.ui.components.StatusPill
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LineSubtle
import com.example.ui.theme.PosSpace as Spacing
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.util.Format
import com.example.ui.theme.PosType
import kotlinx.coroutines.launch

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
            // Dual pane: generous catalog on the left, dedicated cart on the right.
            Row(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                )

                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(LineSubtle)
                        )
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
            }
        } else {
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
                    modifier = Modifier.padding(bottom = if (cartItems.isNotEmpty()) 92.dp else 0.dp)
                )

                if (cartItems.isNotEmpty()) {
                    FloatingCartBar(
                        itemCount = cartItems.sumOf { it.quantity },
                        lineCount = cartItems.size,
                        grandTotal = grandTotal,
                        currencySymbol = settings.currencySymbol,
                        onReview = { showMobileCartSheet = true },
                        onPay = onOpenCheckout,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.md)
                    )
                }
            }

            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = null
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
                            .fillMaxHeight(0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingCartBar(
    itemCount: Int,
    lineCount: Int,
    grandTotal: Double,
    currencySymbol: String,
    onReview: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_cart_bar"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .clickable { onReview() }
                .padding(
                    start = Spacing.lg,
                    end = Spacing.sm,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ShoppingBag,
                    contentDescription = "Cart",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$itemCount ${if (itemCount == 1) "item" else "items"} · $lineCount lines",
                    style = PosType.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = Format.money(grandTotal, 2, currencySymbol),
                    style = PosType.moneyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            PrimaryButton(
                text = "Pay",
                icon = Icons.Rounded.Payments,
                onClick = onPay,
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary,
                height = 46.dp,
                modifier = Modifier.testTag("quick_pay_button")
            )
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
            .padding(
                start = Spacing.xl,
                end = Spacing.xl,
                top = Spacing.xl,
                bottom = Spacing.xl
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = when (activeIndustry) {
                    IndustryMode.RETAIL -> "Search products or scan a barcode"
                    IndustryMode.RESTAURANT -> "Search the menu — momo, chowmein, drinks"
                    IndustryMode.TRANSPORT -> "Search routes and destinations"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )

            if (activeIndustry == IndustryMode.RETAIL) {
                Spacer(Modifier.width(Spacing.sm))
                SoftIconButton(
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan barcode",
                    onClick = onOpenScanner,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    container = MaterialTheme.colorScheme.primary,
                    size = 54.dp,
                    iconSize = 22.dp,
                    modifier = Modifier.testTag("open_barcode_scanner_button")
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        when (activeIndustry) {
            IndustryMode.RESTAURANT -> {
                RestaurantTableSelector(
                    tables = restaurantTables,
                    selectedTable = selectedTable,
                    onSelectTable = onSelectTable
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            IndustryMode.TRANSPORT -> {
                TransportRouteControl(
                    routes = transportRoutes,
                    selectedRoute = selectedRoute,
                    passengerType = passengerType,
                    onSelectRoute = onSelectRoute,
                    onPassengerTypeChange = onPassengerTypeChange,
                    onIssueTicket = onIssueTransportTicket
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            IndustryMode.RETAIL -> {
                val categories = remember(products) {
                    listOf("All") + products.map { it.category }.distinct()
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(categories) { category ->
                        SoftPill(
                            label = category,
                            selected = category == selectedCategory,
                            onClick = { onCategoryChange(category) },
                            testTag = "chip_cat_$category"
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }

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
                    text = if (searchQuery.isBlank()) "No items in this category yet" else "Nothing matched “$searchQuery”",
                    style = PosType.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = Spacing.xl),
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
private fun RestaurantTableSelector(
    tables: List<RestaurantTable>,
    selectedTable: RestaurantTable?,
    onSelectTable: (RestaurantTable) -> Unit
) {
    PosCard(contentPadding = PaddingValues(Spacing.lg)) {
        SectionHeader(
            title = "Table service",
            subtitle = selectedTable?.let { "Billing to ${it.name}" }
                ?: "Pick a table to start the tab",
            icon = Icons.Rounded.TableRestaurant
        )
        Spacer(Modifier.height(Spacing.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(tables) { table ->
                val selected = selectedTable?.id == table.id
                val statusColor = when (table.status) {
                    TableStatus.AVAILABLE -> SuccessGreen
                    TableStatus.OCCUPIED -> WarningOrange
                    TableStatus.BILLING -> MaterialTheme.colorScheme.primary
                }
                val statusLabel = when (table.status) {
                    TableStatus.AVAILABLE -> "Free · ${table.capacity}p"
                    TableStatus.OCCUPIED -> "${table.activeItemCount} items"
                    TableStatus.BILLING -> Format.money(table.currentTabTotal, 0)
                }

                PosCard(
                    modifier = Modifier
                        .width(148.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTable(table) }
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("table_chip_${table.id}"),
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentPadding = PaddingValues(Spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = table.name,
                            style = PosType.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = statusLabel,
                        style = PosType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportRouteControl(
    routes: List<TransportRoute>,
    selectedRoute: TransportRoute,
    passengerType: String,
    onSelectRoute: (TransportRoute) -> Unit,
    onPassengerTypeChange: (String) -> Unit,
    onIssueTicket: (String) -> Unit
) {
    PosCard(contentPadding = PaddingValues(Spacing.lg)) {
        SectionHeader(
            title = "Route & ticketing",
            subtitle = "${selectedRoute.fromStop} → ${selectedRoute.toStop}",
            icon = Icons.Rounded.DirectionsBus
        )

        Spacer(Modifier.height(Spacing.md))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(routes) { route ->
                SoftPill(
                    label = route.routeName,
                    selected = route.id == selectedRoute.id,
                    onClick = { onSelectRoute(route) },
                    testTag = "route_chip_${route.id}"
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Overline("Passenger concession")
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf("Regular", "Student", "Senior").forEach { type ->
                SoftPill(
                    label = when (type) {
                        "Student" -> "Student −45%"
                        "Senior" -> "Senior −50%"
                        else -> "Regular"
                    },
                    selected = passengerType == type,
                    onClick = { onPassengerTypeChange(type) },
                    testTag = "concession_$type"
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Overline("Issue ticket to stop")
        Spacer(Modifier.height(Spacing.xs))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(selectedRoute.intermediateStops) { stop ->
                SoftPill(
                    label = stop,
                    selected = false,
                    onClick = { onIssueTicket(stop) },
                    leadingIcon = Icons.Rounded.Add,
                    testTag = "quick_ticket_stop_$stop"
                )
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
    val isStockManaged = product.industryMode != "TRANSPORT"
    val isOutOfStock = isStockManaged && product.stockQuantity <= 0
    val isLowStock = isStockManaged && product.stockQuantity in 1..product.minStockThreshold

    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product.category.uppercase(),
                style = PosType.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isOutOfStock) {
                StatusPill(text = "SOLD OUT", accent = DangerRed)
            } else if (isLowStock) {
                StatusPill(text = "${product.stockQuantity} left", accent = WarningOrange)
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            text = product.name,
            style = PosType.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(40.dp)
        )

        if (product.nepaliName.isNotBlank()) {
            Text(
                text = product.nepaliName,
                style = PosType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Format.money(product.price, 0, currencySymbol),
                    style = PosType.moneySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "per ${product.unit}",
                    style = PosType.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Spacing.xs))

            SoftIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add ${product.name}",
                onClick = onAdd,
                tint = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                container = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                size = 42.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag("add_product_${product.id}")
            )
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
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = settings.currencySymbol

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = Spacing.xl,
                end = Spacing.xl,
                top = Spacing.xl,
                bottom = Spacing.xl
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current order",
                    style = PosType.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (activeIndustry) {
                        IndustryMode.RETAIL -> "Counter sale"
                        IndustryMode.RESTAURANT -> selectedTable?.name ?: "Dine-in / takeaway"
                        IndustryMode.TRANSPORT -> "Bus ${selectedRoute.busNumber}"
                    },
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (cartItems.isNotEmpty()) {
                SoftIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Clear cart",
                    onClick = onClear,
                    tint = DangerRed,
                    container = MaterialTheme.colorScheme.errorContainer,
                    size = 40.dp,
                    testTag = "clear_cart_button"
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Cart is empty",
                    style = PosType.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = "Tap a product or scan a barcode to begin",
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        currencySymbol = symbol,
                        onIncrement = { onIncrement(item) },
                        onDecrement = { onDecrement(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        HairlineDivider()
        Spacer(Modifier.height(Spacing.lg))

        SummaryRow(label = "Subtotal", value = Format.money(cartSubtotal, 2, symbol))
        if (serviceChargeAmount > 0) {
            Spacer(Modifier.height(Spacing.xs))
            SummaryRow(
                label = "Service charge (${Format.percent(settings.serviceChargePercent)})",
                value = Format.money(serviceChargeAmount, 2, symbol)
            )
        }
        if (vatAmount > 0) {
            Spacer(Modifier.height(Spacing.xs))
            SummaryRow(
                label = "VAT (${Format.percent(settings.vatRatePercent)})",
                value = Format.money(vatAmount, 2, symbol)
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Total payable",
                style = PosType.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Format.money(grandTotal, 2, symbol),
                style = PosType.moneyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        PrimaryButton(
            text = "Proceed to checkout",
            icon = Icons.Rounded.Payments,
            onClick = onCheckout,
            enabled = cartItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_button")
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = PosType.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = PosType.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = PosType.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.passengerType != "Regular") {
                Spacer(Modifier.height(4.dp))
                StatusPill(text = "${item.passengerType} concession", accent = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${Format.money(item.unitPriceAfterDiscount, 0, currencySymbol)} × ${item.quantity}",
                style = PosType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(Spacing.xs))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(
                icon = if (item.quantity == 1) Icons.Rounded.DeleteOutline else Icons.Rounded.Remove,
                contentDescription = if (item.quantity == 1) "Remove item" else "Decrease quantity",
                onClick = onDecrement,
                tint = if (item.quantity == 1) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                container = MaterialTheme.colorScheme.surfaceVariant,
                size = 34.dp,
                iconSize = 16.dp,
                testTag = "decrement_${item.product.id}"
            )
            Text(
                text = item.quantity.toString(),
                style = PosType.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(36.dp)
                    .padding(vertical = 2.dp)
            )
            SoftIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase quantity",
                onClick = onIncrement,
                tint = MaterialTheme.colorScheme.onPrimary,
                container = MaterialTheme.colorScheme.primary,
                size = 34.dp,
                iconSize = 16.dp,
                testTag = "increment_${item.product.id}"
            )
        }

        Spacer(Modifier.width(Spacing.sm))

        Text(
            text = Format.money(item.itemTotal, 0, currencySymbol),
            style = PosType.moneyTiny,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(74.dp),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}