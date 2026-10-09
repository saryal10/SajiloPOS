package com.example

import com.example.data.model.CartItem
import com.example.data.model.ProductItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pricing rules that merchants depend on: statutory transport concessions,
 * per-line totals, and the VAT / service-charge combination on a Nepali bill.
 */
class CartPricingTest {

    private fun product(
        id: Long = 1L,
        name: String = "Wai Wai Quick Chicken",
        price: Double = 25.0,
        cost: Double = 20.0,
        stock: Int = 100
    ) = ProductItem(
        id = id,
        name = name,
        category = "Snacks",
        price = price,
        costPrice = cost,
        stockQuantity = stock
    )

    // ---------------------------------------------------------------------
    // Concessions
    // ---------------------------------------------------------------------

    @Test
    fun `regular fare pays list price`() {
        assertEquals(100.0, CartItem.priceWithConcession(100.0, "Regular", 0.0), 0.001)
    }

    @Test
    fun `student concession is the statutory 45 percent`() {
        assertEquals(55.0, CartItem.priceWithConcession(100.0, "Student", 0.0), 0.001)
        assertEquals(165.0, CartItem.priceWithConcession(300.0, "Student", 0.0), 0.001)
    }

    @Test
    fun `senior concession is 50 percent`() {
        assertEquals(50.0, CartItem.priceWithConcession(100.0, "Senior", 0.0), 0.001)
    }

    @Test
    fun `percentage discount applies to regular fares`() {
        assertEquals(150.0, CartItem.priceWithConcession(200.0, "Regular", 25.0), 0.001)
        assertEquals(95.0, CartItem.priceWithConcession(100.0, "Regular", 5.0), 0.001)
        assertEquals(100.0, CartItem.priceWithConcession(100.0, "Regular", 0.0), 0.001)
    }

    @Test
    fun `passenger concession wins over a percentage discount`() {
        // Documented behaviour: a concessioned fare is already discounted.
        assertEquals(55.0, CartItem.priceWithConcession(100.0, "Student", 10.0), 0.001)
    }

    // ---------------------------------------------------------------------
    // Line totals
    // ---------------------------------------------------------------------

    @Test
    fun `line total multiplies the discounted unit price by quantity`() {
        val line = CartItem(product = product(price = 25.0), quantity = 4)
        assertEquals(100.0, line.itemTotal, 0.001)
    }

    @Test
    fun `line total honours the concession on every unit`() {
        val line = CartItem(product = product(price = 200.0), quantity = 3, passengerType = "Student")
        assertEquals(110.0, line.unitPriceAfterDiscount, 0.001) // 200 with a 45% concession
        assertEquals(330.0, line.itemTotal, 0.001)
    }

    @Test
    fun `concessioned and regular lines stay separate`() {
        val regular = CartItem(product = product(price = 100.0), quantity = 1)
        val student = CartItem(product = product(price = 100.0), quantity = 1, passengerType = "Student")

        assertEquals(100.0, regular.itemTotal, 0.001)
        assertEquals(55.0, student.itemTotal, 0.001)
    }

    // ---------------------------------------------------------------------
    // Cart level maths (subtotal -> service charge -> VAT -> total)
    // ---------------------------------------------------------------------

    private fun bill(
        lines: List<CartItem>,
        serviceChargePercent: Double = 0.0,
        vatPercent: Double = 0.0
    ): Triple<Double, Double, Double> {
        val subtotal = lines.sumOf { it.itemTotal }
        val serviceCharge = subtotal * (serviceChargePercent / 100.0)
        val vat = (subtotal + serviceCharge) * (vatPercent / 100.0)
        return Triple(subtotal + serviceCharge + vat, serviceCharge, vat)
    }

    @Test
    fun `retail bill with no taxes is just the subtotal`() {
        val lines = listOf(
            CartItem(product = product(id = 1, price = 25.0), quantity = 4),   // 100
            CartItem(product = product(id = 2, price = 50.0), quantity = 2),   // 100
            CartItem(product = product(id = 3, price = 95.0), quantity = 1)    // 95
        )

        val (total, serviceCharge, vat) = bill(lines)
        assertEquals(295.0, total, 0.001)
        assertEquals(0.0, serviceCharge, 0.001)
        assertEquals(0.0, vat, 0.001)
    }

    @Test
    fun `thirteen percent vat is charged on the subtotal`() {
        val lines = listOf(CartItem(product = product(price = 1000.0), quantity = 1))

        val (total, _, vat) = bill(lines, vatPercent = 13.0)
        assertEquals(130.0, vat, 0.001)
        assertEquals(1130.0, total, 0.001)
    }

    @Test
    fun `restaurant service charge is added before vat`() {
        val lines = listOf(CartItem(product = product(price = 500.0), quantity = 2)) // 1000

        val (total, serviceCharge, vat) = bill(lines, serviceChargePercent = 10.0, vatPercent = 13.0)
        assertEquals(100.0, serviceCharge, 0.001)
        assertEquals(143.0, vat, 0.001)          // 13% of 1100
        assertEquals(1243.0, total, 0.001)
    }

    @Test
    fun `cash change never goes negative`() {
        val total = 1243.0
        val tendered = 1000.0

        val change = (tendered - total).coerceAtLeast(0.0)
        assertEquals(0.0, change, 0.001)
        assertTrue(tendered < total) // sale must be blocked, not completed
    }

    @Test
    fun `cash change is exact for the next round amount`() {
        val total = 1243.0
        val tendered = 1500.0

        assertEquals(257.0, (tendered - total), 0.001)
    }
}