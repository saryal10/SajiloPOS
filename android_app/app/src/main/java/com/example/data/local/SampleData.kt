package com.example.data.local

import com.example.data.model.ProductItem
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.data.model.TransportRoute

object SampleData {
    val retailProducts = listOf(
        ProductItem(
            name = "Wai Wai Quick Chicken",
            nepaliName = "वाइ वाइ चाउचाउ",
            barcode = "8901234001",
            category = "Snacks",
            price = 25.0,
            costPrice = 20.0,
            stockQuantity = 85,
            minStockThreshold = 15,
            unit = "pkt",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "DDC Pasteurized Milk 500ml",
            nepaliName = "डीडीसी शुद्ध दुध",
            barcode = "8901234002",
            category = "Dairy",
            price = 50.0,
            costPrice = 44.0,
            stockQuantity = 4, // Low stock trigger!
            minStockThreshold = 10,
            unit = "pkt",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Tokla CTC Gold Tea 500g",
            nepaliName = "तोक्ला चिया पत्ती",
            barcode = "8901234003",
            category = "Beverages",
            price = 240.0,
            costPrice = 205.0,
            stockQuantity = 32,
            minStockThreshold = 5,
            unit = "pkt",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Hulas Special Basmati Rice 5kg",
            nepaliName = "हुलास बासमती चामल",
            barcode = "8901234004",
            category = "Grains",
            price = 780.0,
            costPrice = 690.0,
            stockQuantity = 14,
            minStockThreshold = 5,
            unit = "bag",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Real Mixed Fruit Juice 1L",
            nepaliName = "रियल जुस १ लिटर",
            barcode = "8901234005",
            category = "Beverages",
            price = 260.0,
            costPrice = 220.0,
            stockQuantity = 18,
            minStockThreshold = 6,
            unit = "box",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Current Hot 'n' Spicy Noodles",
            nepaliName = "करेन्ट पिरो चाउचाउ",
            barcode = "8901234006",
            category = "Snacks",
            price = 60.0,
            costPrice = 48.0,
            stockQuantity = 3, // Low stock trigger!
            minStockThreshold = 12,
            unit = "pkt",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Dabur Pure Honey 250g",
            nepaliName = "डाबर मह",
            barcode = "8901234007",
            category = "Groceries",
            price = 210.0,
            costPrice = 180.0,
            stockQuantity = 22,
            minStockThreshold = 5,
            unit = "jar",
            industryMode = "RETAIL"
        ),
        ProductItem(
            name = "Everest Meat Masala 100g",
            nepaliName = "एभरेष्ट मिट मसला",
            barcode = "8901234008",
            category = "Spices",
            price = 95.0,
            costPrice = 80.0,
            stockQuantity = 40,
            minStockThreshold = 8,
            unit = "box",
            industryMode = "RETAIL"
        )
    )

    val restaurantProducts = listOf(
        ProductItem(
            name = "Steam Buff Momo (10 pcs)",
            nepaliName = "बफ स्टिम म:म",
            barcode = "MOMO-001",
            category = "Momo Special",
            price = 160.0,
            costPrice = 75.0,
            stockQuantity = 120,
            minStockThreshold = 20,
            unit = "plate",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Chicken C-Momo (Chilli)",
            nepaliName = "चिकन सि म:म",
            barcode = "MOMO-002",
            category = "Momo Special",
            price = 220.0,
            costPrice = 110.0,
            stockQuantity = 60,
            minStockThreshold = 10,
            unit = "plate",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Jhol Momo Special (Sesame Soup)",
            nepaliName = "झोल म:म विशेष",
            barcode = "MOMO-003",
            category = "Momo Special",
            price = 200.0,
            costPrice = 90.0,
            stockQuantity = 45,
            minStockThreshold = 10,
            unit = "plate",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Special Mix Chowmein",
            nepaliName = "मिक्स चाउमिन",
            barcode = "CHOW-001",
            category = "Noodles",
            price = 180.0,
            costPrice = 80.0,
            stockQuantity = 75,
            minStockThreshold = 15,
            unit = "plate",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Nepali Sekuwa Khaja Set",
            nepaliName = "नेपाली सेकुवा खाजा सेट",
            barcode = "KHAJA-001",
            category = "Platters",
            price = 350.0,
            costPrice = 160.0,
            stockQuantity = 30,
            minStockThreshold = 8,
            unit = "set",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Authentic Nepali Thali Set",
            nepaliName = "नेपाली खाना थाली सेट",
            barcode = "THALI-001",
            category = "Main Meals",
            price = 380.0,
            costPrice = 170.0,
            stockQuantity = 50,
            minStockThreshold = 10,
            unit = "thali",
            industryMode = "RESTAURANT",
            isKitchenItem = true
        ),
        ProductItem(
            name = "Traditional Spiced Milk Tea",
            nepaliName = "नेपाली मसला चिया",
            barcode = "TEA-001",
            category = "Beverages",
            price = 40.0,
            costPrice = 15.0,
            stockQuantity = 200,
            minStockThreshold = 30,
            unit = "cup",
            industryMode = "RESTAURANT",
            isKitchenItem = false
        ),
        ProductItem(
            name = "Fresh Lemon Ginger Honey",
            nepaliName = "ताजा कागती अदुवा मह",
            barcode = "DRK-001",
            category = "Beverages",
            price = 90.0,
            costPrice = 30.0,
            stockQuantity = 80,
            minStockThreshold = 15,
            unit = "glass",
            industryMode = "RESTAURANT",
            isKitchenItem = false
        )
    )

