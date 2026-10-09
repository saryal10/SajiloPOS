package com.example

import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.RestockUrgency
import com.example.data.local.ItemVelocityRow
import com.example.data.model.ProductItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Predictive restocking is the one feature that can quietly cost a shop money:
 * too little suggested stock means empty shelves, too much means dead capital.
 * These tests pin the maths, the lead-time behaviour and the safety filters.
 */
class RestockPredictionTest {

    private val WINDOW_DAYS = 14
    private val LEAD_TIME_DAYS = 7

    private fun product(
        id: Long,
        stock: Int,
        price: Double = 100.0,
        cost: Double = 80.0,
        mode: String = "RETAIL"
    ) = ProductItem(
        id = id,
        name = "Product $id",
        category = "General",
        price = price,
        costPrice = cost,
        stockQuantity = stock,
        minStockThreshold = 5,
        unit = "pcs",
        industryMode = mode
    )

    private fun velocity(id: Long, units: Int) =
        ItemVelocityRow(productId = id, name = "Product $id", units = units, lastSoldAt = 0L)

    private fun suggestions(
        products: List<ProductItem>,
        velocity: List<ItemVelocityRow>,
        leadTimeDays: Int = LEAD_TIME_DAYS
    ) = AnalyticsEngine.buildRestockSuggestions(
        products = products,
        velocity = velocity,
        windowDays = WINDOW_DAYS,
        leadTimeDays = leadTimeDays
    )

    // ---------------------------------------------------------------------
    // Velocity maths
    // ---------------------------------------------------------------------

    @Test
    fun `average daily sales divides units by the whole window`() {
        val result = suggestions(
            products = listOf(product(id = 1, stock = 0)),
            velocity = listOf(velocity(1, units = 70))
        ).single()

        assertEquals(5.0, result.avgDailySales, 0.001)
    }

    @Test
    fun `days of cover is stock divided by daily velocity`() {
        val result = suggestions(
            products = listOf(product(id = 1, stock = 12)),
            velocity = listOf(velocity(1, units = 70)) // 5/day
        ).single()

        assertEquals(2.4, result.daysOfCover, 0.001)
    }

    @Test
    fun `product with no sales has effectively unlimited cover`() {
        val result = suggestions(
            products = listOf(product(id = 1, stock = 0)),
            velocity = emptyList()
        ).single()

        assertEquals(0.0, result.avgDailySales, 0.001)
        assertEquals(Double.MAX_VALUE, result.daysOfCover, 0.0)
    }

    // ---------------------------------------------------------------------
    // Order quantity
    // ---------------------------------------------------------------------

    @Test
    fun `suggested quantity tops the shelf up to the lead time target`() {
        // 5/day * 7 day lead time = 35 units needed, 10 on the shelf -> order 25.
        val result = suggestions(
            products = listOf(product(id = 1, stock = 10)),
            velocity = listOf(velocity(1, units = 70))
        ).single()

        assertEquals(35, result.suggestedQty + 10)
        assertEquals(25, result.suggestedQty)
    }

    @Test
    fun `a longer lead time asks for more stock`() {
        val shortLead = suggestions(
            products = listOf(product(id = 1, stock = 5)),
            velocity = listOf(velocity(1, units = 70)),
            leadTimeDays = 7
        ).single()

        val longLead = suggestions(
            products = listOf(product(id = 1, stock = 5)),
            velocity = listOf(velocity(1, units = 70)),
            leadTimeDays = 21
        ).single()

        assertEquals(30, shortLead.suggestedQty)   // 35 - 5
        assertEquals(100, longLead.suggestedQty)   // 105 - 5
        assertTrue(longLead.suggestedQty > shortLead.suggestedQty)
    }

    @Test
    fun `partial days round the target up`() {
        // 1 unit / 14 days = 0.071/day; 10 day lead time -> 0.71 -> 1 unit, shelf is 0.
        val result = suggestions(
            products = listOf(product(id = 1, stock = 0)),
            velocity = listOf(velocity(1, units = 1)),
            leadTimeDays = 10
        ).single()

        assertEquals(1, result.suggestedQty)
    }

