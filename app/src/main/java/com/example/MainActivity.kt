package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IndustryMode
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.PaymentModal
import com.example.ui.components.ThermalReceiptModal
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PosAmberAccent
import com.example.ui.theme.PosSlate900
import com.example.ui.viewmodel.PosViewModel

enum class PosTab(val title: String, val icon: ImageVector) {
    SALE("Sale (POS)", Icons.Default.PointOfSale),
    INVENTORY("Inventory", Icons.Default.Inventory2),
    TRANSACTIONS("History", Icons.Default.ReceiptLong),
    SETTINGS("Settings", Icons.Default.Settings)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: PosViewModel) {
    var selectedTab by remember { mutableStateOf(PosTab.SALE) }
    var industryMenuExpanded by remember { mutableStateOf(false) }

    // Reactive State from ViewModel
    val activeIndustry by viewModel.activeIndustry.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
            // Navigation Rail on Expanded Tablet Terminals
            if (isTabletExpanded) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("pos_navigation_rail"),
                    containerColor = PosSlate900,
                    contentColor = Color.White,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "स",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SajiloPOS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                ) {
                    PosTab.values().forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (tab == PosTab.INVENTORY && lowStockProducts.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = Color(0xFFEF4444)) {
                                                Text(lowStockProducts.size.toString())
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            },
                            label = { Text(tab.title, fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // Main Content Area with Adaptive TopAppBar & Body
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SajiloPOS",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${settings.businessName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        },
                        actions = {
                            // Quick Industry Mode Dropdown Switcher in Top Bar
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { industryMenuExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("industry_mode_selector"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (activeIndustry) {
                                            IndustryMode.RETAIL -> Icons.Default.Store
                                            IndustryMode.RESTAURANT -> Icons.Default.Restaurant
                                            IndustryMode.TRANSPORT -> Icons.Default.DirectionsBus
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeIndustry.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Change",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = industryMenuExpanded,
                                    onDismissRequest = { industryMenuExpanded = false }
                                ) {
                                    IndustryMode.values().forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(mode.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                viewModel.setIndustryMode(mode)
                                                industryMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        IndustryMode.RETAIL -> Icons.Default.Store
                                                        IndustryMode.RESTAURANT -> Icons.Default.Restaurant
                                                        IndustryMode.TRANSPORT -> Icons.Default.DirectionsBus
                                                    },
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.testTag("menu_item_${mode.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            if (lowStockProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .clickable { selectedTab = PosTab.INVENTORY }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Low Stock",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${lowStockProducts.size} Low",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    // Mobile Bottom Navigation Bar
                    if (!isTabletExpanded) {
                        NavigationBar(
                            modifier = Modifier.testTag("mobile_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            PosTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        if (tab == PosTab.INVENTORY && lowStockProducts.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = Color(0xFFEF4444)) {
                                                        Text(lowStockProducts.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(tab.icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        }
                                    },
                                    label = { Text(tab.title, fontSize = 11.sp) },
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
                                onOpenCheckout = { viewModel.openPaymentDialog() }
                            )
                        }
                        PosTab.INVENTORY -> {
                            InventoryScreen(
                                products = products,
                                lowStockProducts = lowStockProducts,
                                settings = settings,
                                activeIndustry = activeIndustry,
                                onSaveProduct = { viewModel.saveProduct(it) },
                                onDeleteProduct = { viewModel.deleteProduct(it) },
                                onAdjustStock = { id, delta -> viewModel.adjustStock(id, delta) }
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
                                onIndustryChange = { viewModel.setIndustryMode(it) },
                                onResetCatalog = { viewModel.resetCatalogToDefaults() }
                            )
                        }
                    }
                }
            }
        }

        // Modals
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
