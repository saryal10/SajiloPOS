package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SampleData
import com.example.data.model.BusinessSettings
import com.example.data.model.CartItem
import com.example.data.model.IndustryMode
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductItem
import com.example.data.model.RestaurantTable
import com.example.data.model.SaleTransaction
import com.example.data.model.TableStatus
import com.example.data.model.TransportRoute
import com.example.data.repository.PosRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PaymentVerificationState(
    val isVerifying: Boolean = false,
    val currentStep: String = "",
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val transactionRef: String = ""
)

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository(application)

    val settings: StateFlow<BusinessSettings> = repository.settingsFlow

    private val _activeIndustry = MutableStateFlow(IndustryMode.RETAIL)
    val activeIndustry: StateFlow<IndustryMode> = _activeIndustry.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Reactive products from Room Database
    val products: StateFlow<List<ProductItem>> = combine(_activeIndustry, _searchQuery) { industry, query ->
        Pair(industry, query)
    }.flatMapLatest { (industry, query) ->
        if (query.isBlank()) {
            repository.getProductsByIndustry(industry)
        } else {
            repository.searchProducts(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductItem>> = repository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<SaleTransaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Restaurant Specific State
    private val _restaurantTables = MutableStateFlow(SampleData.defaultTables)
    val restaurantTables: StateFlow<List<RestaurantTable>> = _restaurantTables.asStateFlow()

    private val _selectedTable = MutableStateFlow<RestaurantTable?>(null)
    val selectedTable: StateFlow<RestaurantTable?> = _selectedTable.asStateFlow()

    // Transport Specific State
    val transportRoutes: List<TransportRoute> = SampleData.transportRoutes
    private val _selectedRoute = MutableStateFlow(transportRoutes.first())
    val selectedRoute: StateFlow<TransportRoute> = _selectedRoute.asStateFlow()

    private val _passengerType = MutableStateFlow("Regular") // Regular, Student, Senior
    val passengerType: StateFlow<String> = _passengerType.asStateFlow()

    // Payment Dialog & Verification State
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    private val _cashTendered = MutableStateFlow("")
    val cashTendered: StateFlow<String> = _cashTendered.asStateFlow()

    private val _paymentVerificationState = MutableStateFlow(PaymentVerificationState())
    val paymentVerificationState: StateFlow<PaymentVerificationState> = _paymentVerificationState.asStateFlow()

    // Receipt Modal State
    private val _completedSale = MutableStateFlow<SaleTransaction?>(null)
    val completedSale: StateFlow<SaleTransaction?> = _completedSale.asStateFlow()

    private val _showReceiptModal = MutableStateFlow(false)
    val showReceiptModal: StateFlow<Boolean> = _showReceiptModal.asStateFlow()

    // Barcode Scanner Modal State
    private val _showBarcodeScanner = MutableStateFlow(false)
    val showBarcodeScanner: StateFlow<Boolean> = _showBarcodeScanner.asStateFlow()

    private val _scannerScanFeedback = MutableStateFlow<String?>(null)
    val scannerScanFeedback: StateFlow<String?> = _scannerScanFeedback.asStateFlow()

    // Quick Toast / Snackbar Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfNeeded()
            _activeIndustry.value = settings.value.activeIndustry
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun setIndustryMode(mode: IndustryMode) {
        _activeIndustry.value = mode
        _selectedCategory.value = "All"
        _searchQuery.value = ""
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(activeIndustry = mode))
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Cart Management
    fun addToCart(product: ProductItem, quantity: Int = 1, passType: String = _passengerType.value, notes: String = "") {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst {
            it.product.id == product.id && it.passengerType == passType && it.notes == notes
        }

        if (existingIndex != -1) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(product = product, quantity = quantity, passengerType = passType, notes = notes))
        }
        _cartItems.value = current
        _userMessage.value = "Added ${product.name} to cart"
    }

    fun incrementCartItem(cartItem: CartItem) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOf(cartItem)
        if (index != -1) {
            current[index] = cartItem.copy(quantity = cartItem.quantity + 1)
            _cartItems.value = current
        }
    }

    fun decrementCartItem(cartItem: CartItem) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOf(cartItem)
        if (index != -1) {
            if (cartItem.quantity > 1) {
                current[index] = cartItem.copy(quantity = cartItem.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun removeCartItem(cartItem: CartItem) {
        val current = _cartItems.value.toMutableList()
        current.remove(cartItem)
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // Calculations
    val cartSubtotal: Double
        get() = _cartItems.value.sumOf { it.itemTotal }

    val serviceChargeAmount: Double
        get() = if (settings.value.serviceChargeEnabled && _activeIndustry.value == IndustryMode.RESTAURANT) {
            cartSubtotal * (settings.value.serviceChargePercent / 100.0)
        } else 0.0

    val vatAmount: Double
        get() = if (settings.value.vatEnabled) {
            (cartSubtotal + serviceChargeAmount) * (settings.value.vatRatePercent / 100.0)
        } else 0.0

    val grandTotal: Double
        get() = cartSubtotal + serviceChargeAmount + vatAmount

    // Barcode Simulation
    fun openBarcodeScanner() {
        _showBarcodeScanner.value = true
        _scannerScanFeedback.value = null
    }

    fun closeBarcodeScanner() {
        _showBarcodeScanner.value = false
        _scannerScanFeedback.value = null
    }

    fun scanBarcode(rawBarcode: String) {
        val barcode = rawBarcode.trim()
        if (barcode.isBlank()) return

        viewModelScope.launch {
            var product = repository.findByBarcode(barcode)

            // Try without leading zeros or search
            if (product == null && barcode.startsWith("0")) {
                product = repository.findByBarcode(barcode.trimStart('0'))
            }

            if (product != null) {
                addToCart(product)
                _scannerScanFeedback.value = "✓ Scanned: ${product.name} (NPR ${product.price})"
                delay(700)
                _showBarcodeScanner.value = false
            } else {
                // If scanned item is not in catalog, create a new custom scanned product so sale is never blocked!
                val newProduct = ProductItem(
                    name = "Scanned Item ($barcode)",
                    nepaliName = "स्क्यान गरिएको सामान",
                    barcode = barcode,
                    category = "Scanned",
                    price = 100.0,
                    costPrice = 80.0,
                    stockQuantity = 50,
                    unit = "pcs",
                    industryMode = _activeIndustry.value.name
                )
                val newId = repository.saveProduct(newProduct)
                val savedProduct = newProduct.copy(id = newId)
                addToCart(savedProduct)
                _scannerScanFeedback.value = "✓ Scanned & Added: ${newProduct.name} (NPR ${newProduct.price})"
                delay(700)
                _showBarcodeScanner.value = false
            }
        }
    }

    // Restaurant Actions
    fun selectTable(table: RestaurantTable) {
        _selectedTable.value = table
    }

    fun markTableOccupied(tableId: Int) {
        val updated = _restaurantTables.value.map {
            if (it.id == tableId) it.copy(status = TableStatus.OCCUPIED) else it
        }
        _restaurantTables.value = updated
    }

    fun markTableAvailable(tableId: Int) {
        val updated = _restaurantTables.value.map {
            if (it.id == tableId) it.copy(status = TableStatus.AVAILABLE, currentTabTotal = 0.0, activeItemCount = 0) else it
        }
        _restaurantTables.value = updated
    }

    // Transport Actions
    fun selectRoute(route: TransportRoute) {
        _selectedRoute.value = route
    }

    fun setPassengerType(type: String) {
        _passengerType.value = type
    }

    fun issueQuickTicket(stop: String) {
        val route = _selectedRoute.value
        val passType = _passengerType.value
        val ticketProduct = ProductItem(
            name = "${route.fromStop} -> $stop",
            nepaliName = "बस टिकट",
            barcode = "TKT-${System.currentTimeMillis() % 10000}",
            category = "Bus Ticket",
            price = route.baseFareNpr,
            stockQuantity = 9999,
            unit = "ticket",
            industryMode = "TRANSPORT",
            destinationRoute = "${route.routeName} (${route.busNumber})"
        )
        addToCart(ticketProduct, 1, passType, "Bus: ${route.busNumber}")
    }

    // Payment Dialog Flow
    fun openPaymentDialog() {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "Cart is empty!"
            return
        }
        _cashTendered.value = grandTotal.toInt().toString()
        _paymentVerificationState.value = PaymentVerificationState()
        _showPaymentDialog.value = true
    }

    fun closePaymentDialog() {
        _showPaymentDialog.value = false
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
        _paymentVerificationState.value = PaymentVerificationState()
    }

    fun setCashTendered(amount: String) {
        _cashTendered.value = amount
    }

    // Simulated Digital Payment Webhook Verification (eSewa / Fonepay / Khalti)
    fun verifyDigitalPayment() {
        viewModelScope.launch {
            val method = _selectedPaymentMethod.value
            val refCode = "TXN-NP-${method.code}-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

            _paymentVerificationState.value = PaymentVerificationState(
                isVerifying = true,
                currentStep = "Contacting ${method.displayName} Nepal Switch...",
                transactionRef = refCode
            )

            delay(700)
            _paymentVerificationState.value = _paymentVerificationState.value.copy(
                currentStep = "Validating dynamic EMVCo QR token for NPR ${"%.2f".format(grandTotal)}..."
            )

            delay(750)
            _paymentVerificationState.value = _paymentVerificationState.value.copy(
                currentStep = "Polling Webhook Callback: Checking transaction $refCode..."
            )

            delay(600)
            // Simulates authentic 205 OK confirmation payload
            _paymentVerificationState.value = _paymentVerificationState.value.copy(
                isVerifying = false,
                isSuccess = true,
                currentStep = "HTTP 205 OK: Transaction verified & settled via ${method.displayName}!"
            )

            delay(800)
            // Complete sale automatically upon verification
            finalizeSaleAndPromptReceipt(
                paymentMethod = method.code,
                paymentStatus = "205_VERIFIED_OK",
                transactionRef = refCode,
                tendered = grandTotal,
                change = 0.0
            )
        }
    }

    fun completeCashSale() {
        val tendered = _cashTendered.value.toDoubleOrNull() ?: grandTotal
        if (tendered < grandTotal) {
            _userMessage.value = "Tendered cash is less than grand total!"
            return
        }
        val change = tendered - grandTotal
        val refCode = "CASH-REG-${System.currentTimeMillis() % 100000}"

        finalizeSaleAndPromptReceipt(
            paymentMethod = "CASH",
            paymentStatus = "SETTLED_CASH",
            transactionRef = refCode,
            tendered = tendered,
            change = change
        )
    }

    private fun finalizeSaleAndPromptReceipt(
        paymentMethod: String,
        paymentStatus: String,
        transactionRef: String,
        tendered: Double,
        change: Double
    ) {
        viewModelScope.launch {
            val currentItems = _cartItems.value
            val sub = cartSubtotal
            val disc = 0.0
            val sc = serviceChargeAmount
            val vat = vatAmount
            val total = grandTotal
            val mode = _activeIndustry.value

            val meta = when (mode) {
                IndustryMode.RESTAURANT -> _selectedTable.value?.name ?: "Dine-In"
                IndustryMode.TRANSPORT -> _selectedRoute.value.busNumber
                IndustryMode.RETAIL -> "Counter POS 01"
            }

            val savedSale = repository.recordSale(
                cartItems = currentItems,
                subtotal = sub,
                discountTotal = disc,
                serviceCharge = sc,
                vatAmount = vat,
                grandTotal = total,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                transactionRef = transactionRef,
                industryMode = mode,
                metaInfo = meta,
                cashTendered = tendered,
                cashChange = change
            )

            // If restaurant table was billing, mark available
            if (mode == IndustryMode.RESTAURANT && _selectedTable.value != null) {
                markTableAvailable(_selectedTable.value!!.id)
                _selectedTable.value = null
            }

            _completedSale.value = savedSale
            _cartItems.value = emptyList()
            _showPaymentDialog.value = false
            _showReceiptModal.value = true
        }
    }

    fun closeReceiptModal() {
        _showReceiptModal.value = false
        _completedSale.value = null
    }

    fun openReprintReceipt(sale: SaleTransaction) {
        _completedSale.value = sale
        _showReceiptModal.value = true
    }

    // Inventory CRUD
    fun saveProduct(product: ProductItem) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "Product '${product.name}' saved"
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _userMessage.value = "Product removed from inventory"
        }
    }

    fun adjustStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta)
        }
    }

    // Settings Updates
    fun updateBusinessSettings(newSettings: BusinessSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            _userMessage.value = "Business settings saved"
        }
    }

    fun resetCatalogToDefaults() {
        viewModelScope.launch {
            repository.resetCatalogToDefault()
            _userMessage.value = "Sample catalog reloaded"
        }
    }
}
