package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IndustryMode
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.PaymentModal
import com.example.ui.components.ThermalReceiptModal
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PosGradients
import com.example.ui.theme.TintRose
import com.example.ui.theme.PosSpace as Spacing
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.theme.PosType

enum class PosTab(val title: String, val shortTitle: String, val icon: ImageVector) {
    SALE("Point of Sale", "Sell", Icons.Rounded.PointOfSale),
    INSIGHTS("Insights", "Insights", Icons.Rounded.AutoGraph),
    INVENTORY("Inventory", "Stock", Icons.Rounded.Inventory2),
    TRANSACTIONS("Sales History", "History", Icons.Rounded.ReceiptLong),
    SETTINGS("Settings", "Settings", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PosViewModel) {
    var selectedTab by remember { mutableStateOf(PosTab.SALE) }

    val activeIndustry by viewModel.activeIndustry.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val inventoryValue by viewModel.inventoryValue.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val restaurantTables by viewModel.restaurantTables.collectAsStateWithLifecycle()
    val selectedTable by viewModel.selectedTable.collectAsStateWithLifecycle()
    val transportRoutes = viewModel.transportRoutes
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val passengerType by viewModel.passengerType.collectAsStateWithLifecycle()

    val showPaymentDialog by viewModel.showPaymentDialog.collectAsStateWithLifecycle()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val cashTendered by viewModel.cashTendered.collectAsStateWithLifecycle()
    val paymentVerificationState by viewModel.paymentVerificationState.collectAsStateWithLifecycle()

    val showReceiptModal by viewModel.showReceiptModal.collectAsStateWithLifecycle()
    val completedSale by viewModel.completedSale.collectAsStateWithLifecycle()

    val showBarcodeScanner by viewModel.showBarcodeScanner.collectAsStateWithLifecycle()
    val scannerFeedback by viewModel.scannerScanFeedback.collectAsStateWithLifecycle()
    val intakeBarcode by viewModel.intakeBarcode.collectAsStateWithLifecycle()

    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val insightRange by viewModel.insightRange.collectAsStateWithLifecycle()
    val restockSuggestions by viewModel.restockSuggestions.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletExpanded = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTabletExpanded) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .testTag("pos_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        BrandMark(modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xl))
                    }
                ) {
                    Spacer(Modifier.height(Spacing.xs))
                    PosTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                NavIcon(
                                    tab = tab,
                                    selected = isSelected,
                                    lowStockCount = lowStockProducts.size
                                )
                            },
                            label = {
                                Text(
                                    text = tab.shortTitle,
                                    style = PosType.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier
                                .padding(vertical = Spacing.xxs)
                                .testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(Spacing.md)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
                // Top and bottom bars own their own insets, so the body must not add them twice.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    AppTopBar(
                        activeTab = selectedTab,
                        businessName = settings.businessName,
                        lowStockCount = lowStockProducts.size,
                        onOpenInventory = { selectedTab = PosTab.INVENTORY }
                    )
                },
                bottomBar = {
                    if (!isTabletExpanded) {
                        NavigationBar(
                            modifier = Modifier.testTag("mobile_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            PosTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        NavIcon(
                                            tab = tab,
                                            selected = selectedTab == tab,
                                            lowStockCount = lowStockProducts.size
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.shortTitle,
                                            style = PosType.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        PosTab.SALE -> {
                            PosScreen(
                                activeIndustry = activeIndustry,
                                products = products,
                                cartItems = cartItems,
                                settings = settings,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                restaurantTables = restaurantTables,
                                selectedTable = selectedTable,
                                transportRoutes = transportRoutes,
                                selectedRoute = selectedRoute,
                                passengerType = passengerType,
                                cartSubtotal = viewModel.cartSubtotal,
                                serviceChargeAmount = viewModel.serviceChargeAmount,
                                vatAmount = viewModel.vatAmount,
                                grandTotal = viewModel.grandTotal,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onCategoryChange = { viewModel.setSelectedCategory(it) },
                                onAddToCart = { viewModel.addToCart(it) },
                                onIncrementCart = { viewModel.incrementCartItem(it) },
                                onDecrementCart = { viewModel.decrementCartItem(it) },
                                onRemoveCart = { viewModel.removeCartItem(it) },
                                onClearCart = { viewModel.clearCart() },
                                onOpenScanner = { viewModel.openBarcodeScanner() },
                                onSelectTable = { viewModel.selectTable(it) },
                                onSelectRoute = { viewModel.selectRoute(it) },
                                onPassengerTypeChange = { viewModel.setPassengerType(it) },
                                onIssueTransportTicket = { viewModel.issueQuickTicket(it) },
                                onOpenCheckout = { viewModel.openPaymentDialog() },
                                onOpenInsights = { selectedTab = PosTab.INSIGHTS },
                                todayRevenue = insights.todayRevenue,
                                todayOrders = insights.series.lastOrNull()?.orders ?: 0
                            )
                        }

                        PosTab.INSIGHTS -> {
                            InsightsScreen(
                                snapshot = insights,
                                selectedRange = insightRange,
                                restockSuggestions = restockSuggestions,
                                inventoryValue = inventoryValue,
                                catalogCount = allProducts.count { it.industryMode == activeIndustry.name },
                                settings = settings,
                                onRangeChange = { viewModel.setInsightRange(it) },
                                onRestock = { viewModel.applyRestockSuggestion(it) },
                                onOpenInventory = { selectedTab = PosTab.INVENTORY }
                            )
                        }

                        PosTab.INVENTORY -> {
                            InventoryScreen(
                                products = allProducts,
                                lowStockProducts = lowStockProducts,
                                settings = settings,
                                activeIndustry = activeIndustry,
                                inventoryValue = inventoryValue,
                                onSaveProduct = { viewModel.saveProduct(it) },
                                onDeleteProduct = { viewModel.deleteProduct(it) },
                                onAdjustStock = { id, delta -> viewModel.adjustStock(id, delta) },
                                onScanBarcode = { viewModel.openIntakeScanner() },
                                intakeBarcode = intakeBarcode,
                                onIntakeConsumed = { viewModel.clearIntakeBarcode() }
                            )
                        }

                        PosTab.TRANSACTIONS -> {
                            TransactionsScreen(
                                transactions = transactions,
                                settings = settings,
                                onViewReceipt = { viewModel.openReprintReceipt(it) }
                            )
                        }

                        PosTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                activeIndustry = activeIndustry,
                                onSaveSettings = { viewModel.updateBusinessSettings(it) },
                                onResetCatalog = { viewModel.resetCatalogToDefaults() }
                            )
                        }
                    }
                }
            }
        }

        if (showBarcodeScanner) {
            BarcodeScannerModal(
                onDismiss = { viewModel.closeBarcodeScanner() },
                onBarcodeScanned = { code -> viewModel.scanBarcode(code) },
                feedbackMessage = scannerFeedback
            )
        }

        if (showPaymentDialog) {
            PaymentModal(
                grandTotal = viewModel.grandTotal,
                subtotal = viewModel.cartSubtotal,
                vatAmount = viewModel.vatAmount,
                serviceChargeAmount = viewModel.serviceChargeAmount,
                settings = settings,
                selectedMethod = selectedPaymentMethod,
                cashTendered = cashTendered,
                verificationState = paymentVerificationState,
                onSelectMethod = { viewModel.selectPaymentMethod(it) },
                onCashTenderedChange = { viewModel.setCashTendered(it) },
                onVerifyDigitalPayment = { viewModel.verifyDigitalPayment() },
                onCompleteCashSale = { viewModel.completeCashSale() },
                onDismiss = { viewModel.closePaymentDialog() }
            )
        }

        if (showReceiptModal && completedSale != null) {
            ThermalReceiptModal(
                sale = completedSale!!,
                settings = settings,
                onDismiss = { viewModel.closeReceiptModal() },
                onNewSale = { viewModel.closeReceiptModal() }
            )
        }
    }
}

