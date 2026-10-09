package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProductItem
import com.example.data.model.SaleLineItem
import com.example.data.model.SaleTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductItem>>

    @Query("SELECT * FROM products WHERE industryMode = :mode ORDER BY category ASC, name ASC")
    fun getProductsByIndustry(mode: String): Flow<List<ProductItem>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductItem?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR nepaliName LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductItem>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductItem>>

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity - :qty) WHERE id = :productId")
    suspend fun decrementStock(productId: Long, qty: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qty WHERE id = :productId")
    suspend fun incrementStock(productId: Long, qty: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductItem>)

    @Update
    suspend fun updateProduct(product: ProductItem)

    @Delete
    suspend fun deleteProduct(product: ProductItem)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<SaleTransaction>>

    @Query("SELECT * FROM transactions WHERE industryMode = :mode ORDER BY timestamp DESC")
    fun getTransactionsByIndustry(mode: String): Flow<List<SaleTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): SaleTransaction?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getTransactionByInvoice(invoiceNumber: String): SaleTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SaleTransaction): Long

    @Query("SELECT SUM(grandTotal) FROM transactions WHERE timestamp >= :sinceTimestamp")
    fun getTotalSalesSince(sinceTimestamp: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :sinceTimestamp")
    fun getTransactionCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE invoiceNumber LIKE :prefix || '%'")
    suspend fun countInvoicesWithPrefix(prefix: String): Int

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

// -----------------------------------------------------------------------------
// Analytics projections (Phase 2 of the requirement doc)
// -----------------------------------------------------------------------------

data class DailySalesRow(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "orders") val orders: Int
)

data class PaymentMixRow(
    @ColumnInfo(name = "paymentMethod") val paymentMethod: String,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "orders") val orders: Int
)

data class TopItemRow(
    @ColumnInfo(name = "productId") val productId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "units") val units: Int,
    @ColumnInfo(name = "revenue") val revenue: Double,
    @ColumnInfo(name = "cost") val cost: Double
)

data class ItemVelocityRow(
    @ColumnInfo(name = "productId") val productId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "units") val units: Int,
    @ColumnInfo(name = "lastSoldAt") val lastSoldAt: Long
)

data class MarginRow(
    @ColumnInfo(name = "revenue") val revenue: Double,
    @ColumnInfo(name = "cost") val cost: Double
)

@Dao
interface AnalyticsDao {

    @Query(
        "SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS day, " +
            "COALESCE(SUM(grandTotal), 0) AS total, COUNT(*) AS orders " +
            "FROM transactions WHERE timestamp >= :since GROUP BY day ORDER BY day ASC"
    )
    fun dailySalesSince(since: Long): Flow<List<DailySalesRow>>

    @Query(
        "SELECT paymentMethod, COALESCE(SUM(grandTotal), 0) AS total, COUNT(*) AS orders " +
            "FROM transactions WHERE timestamp >= :since GROUP BY paymentMethod ORDER BY total DESC"
    )
    fun paymentMixSince(since: Long): Flow<List<PaymentMixRow>>

    @Query(
        "SELECT productId, productName AS name, SUM(quantity) AS units, " +
            "COALESCE(SUM(lineTotal), 0) AS revenue, COALESCE(SUM(costTotal), 0) AS cost " +
            "FROM sale_items WHERE timestamp >= :since " +
            "GROUP BY productId, productName ORDER BY revenue DESC LIMIT :limit"
    )
    fun topItemsSince(since: Long, limit: Int): Flow<List<TopItemRow>>

    @Query(
        "SELECT productId, productName AS name, SUM(quantity) AS units, MAX(timestamp) AS lastSoldAt " +
            "FROM sale_items WHERE timestamp >= :since AND productId > 0 " +
            "GROUP BY productId, productName"
    )
    fun salesVelocitySince(since: Long): Flow<List<ItemVelocityRow>>

    @Query(
        "SELECT COALESCE(SUM(lineTotal), 0) AS revenue, COALESCE(SUM(costTotal), 0) AS cost " +
            "FROM sale_items WHERE timestamp >= :since"
    )
    fun marginSince(since: Long): Flow<MarginRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<SaleLineItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(item: SaleLineItem)
}
