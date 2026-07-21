package com.example.androiddevops.restaurant.data

import com.example.androiddevops.restaurant.domain.RestaurantRepository
import com.example.androiddevops.restaurant.model.CartState
import com.example.androiddevops.restaurant.model.MenuItem
import com.example.androiddevops.restaurant.model.OrderLine
import com.example.androiddevops.restaurant.model.RestaurantOrder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L

@Singleton
class InMemoryRestaurantRepository
    @Inject
    constructor() : RestaurantRepository {
        private val menuItems =
            listOf(
                MenuItem(
                    id = "m1",
                    name = "Big Mac",
                    description = "Double patty burger with signature sauce.",
                    priceUsd = 5.99,
                    imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=800&q=80",
                    category = "BURGER",
                ),
                MenuItem(
                    id = "m2",
                    name = "McSpicy Chicken",
                    description = "Crispy spicy chicken burger.",
                    priceUsd = 6.49,
                    imageUrl = "https://images.unsplash.com/photo-1610614819513-58e34989848b?auto=format&fit=crop&w=800&q=80",
                    category = "BURGER",
                ),
                MenuItem(
                    id = "m3",
                    name = "Fries (Large)",
                    description = "Golden and crispy fries.",
                    priceUsd = 2.99,
                    imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=800&q=80",
                    category = "SNACK",
                ),
                MenuItem(
                    id = "m4",
                    name = "Coke (Medium)",
                    description = "Chilled fizzy soft drink.",
                    priceUsd = 1.99,
                    imageUrl = "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=80",
                    category = "DRINK",
                ),
                MenuItem(
                    id = "m5",
                    name = "Soft Serve Cone",
                    description = "Classic vanilla soft serve.",
                    priceUsd = 1.49,
                    imageUrl = "https://images.unsplash.com/photo-1567206563064-6f60f40a2b57?auto=format&fit=crop&w=800&q=80",
                    category = "DESSERT",
                ),
            )

        private val menuFlow = MutableStateFlow(menuItems)

        private val cartFlow = MutableStateFlow(CartState())

        private val ordersFlow =
            MutableStateFlow(
                listOf(
                    RestaurantOrder(
                        id = "order-9001",
                        items =
                            listOf(
                                OrderLine(
                                    itemId = "m3",
                                    itemName = "Fries (Large)",
                                    quantity = 1,
                                    unitPriceUsd = 2.99,
                                    imageUrl = menuItems.first { it.id == "m3" }.imageUrl,
                                ),
                            ),
                        totalAmountUsd = 2.99,
                        placedAtEpochMillis = System.currentTimeMillis() - (3 * 60 * 60 * 1000L),
                        estimatedReadyAtEpochMillis = System.currentTimeMillis() - (3 * 60 * 60 * 1000L) + (20 * 60 * 1000L),
                    ),
                ),
            )

        override fun observeMenuItems(): Flow<List<MenuItem>> = menuFlow

        override fun observeCartState(): Flow<CartState> = cartFlow

        override fun observeOrders(): Flow<List<RestaurantOrder>> = ordersFlow

        override suspend fun addItemToCart(itemId: String) {
            val menuItem = menuItems.firstOrNull { it.id == itemId } ?: return
            cartFlow.update { current ->
                val existing = current.items.firstOrNull { it.itemId == menuItem.id }
                val updatedItems =
                    if (existing == null) {
                        current.items +
                            OrderLine(
                                itemId = menuItem.id,
                                itemName = menuItem.name,
                                quantity = 1,
                                unitPriceUsd = menuItem.priceUsd,
                                imageUrl = menuItem.imageUrl,
                            )
                    } else {
                        current.items.map {
                            if (it.itemId == existing.itemId) {
                                it.copy(quantity = it.quantity + 1)
                            } else {
                                it
                            }
                        }
                    }
                current.copy(
                    items = updatedItems,
                    totalAmountUsd = updatedItems.sumOf { it.quantity * it.unitPriceUsd },
                )
            }
        }

        override suspend fun removeItemFromCart(itemId: String) {
            cartFlow.update { current ->
                val updatedItems =
                    current.items.mapNotNull {
                        if (it.itemId != itemId) {
                            it
                        } else if (it.quantity > 1) {
                            it.copy(quantity = it.quantity - 1)
                        } else {
                            null
                        }
                    }
                current.copy(
                    items = updatedItems,
                    totalAmountUsd = updatedItems.sumOf { it.quantity * it.unitPriceUsd },
                )
            }
        }

        override suspend fun placeCurrentCartOrder(): RestaurantOrder {
            val cart = cartFlow.value
            require(cart.items.isNotEmpty()) { "Cart is empty. Add at least one item." }

            val now = System.currentTimeMillis()
            val newOrder =
                RestaurantOrder(
                    id = "order-${UUID.randomUUID()}",
                    items = cart.items,
                    totalAmountUsd = cart.totalAmountUsd,
                    placedAtEpochMillis = now,
                    estimatedReadyAtEpochMillis = now + (25 * 60 * 1000L),
                )

            ordersFlow.update { current -> listOf(newOrder) + current }
            cartFlow.value = CartState()
            return newOrder
        }

        override suspend fun placeOrderForMenuItem(itemId: String): RestaurantOrder {
            val menuItem = menuItems.firstOrNull { it.id == itemId } ?: error("Menu item not found.")
            val now = System.currentTimeMillis()
            val orderLine =
                OrderLine(
                    itemId = menuItem.id,
                    itemName = menuItem.name,
                    quantity = 1,
                    unitPriceUsd = menuItem.priceUsd,
                    imageUrl = menuItem.imageUrl,
                )
            val newOrder =
                RestaurantOrder(
                    id = "order-${UUID.randomUUID()}",
                    items = listOf(orderLine),
                    totalAmountUsd = menuItem.priceUsd,
                    placedAtEpochMillis = now,
                    estimatedReadyAtEpochMillis = now + (25 * 60 * 1000L),
                )

            ordersFlow.update { current -> listOf(newOrder) + current }
            return newOrder
        }

        override suspend fun getLatestOrder(): RestaurantOrder? = ordersFlow.value.maxByOrNull { it.placedAtEpochMillis }

        override suspend fun cancelLatestOrder(): RestaurantOrder {
            val latestOrder = getLatestOrder() ?: error("No previous order to cancel.")
            ordersFlow.update { current -> current.filterNot { it.id == latestOrder.id } }
            return latestOrder
        }

        override suspend fun repeatLatestOrder(): RestaurantOrder {
            val latest = getLatestOrder() ?: error("No previous order to repeat.")
            val now = System.currentTimeMillis()
            val repeated =
                latest.copy(
                    id = "order-${UUID.randomUUID()}",
                    placedAtEpochMillis = now,
                    estimatedReadyAtEpochMillis = now + (25 * 60 * 1000L),
                )
            ordersFlow.update { current -> listOf(repeated) + current }
            return repeated
        }

        override suspend fun getCurrentOrderStatus(orderId: String?): String {
            val now = System.currentTimeMillis()
            val currentOrders = ordersFlow.value.filter { now - it.placedAtEpochMillis < TWO_HOURS_MILLIS }
            val targetOrder =
                if (orderId.isNullOrBlank()) {
                    currentOrders.maxByOrNull { it.placedAtEpochMillis }
                } else {
                    currentOrders.firstOrNull { it.id == orderId }
                }

            return if (targetOrder == null) {
                "No active current order found."
            } else {
                val etaMinutes = ((targetOrder.estimatedReadyAtEpochMillis - now) / 60000).coerceAtLeast(0)
                "Order ${targetOrder.id} is in progress. ETA: $etaMinutes minutes. Total: $${"%.2f".format(targetOrder.totalAmountUsd)}"
            }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class RestaurantModule {
    @Binds
    abstract fun bindRestaurantRepository(
        impl: InMemoryRestaurantRepository,
    ): RestaurantRepository
}

