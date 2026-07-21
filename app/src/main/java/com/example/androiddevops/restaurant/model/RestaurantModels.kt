package com.example.androiddevops.restaurant.model

/** Menu item displayed in the catalog. */
data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val priceUsd: Double,
    val imageUrl: String,
    val category: String,
)

/** Lightweight item for cart and orders. */
data class OrderLine(
    val itemId: String,
    val itemName: String,
    val quantity: Int,
    val unitPriceUsd: Double,
    val imageUrl: String,
)

enum class OrderStatus {
    CURRENT,
    PAST,
}

/** Order persisted in-memory for demo purposes. */
data class RestaurantOrder(
    val id: String,
    val items: List<OrderLine>,
    val totalAmountUsd: Double,
    val placedAtEpochMillis: Long,
    val estimatedReadyAtEpochMillis: Long,
)

/** UI-facing cart state. */
data class CartState(
    val items: List<OrderLine> = emptyList(),
    val totalAmountUsd: Double = 0.0,
)
