package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.ProductDao
import com.example.data.local.SampleData
import com.example.data.local.TransactionDao
import com.example.data.model.BusinessSettings
import com.example.data.model.CartItem
import com.example.data.model.IndustryMode
import com.example.data.model.ProductItem
import com.example.data.model.SaleTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PosRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val productDao: ProductDao = database.productDao()
    private val transactionDao: TransactionDao = database.transactionDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("sajilo_pos_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow = _settingsFlow.asStateFlow()

    private fun loadSettings(): BusinessSettings {
        val modeStr = prefs.getString("industry_mode", IndustryMode.RETAIL.name) ?: IndustryMode.RETAIL.name
        val mode = try {
            IndustryMode.valueOf(modeStr)
        } catch (e: Exception) {
            IndustryMode.RETAIL
        }

        return BusinessSettings(
            businessName = prefs.getString("business_name", "Himalayan Mart & Cafe") ?: "Himalayan Mart & Cafe",
            panVatNumber = prefs.getString("pan_vat_number", "602394812") ?: "602394812",
            address = prefs.getString("business_address", "New Road, Kathmandu, Nepal") ?: "New Road, Kathmandu, Nepal",
            phone = prefs.getString("business_phone", "+977 1-4235890") ?: "+977 1-4235890",
            vatEnabled = prefs.getBoolean("vat_enabled", true),
            vatRatePercent = prefs.getFloat("vat_rate", 13.0f).toDouble(),
            serviceChargeEnabled = prefs.getBoolean("service_charge_enabled", false),
            serviceChargePercent = prefs.getFloat("service_charge_rate", 10.0f).toDouble(),
            printerWidth = prefs.getString("printer_width", "80mm") ?: "80mm",
            currencySymbol = prefs.getString("currency_symbol", "रू") ?: "रू",
            activeIndustry = mode
        )
    }

    suspend fun updateSettings(settings: BusinessSettings) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString("business_name", settings.businessName)
            putString("pan_vat_number", settings.panVatNumber)
            putString("business_address", settings.address)
            putString("business_phone", settings.phone)
            putBoolean("vat_enabled", settings.vatEnabled)
            putFloat("vat_rate", settings.vatRatePercent.toFloat())
            putBoolean("service_charge_enabled", settings.serviceChargeEnabled)
            putFloat("service_charge_rate", settings.serviceChargePercent.toFloat())
            putString("printer_width", settings.printerWidth)
            putString("currency_symbol", settings.currencySymbol)
            putString("industry_mode", settings.activeIndustry.name)
            apply()
        }
        _settingsFlow.value = settings
    }

    // Products
    fun getProductsByIndustry(mode: IndustryMode): Flow<List<ProductItem>> {
        return productDao.getProductsByIndustry(mode.name)
    }

    fun getAllProducts(): Flow<List<ProductItem>> = productDao.getAllProducts()

    fun getLowStockProducts(): Flow<List<ProductItem>> = productDao.getLowStockProducts()

    fun searchProducts(query: String): Flow<List<ProductItem>> = productDao.searchProducts(query)

    suspend fun findByBarcode(barcode: String): ProductItem? = withContext(Dispatchers.IO) {
        productDao.findByBarcode(barcode)
    }

    suspend fun saveProduct(product: ProductItem): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(id: Long) = withContext(Dispatchers.IO) {
        productDao.deleteById(id)
    }

    suspend fun adjustStock(productId: Long, delta: Int) = withContext(Dispatchers.IO) {
        if (delta > 0) {
            productDao.incrementStock(productId, delta)
        } else {
            productDao.decrementStock(productId, -delta)
        }
    }

    // Transactions
    fun getAllTransactions(): Flow<List<SaleTransaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByIndustry(mode: IndustryMode): Flow<List<SaleTransaction>> {
        return transactionDao.getTransactionsByIndustry(mode.name)
    }

    suspend fun recordSale(
        cartItems: List<CartItem>,
        subtotal: Double,
        discountTotal: Double,
        serviceCharge: Double,
        vatAmount: Double,
        grandTotal: Double,
        paymentMethod: String,
        paymentStatus: String,
        transactionRef: String,
        industryMode: IndustryMode,
        metaInfo: String,
        cashTendered: Double = 0.0,
        cashChange: Double = 0.0
    ): SaleTransaction = withContext(Dispatchers.IO) {
        // Generate Invoice Number: INV-YYYYMMDD-XXXX
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomSuffix = Random.nextInt(1000, 9999)
        val invoiceNo = "INV-$datePrefix-$randomSuffix"

        // Build itemized summary text
        val summaryBuilder = StringBuilder()
        for (item in cartItems) {
            summaryBuilder.append("${item.quantity}x ${item.product.name} @ NPR ${"%.2f".format(item.unitPriceAfterDiscount)}")
            if (item.passengerType != "Regular") {
                summaryBuilder.append(" [${item.passengerType}]")
            }
            if (item.notes.isNotBlank()) {
                summaryBuilder.append(" (${item.notes})")
            }
            summaryBuilder.append("\n")
        }

        val sale = SaleTransaction(
            invoiceNumber = invoiceNo,
            timestamp = System.currentTimeMillis(),
            subtotal = subtotal,
            discountTotal = discountTotal,
            serviceCharge = serviceCharge,
            vatTaxAmount = vatAmount,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            transactionRef = transactionRef,
            industryMode = industryMode.name,
            metaInfo = metaInfo,
            itemsSummary = summaryBuilder.toString().trim(),
            cashTendered = cashTendered,
            cashChange = cashChange
        )

        val insertedId = transactionDao.insertTransaction(sale)

        // Decrement stock in SQLite for inventory items
        for (item in cartItems) {
            productDao.decrementStock(item.product.id, item.quantity)
        }

        sale.copy(id = insertedId)
    }

    suspend fun seedSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = productDao.getProductCount()
        if (count == 0) {
            productDao.insertAll(SampleData.retailProducts)
            productDao.insertAll(SampleData.restaurantProducts)
            productDao.insertAll(SampleData.transportProducts)
        }
    }

    suspend fun resetCatalogToDefault() = withContext(Dispatchers.IO) {
        // Re-insert initial sets
        productDao.insertAll(SampleData.retailProducts)
        productDao.insertAll(SampleData.restaurantProducts)
        productDao.insertAll(SampleData.transportProducts)
    }
}
