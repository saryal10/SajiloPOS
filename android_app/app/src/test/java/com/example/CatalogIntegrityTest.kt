package com.example

import com.example.data.local.SampleData
import com.example.data.model.IndustryMode
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the demo catalog that ships with the app. A bad seed value shows up as a
 * broken counter on a real shop's first day, so these invariants are worth pinning.
 */
class CatalogIntegrityTest {

    private val allProducts: List<ProductItem> =
        SampleData.retailProducts + SampleData.restaurantProducts + SampleData.transportProducts

    @Test
    fun `every industry mode ships with products`() {
        val modes = allProducts.map { it.industryMode }.toSet()
        val expected = IndustryMode.entries.map { it.name }.toSet()

        assertEquals("catalog must cover all business types", expected, modes)
        assertTrue(modes.size >= 3)
    }

    @Test
    fun `barcodes are unique across the whole catalog`() {
        val barcodes = allProducts.map { it.barcode }
        assertTrue("every product needs a barcode", barcodes.all { it.isNotBlank() })
        assertEquals(
            "duplicate barcodes break scanning",
            barcodes.size,
            barcodes.distinct().size
        )
    }

    @Test
    fun `products always have a name category and unit`() {
        allProducts.forEach { product ->
            assertTrue("blank name in catalog", product.name.isNotBlank())
            assertTrue("blank category for ${product.name}", product.category.isNotBlank())
            assertTrue("blank unit for ${product.name}", product.unit.isNotBlank())
        }
    }

    @Test
    fun `prices are positive and cost never exceeds price`() {
        allProducts.forEach { product ->
            assertTrue("non positive price for ${product.name}", product.price > 0)
            assertTrue("negative cost for ${product.name}", product.costPrice >= 0)
            assertTrue(
                "cost above price for ${product.name}",
                product.costPrice <= product.price
            )
        }
    }

    @Test
    fun `stock and thresholds are never negative`() {
        allProducts.forEach { product ->
            assertTrue("negative stock for ${product.name}", product.stockQuantity >= 0)
            assertTrue("negative threshold for ${product.name}", product.minStockThreshold >= 0)
        }
    }

    @Test
    fun `catalog demonstrates the low stock path`() {
        val lowStock = allProducts.filter { it.stockQuantity <= it.minStockThreshold }
        assertTrue("demo catalog should exercise low stock alerts", lowStock.isNotEmpty())
    }

    @Test
    fun `transport products carry their route metadata`() {
        val transport = allProducts.filter { it.industryMode == IndustryMode.TRANSPORT.name }
        assertTrue(transport.isNotEmpty())
        transport.forEach { product ->
            assertNotNull("${product.name} is missing a route", product.destinationRoute)
            assertTrue(product.destinationRoute!!.isNotBlank())
            assertEquals("ticket", product.unit)
        }
    }

    @Test
    fun `retail and restaurant products do not carry route metadata`() {
        allProducts
            .filter { it.industryMode != IndustryMode.TRANSPORT.name }
            .forEach { product ->
                assertTrue(
                    "${product.name} should not have a route",
                    product.destinationRoute == null
                )
            }
    }

    @Test
    fun `restaurant catalog contains kitchen items`() {
        val kitchen = SampleData.restaurantProducts.count { it.isKitchenItem }
        assertTrue("KOT printing needs kitchen items", kitchen > 0)
    }

    @Test
    fun `nepali names are supplied for catalog search`() {
        assertTrue(
            "Devanagari names power Nepali-language search",
            allProducts.all { it.nepaliName.isNotBlank() }
        )
    }

    @Test
    fun `demo tables and routes are populated`() {
        assertTrue(SampleData.defaultTables.isNotEmpty())
        assertTrue(SampleData.defaultTables.all { it.name.isNotBlank() && it.capacity > 0 })
        assertTrue(SampleData.transportRoutes.isNotEmpty())
        SampleData.transportRoutes.forEach { route ->
            assertTrue(route.routeName.isNotBlank())
            assertTrue(route.busNumber.isNotBlank())
            assertTrue("fare must be positive", route.baseFareNpr > 0)
            assertTrue("routes need intermediate stops", route.intermediateStops.isNotEmpty())
        }
    }

    // ---------------------------------------------------------------------
    // Payment rails
    // ---------------------------------------------------------------------

    @Test
    fun `all four nepalese payment rails are supported`() {
        val codes = PaymentMethod.entries.map { it.code }
        assertEquals(4, codes.size)
        assertEquals(codes.size, codes.distinct().size)
        assertTrue(codes.containsAll(listOf("CASH", "ESEWA", "FONEPAY", "KHALTI")))
    }

    @Test
    fun `payment rails map back from their stored code`() {
        assertEquals(PaymentMethod.FONEPAY, PaymentMethod.fromCode("FONEPAY"))
        assertEquals(PaymentMethod.ESEWA, PaymentMethod.fromCode("esewa"))
        assertEquals(PaymentMethod.KHALTI, PaymentMethod.fromCode("  Khalti  ".trim()))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("CASH"))
    }

    @Test
    fun `unknown payment codes fall back to cash`() {
        // Old receipts must still render instead of crashing the history screen.
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("CONNECTIPS_205"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode(""))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("unknown"))
    }

    @Test
    fun `every payment rail has a customer facing label`() {
        PaymentMethod.entries.forEach { method ->
            assertTrue(method.displayName.isNotBlank())
            assertTrue(method.shortLabel.isNotBlank())
            assertFalse(method.shortLabel.length > 8)
        }
    }
}