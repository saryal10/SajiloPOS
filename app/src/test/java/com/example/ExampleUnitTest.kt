package com.example

import com.example.data.model.CartItem
import com.example.data.model.ProductItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testStudentConcession45PercentDiscount() {
    val basePrice = 100.0
    val regularPrice = CartItem.priceWithConcession(basePrice, "Regular", 0.0)
    val studentPrice = CartItem.priceWithConcession(basePrice, "Student", 0.0)
    val seniorPrice = CartItem.priceWithConcession(basePrice, "Senior", 0.0)

    assertEquals(100.0, regularPrice, 0.01)
    assertEquals(55.0, studentPrice, 0.01) // 45% discount -> 55%
    assertEquals(50.0, seniorPrice, 0.01) // 50% discount -> 50%
  }

  @Test
  fun testCartItemTotal() {
    val item = ProductItem(
      id = 1,
      name = "Wai Wai Noodles",
      price = 25.0,
      stockQuantity = 50,
      category = "Snacks"
    )
    val cartItem = CartItem(product = item, quantity = 4)
    assertEquals(100.0, cartItem.itemTotal, 0.01)
  }
}

