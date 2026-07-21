package com.example.androiddevops

data class OrderResponse(
    val orderId: String,
    val itemName: String,
    val total: Double,
    val etaMinutes: Int
)

class MockAPIClient {
    fun placeOrder(itemName: String): OrderResponse {
        // Simulate an API call
        return OrderResponse(
            orderId = "ORD-${(1000..9999).random()}",
            itemName = itemName,
            total = 5.99,
            etaMinutes = 25
        )
    }
}
