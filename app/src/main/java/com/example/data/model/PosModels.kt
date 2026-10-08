package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IndustryMode(val title: String, val subtitle: String) {
    RETAIL("Retail Store", "Groceries, items & barcodes"),
    RESTAURANT("Restaurant & Cafe", "Table service & KOT"),
    TRANSPORT("Public Transport", "Bus routes & tickets")
}

enum class PaymentMethod(val displayName: String, val code: String) {
    CASH("Cash Payment", "CASH"),
    ESEWA("eSewa Wallet", "ESEWA"),
    FONEPAY("Fonepay QR", "FONEPAY"),
    KHALTI("Khalti Wallet", "KHALTI")
}

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    BILLING
}

data class RestaurantTable(
    val id: Int,
    val name: String,
    val capacity: Int,
    val status: TableStatus = TableStatus.AVAILABLE,
    val currentTabTotal: Double = 0.0,
    val activeItemCount: Int = 0
)

data class TransportRoute(
    val id: String,
    val routeName: String,
    val busNumber: String,
    val fromStop: String,
    val toStop: String,
    val baseFareNpr: Double,
    val intermediateStops: List<String>
)

@Entity(tableName = "products")
data class ProductItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nepaliName: String = "",
    val barcode: String = "",
    val category: String,
    val price: Double, // in NPR
    val costPrice: Double = 0.0,
    val stockQuantity: Int,
    val minStockThreshold: Int = 5,
    val unit: String = "pcs", // pcs, kg, pkt, plate, ticket
    val industryMode: String = "RETAIL", // RETAIL, RESTAURANT, TRANSPORT
    val destinationRoute: String? = null,
    val isKitchenItem: Boolean = false // For KOT printing in restaurant mode
)

data class CartItem(
    val product: ProductItem,
    val quantity: Int = 1,
    val notes: String = "",
    val passengerType: String = "Regular", // Regular, Student (-45%), Senior (-50%)
    val discountPercent: Double = 0.0
) {
    val unitPriceAfterDiscount: Double
        get() = priceWithConcession(product.price, passengerType, discountPercent)

    val itemTotal: Double
        get() = unitPriceAfterDiscount * quantity

    companion object {
        fun priceWithConcession(basePrice: Double, passengerType: String, customDiscount: Double): Double {
            return when {
                passengerType == "Student" -> basePrice * 0.55 // Nepal statutory 45% discount
                passengerType == "Senior" -> basePrice * 0.50  // 50% concession
                customDiscount > 0 -> basePrice * (1.0 - (customDiscount / 100.0))
                else -> basePrice
            }
        }
    }
}

@Entity(tableName = "transactions")
data class SaleTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discountTotal: Double,
    val serviceCharge: Double,
    val vatTaxAmount: Double,
    val grandTotal: Double,
    val paymentMethod: String,
    val paymentStatus: String, // e.g. "205_VERIFIED_OK", "PAID_CASH"
    val transactionRef: String,
    val industryMode: String,
    val metaInfo: String = "", // Table #4, Route: Ratnapark-Bhaktapur, etc.
    val itemsSummary: String = "", // JSON or plaintext of line items
    val cashTendered: Double = 0.0,
    val cashChange: Double = 0.0
)

data class BusinessSettings(
    val businessName: String = "Himalayan Express Mart & Cafe",
    val panVatNumber: String = "602394812",
    val address: String = "New Road, Ward 22, Kathmandu, Nepal",
    val phone: String = "+977 1-4235890",
    val vatEnabled: Boolean = true, // 13% Nepal VAT
    val vatRatePercent: Double = 13.0,
    val serviceChargeEnabled: Boolean = false, // 10% Restaurant service charge
    val serviceChargePercent: Double = 10.0,
    val printerWidth: String = "80mm", // "58mm" or "80mm"
    val currencySymbol: String = "रू",
    val activeIndustry: IndustryMode = IndustryMode.RETAIL
)