@Composable
private fun NavIcon(tab: PosTab, selected: Boolean, lowStockCount: Int) {
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) PosGradients.brand else SolidColor(MaterialTheme.colorScheme.surface)
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
        contentAlignment = Alignment.Center
    ) {
        if (tab == PosTab.INVENTORY && lowStockCount > 0) {
            Box {
                Icon(tab.icon, contentDescription = tab.title, tint = tint)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        } else {
            Icon(tab.icon, contentDescription = tab.title, tint = tint)
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PosGradients.hero),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "स",
                style = PosType.titleLarge.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "SajiloPOS",
            style = PosType.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AppTopBar(
    activeTab: PosTab,
    businessName: String,
    lowStockCount: Int,
    onOpenInventory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact brand lockup for phones (tablet already shows the rail brand).
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeTab.title,
                    style = PosType.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = businessName,
                    style = PosType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (lowStockCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TintRose)
                        .clickable { onOpenInventory() }
                        .padding(horizontal = Spacing.sm, vertical = 7.dp)
                        .testTag("topbar_low_stock_chip")
                ) {
                    Text(
                        text = "$lowStockCount low",
                        style = PosType.labelMedium,
                        color = DangerRed,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(Spacing.xs))
            }

            // Retail-only app: static store badge, no business-type switching.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PosGradients.brand)
                    .padding(start = Spacing.sm, end = Spacing.sm, top = 8.dp, bottom = 8.dp)
                    .testTag("industry_mode_selector"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Store,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = IndustryMode.RETAIL.title,
                    style = PosType.labelMedium,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