    @Test
    fun `stock value on the shelf is priced at cost`() {
        val result = suggestions(
            products = listOf(product(id = 1, stock = 20, price = 120.0, cost = 75.0)),
            velocity = listOf(velocity(1, units = 140)) // 10/day -> target 70
        ).single()

        assertEquals(1500.0, result.stockValue, 0.001) // 20 * 75
        assertEquals(50, result.suggestedQty)           // 70 - 20
    }

    // ---------------------------------------------------------------------
    // Urgency buckets
    // ---------------------------------------------------------------------

    @Test
    fun `out of stock is the most urgent state`() {
        assertEquals(RestockUrgency.OUT_OF_STOCK, AnalyticsEngine.urgencyFor(stock = 0, suggested = 5, avgDaily = 1.0))
    }

    @Test
    fun `a reorder needed with real sales flags a stockout risk`() {
        assertEquals(RestockUrgency.STOCKOUT_SOON, AnalyticsEngine.urgencyFor(stock = 4, suggested = 30, avgDaily = 5.0))
    }

    @Test
    fun `thin but not projected to run out is a watch item`() {
        assertEquals(RestockUrgency.WATCH, AnalyticsEngine.urgencyFor(stock = 4, suggested = 0, avgDaily = 0.0))
    }

    @Test
    fun `healthy shelves are plenty`() {
        assertEquals(RestockUrgency.PLENTY, AnalyticsEngine.urgencyFor(stock = 80, suggested = 0, avgDaily = 0.0))
    }

    // ---------------------------------------------------------------------
    // Filtering and ordering
    // ---------------------------------------------------------------------

    @Test
    fun `items that need nothing are not shown to the merchant`() {
        val result = suggestions(
            products = listOf(
                product(id = 1, stock = 500),  // plenty of stock, no demand
                product(id = 2, stock = 2)     // sold out
            ),
            velocity = listOf(velocity(2, units = 70))
        )

        assertEquals(1, result.size)
        assertEquals(2L, result.single().productId)
    }

    @Test
    fun `bus tickets are never treated as stock`() {
        val result = suggestions(
            products = listOf(
                product(id = 1, stock = 0, mode = "TRANSPORT"),
                product(id = 2, stock = 0, mode = "RETAIL")
            ),
            velocity = listOf(velocity(1, units = 300), velocity(2, units = 10))
        )

        assertEquals(1, result.size)
        assertEquals(2L, result.single().productId)
    }

    @Test
    fun `the most urgent item is listed first`() {
        val result = suggestions(
            products = listOf(
                product(id = 1, stock = 6),   // slow-ish, still needs a top up
                product(id = 2, stock = 0),   // sold out
                product(id = 3, stock = 20)   // needs a big top up
            ),
            velocity = listOf(
                velocity(1, units = 21),      // 1.5/day
                velocity(2, units = 70),      // 5/day
                velocity(3, units = 140)      // 10/day
            )
        )

        assertEquals(3, result.size)
        assertEquals("sold out comes first", 2L, result.first().productId)
        assertTrue(result.all { it.urgency != RestockUrgency.PLENTY })
    }

    @Test
    fun `empty catalog produces no suggestions`() {
        assertTrue(suggestions(products = emptyList(), velocity = emptyList()).isEmpty())
    }

    @Test
    fun `a custom cost lookup is respected`() {
        val result = AnalyticsEngine.buildRestockSuggestions(
            products = listOf(product(id = 1, stock = 10, cost = 50.0)),
            velocity = listOf(velocity(1, units = 70)),
            windowDays = WINDOW_DAYS,
            leadTimeDays = LEAD_TIME_DAYS,
            costPriceOf = { _ -> 10.0 }
        ).single()

        assertNotNull(result)
        assertEquals(100.0, result.stockValue, 0.001) // 10 units * overridden cost 10
    }

    @Test
    fun `ceil helper rounds fractional targets up`() {
        assertEquals(1, AnalyticsEngine.ceilToInt(0.1))
        assertEquals(3, AnalyticsEngine.ceilToInt(2.2))
        assertEquals(10, AnalyticsEngine.ceilToInt(10.0))
        assertEquals(0, AnalyticsEngine.ceilToInt(0.0))
    }
}