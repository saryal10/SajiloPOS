package com.example

import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.InsightRange
import com.example.data.analytics.RestockUrgency
import com.example.data.local.DailySalesRow
import com.example.data.local.ItemVelocityRow
import com.example.data.local.PaymentMixRow
import com.example.data.model.ProductItem
import com.example.ui.util.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the pure analytics + formatting logic that powers the Insights tab.
 * No Android framework required, so these run on the JVM in seconds.
 */
class AnalyticsEngineTest {

    private fun daysAgo(days: Int, now: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    @Test
    fun dailySeriesFillsMissingDaysWithZero() {
        val now = 1_700_000_000_000L
        val rows = listOf(
            DailySalesRow(day = daysAgo(0, now), total = 500.0, orders = 2),
            DailySalesRow(day = daysAgo(3, now), total = 250.0, orders = 1)
        )

        val series = AnalyticsEngine.buildDailySeries(rows, days = 7, now = now)

        assertEquals(7, series.size)
        assertEquals(750.0, series.sumOf { it.total }, 0.01)
        assertEquals(3, series.sumOf { it.orders })
        // The series must be chronological and end with today.
        assertEquals(daysAgo(0, now), series.last().dayKey)
        assertEquals(daysAgo(6, now), series.first().dayKey)
    }

    @Test
    fun paymentMixSharesSumToOne() {
        val slices = AnalyticsEngine.buildPaymentMix(
            listOf(
                PaymentMixRow(paymentMethod = "CASH", total = 300.0, orders = 3),
                PaymentMixRow(paymentMethod = "ESEWA", total = 100.0, orders = 1)
            )
        )

        assertEquals(2, slices.size)
        assertEquals(0.75f, slices.first { it.method == "CASH" }.share, 0.001f)
        assertEquals(0.25f, slices.first { it.method == "ESEWA" }.share, 0.001f)
        assertEquals(1.0f, slices.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun paymentMixIsEmptyWhenNothingWasSold() {
        assertTrue(AnalyticsEngine.buildPaymentMix(emptyList()).isEmpty())
    }

    @Test
    fun restockPredictsQuantityToCoverLeadTime() {
        val fastSeller = ProductItem(
            id = 1,
            name = "Wai Wai Noodles",
            category = "Snacks",
            price = 25.0,
            costPrice = 20.0,
            stockQuantity = 10,
            unit = "pkt",
            minStockThreshold = 5
        )

        // 70 units in 14 days -> 5/day -> 7 day lead time needs 35 units -> order 25 more.
        val suggestions = AnalyticsEngine.buildRestockSuggestions(
            products = listOf(fastSeller),
            velocity = listOf(ItemVelocityRow(productId = 1, name = "Wai Wai Noodles", units = 70, lastSoldAt = 0L)),
            windowDays = 14,
            leadTimeDays = 7
        )

        assertEquals(1, suggestions.size)
        val suggestion = suggestions.first()
        assertEquals(5.0, suggestion.avgDailySales, 0.001)
        assertEquals(2.0, suggestion.daysOfCover, 0.001)
        assertEquals(25, suggestion.suggestedQty)
        assertEquals(RestockUrgency.STOCKOUT_SOON, suggestion.urgency)
    }

    @Test
    fun restockFlagsOutOfStockItems() {
        val soldOut = ProductItem(
            id = 2,
            name = "DDC Milk",
            category = "Dairy",
            price = 50.0,
            stockQuantity = 0,
            unit = "pkt"
        )

        val suggestions = AnalyticsEngine.buildRestockSuggestions(
            products = listOf(soldOut),
            velocity = listOf(ItemVelocityRow(productId = 2, name = "DDC Milk", units = 14, lastSoldAt = 0L)),
            windowDays = 14,
            leadTimeDays = 7
        )

        assertEquals(RestockUrgency.OUT_OF_STOCK, suggestions.first().urgency)
        assertEquals(7, suggestions.first().suggestedQty)
    }

    @Test
    fun restockIgnoresUnsoldAndTransportProducts() {
        val slow = ProductItem(
            id = 3,
            name = "Tokla Tea",
            category = "Beverages",
            price = 240.0,
            stockQuantity = 40
        )
        val ticket = ProductItem(
            id = 4,
            name = "Ratnapark Ticket",
            category = "Valley Express",
            price = 35.0,
            stockQuantity = 0,
            industryMode = "TRANSPORT"
        )

        val suggestions = AnalyticsEngine.buildRestockSuggestions(
            products = listOf(slow, ticket),
            velocity = emptyList(),
            windowDays = 14,
            leadTimeDays = 7
        )

        assertTrue(suggestions.none { it.productId == 4L })
        assertTrue(suggestions.none { it.productId == 3L })
    }

    @Test
    fun insightRangeDefaultsToWeek() {
        assertEquals(InsightRange.WEEK, InsightRange.fromName("NOPE"))
        assertEquals(InsightRange.MONTH, InsightRange.fromName("MONTH"))
        assertEquals(30, InsightRange.MONTH.days)
    }

    @Test
    fun moneyAndNumbersAreFormattedForNepal() {
        assertEquals("रू 1,25,000", Format.money(125000.0, 0))
        assertEquals("रू 1,250.50", Format.money(1250.5, 2))
        assertEquals("12.5k", Format.compact(12500.0))
        assertEquals("13%", Format.percent(13.0))
        // Machine-parseable values must never contain grouping separators.
        assertEquals("1250.50", Format.plain(1250.5, 2))
        assertTrue(Format.plain(1250.5, 2).toDoubleOrNull() != null)
    }
}