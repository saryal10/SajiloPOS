package com.example.data.analytics

import com.example.data.local.DailySalesRow
import com.example.data.local.ItemVelocityRow
import com.example.data.local.PaymentMixRow
import com.example.data.model.ProductItem
import java.util.Calendar
import java.util.Locale

/** Selectable window for the Insights dashboard. */
enum class InsightRange(val label: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30);

    companion object {
        fun fromName(value: String?): InsightRange =
            entries.firstOrNull { it.name == value } ?: WEEK
    }
}

/** One bar in the sales trend chart. */
data class DailyPoint(
    val dayKey: String,
    val label: String,
    val total: Double,
    val orders: Int
)

/** Slice of the payment mix donut. */
data class PaymentSlice(
    val method: String,
    val total: Double,
    val orders: Int,
    val share: Float
)

/** A single predicted restock action. */
data class RestockSuggestion(
    val productId: Long,
    val name: String,
    val unit: String,
    val stock: Int,
    val avgDailySales: Double,
    val daysOfCover: Double,
    val suggestedQty: Int,
    val urgency: RestockUrgency,
    val stockValue: Double
)

enum class RestockUrgency { OUT_OF_STOCK, STOCKOUT_SOON, WATCH, PLENTY }

/**
 * Pure, side-effect free analytics maths. Kept free of Android APIs so it can be
 * unit tested and reused by any screen.
 */
object AnalyticsEngine {

    /**
     * Expands sparse SQL results into a continuous, gap-free series so charts never
     * lie about missing (zero-sale) days.
     */
    fun buildDailySeries(
        rows: List<DailySalesRow>,
        days: Int,
        now: Long = System.currentTimeMillis(),
        locale: Locale = Locale.US
    ): List<DailyPoint> {
        val byKey = rows.associateBy { it.day }
        val keys = ArrayList<String>(days)
        val labels = ArrayList<String>(days)

        for (offset in (days - 1) downTo 0) {
            val day = Calendar.getInstance()
            day.timeInMillis = now
            day.add(Calendar.DAY_OF_YEAR, -offset)
            val key = String.format(
                locale,
                "%04d-%02d-%02d",
                day.get(Calendar.YEAR),
                day.get(Calendar.MONTH) + 1,
                day.get(Calendar.DAY_OF_MONTH)
            )
            keys += key
            labels += String.format(locale, "%d %s", day.get(Calendar.DAY_OF_MONTH), MONTH_ABBR[day.get(Calendar.MONTH)])
        }

        return keys.mapIndexed { index, key ->
            val row = byKey[key]
            DailyPoint(
                dayKey = key,
                label = labels[index],
                total = row?.total ?: 0.0,
                orders = row?.orders ?: 0
            )
        }
    }

    /** Turns per-method totals into donut slices with 0..1 shares. */
    fun buildPaymentMix(rows: List<PaymentMixRow>): List<PaymentSlice> {
        val total = rows.sumOf { it.total }
        if (total <= 0.0) return emptyList()
        return rows
            .sortedByDescending { it.total }
            .map {
                PaymentSlice(
                    method = it.paymentMethod,
                    total = it.total,
                    orders = it.orders,
                    share = (it.total / total).toFloat().coerceIn(0f, 1f)
                )
            }
    }

    /**
     * Predictive restocking: average daily velocity over the window, days of cover left,
     * and how much to order to survive the supplier lead time.
     *
     * @param leadTimeDays how many days of stock the merchant wants on hand after restocking.
     */
    fun buildRestockSuggestions(
        products: List<ProductItem>,
        velocity: List<ItemVelocityRow>,
        windowDays: Int,
        leadTimeDays: Int = 7,
        costPriceOf: (ProductItem) -> Double = { it.costPrice }
    ): List<RestockSuggestion> {
        val unitsByProduct = velocity.associate { it.productId to it.units }
        val safeWindow = if (windowDays <= 0) 1 else windowDays

        return products
            .filter { it.industryMode != "TRANSPORT" } // tickets are not stock managed
            .map { product ->
                val units = unitsByProduct[product.id] ?: 0
                val avgDaily = units.toDouble() / safeWindow.toDouble()
                val stock = product.stockQuantity
                val cover = if (avgDaily <= 0.0) Double.MAX_VALUE else stock / avgDaily
                val target = ceilToInt(avgDaily * leadTimeDays)
                val suggested = (target - stock).coerceAtLeast(0)

                RestockSuggestion(
                    productId = product.id,
                    name = product.name,
                    unit = product.unit,
                    stock = stock,
                    avgDailySales = avgDaily,
                    daysOfCover = cover,
                    suggestedQty = suggested,
                    urgency = urgencyFor(stock = stock, suggested = suggested, avgDaily = avgDaily),
                    stockValue = costPriceOf(product) * stock
                )
            }
            .filter { it.suggestedQty > 0 || it.stock <= ZERO_STOCK_FALLBACK }
            .sortedWith(
                compareBy(
                    { it.urgency.ordinal },
                    { it.daysOfCover.takeIf { d -> d != Double.MAX_VALUE } ?: Double.MAX_VALUE }
                )
            )
    }

    /** Simple, readable urgency buckets used for colour coding. */
    fun urgencyFor(stock: Int, suggested: Int, avgDaily: Double): RestockUrgency = when {
        stock <= 0 -> RestockUrgency.OUT_OF_STOCK
        suggested > 0 && avgDaily > 0.0 -> RestockUrgency.STOCKOUT_SOON
        stock <= 10 -> RestockUrgency.WATCH
        else -> RestockUrgency.PLENTY
    }

    fun ceilToInt(value: Double): Int = kotlin.math.ceil(value).toInt()

    private val MONTH_ABBR = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private const val ZERO_STOCK_FALLBACK = 0
}