    val transportProducts = listOf(
        ProductItem(
            name = "Ratnapark -> Bhaktapur (Suryabinayak)",
            nepaliName = "रत्नपार्क - भक्तपुर",
            barcode = "BUS-001",
            category = "Valley Express",
            price = 35.0,
            stockQuantity = 999,
            minStockThreshold = 50,
            unit = "ticket",
            industryMode = "TRANSPORT",
            destinationRoute = "Route #11 (Sajha Yatayat) • Bus #BA 2 KHA 7842"
        ),
        ProductItem(
            name = "Kalanki -> Dhulikhel Highway",
            nepaliName = "कलंकी - धुलिखेल",
            barcode = "BUS-002",
            category = "Araniko Highway",
            price = 85.0,
            stockQuantity = 999,
            minStockThreshold = 50,
            unit = "ticket",
            industryMode = "TRANSPORT",
            destinationRoute = "Route #24 (Super Express) • Bus #BA 3 KHA 1109"
        ),
        ProductItem(
            name = "Sundhara -> Kirtipur Ring Route",
            nepaliName = "सुन्धारा - कीर्तिपुर",
            barcode = "BUS-003",
            category = "City Local",
            price = 25.0,
            stockQuantity = 999,
            minStockThreshold = 50,
            unit = "ticket",
            industryMode = "TRANSPORT",
            destinationRoute = "Route #04 (Dillibazar Link) • Bus #BA 1 KHA 4421"
        ),
        ProductItem(
            name = "Gongabu Buspark -> Pokhara Deluxe",
            nepaliName = "काठमाडौं - पोखरा डिलक्स",
            barcode = "BUS-004",
            category = "Intercity Tourist",
            price = 950.0,
            stockQuantity = 36,
            minStockThreshold = 5,
            unit = "ticket",
            industryMode = "TRANSPORT",
            destinationRoute = "Prithvi Highway AC • Bus #BA 5 KHA 9920"
        )
    )

    val defaultTables = listOf(
        RestaurantTable(id = 1, name = "Table 1 (Indoor)", capacity = 4, status = TableStatus.AVAILABLE),
        RestaurantTable(id = 2, name = "Table 2 (Window)", capacity = 2, status = TableStatus.OCCUPIED, currentTabTotal = 560.0, activeItemCount = 3),
        RestaurantTable(id = 3, name = "Table 3 (Family)", capacity = 6, status = TableStatus.AVAILABLE),
        RestaurantTable(id = 4, name = "Table 4 (Booth)", capacity = 4, status = TableStatus.BILLING, currentTabTotal = 1140.0, activeItemCount = 5),
        RestaurantTable(id = 5, name = "Table 5 (Rooftop)", capacity = 4, status = TableStatus.AVAILABLE),
        RestaurantTable(id = 6, name = "Table 6 (Balcony)", capacity = 2, status = TableStatus.AVAILABLE),
        RestaurantTable(id = 7, name = "Takeaway Counter", capacity = 1, status = TableStatus.AVAILABLE)
    )

    val transportRoutes = listOf(
        TransportRoute(
            id = "R1",
            routeName = "Ratnapark <-> Suryabinayak",
            busNumber = "बा २ ख ७८४२ (Sajha 11)",
            fromStop = "Ratnapark",
            toStop = "Suryabinayak",
            baseFareNpr = 35.0,
            intermediateStops = listOf("Maitighar", "Baneshwor", "Koteshwor", "Thimi", "Sallaghari", "Suryabinayak")
        ),
        TransportRoute(
            id = "R2",
            routeName = "Kalanki <-> Dhulikhel",
            busNumber = "बा ३ ख ११०९ (Express)",
            fromStop = "Kalanki",
            toStop = "Dhulikhel",
            baseFareNpr = 85.0,
            intermediateStops = listOf("Balkhu", "Satdobato", "Gwarko", "Koteshwor", "Banepa", "Dhulikhel")
        ),
        TransportRoute(
            id = "R3",
            routeName = "Sundhara <-> Kirtipur Naya Bazar",
            busNumber = "बा १ ख ४४२१",
            fromStop = "Sundhara",
            toStop = "Kirtipur",
            baseFareNpr = 25.0,
            intermediateStops = listOf("Tripureshwor", "Teku", "Kalimati", "Balkhu", "TU Gate", "Kirtipur")
        )
    )
}
