package com.example.androiddevops.restaurant.domain

import com.example.androiddevops.restaurant.model.CartState
import com.example.androiddevops.restaurant.model.MenuItem
import com.example.androiddevops.restaurant.model.RestaurantOrder
import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {
    fun observeMenuItems(): Flow<List<MenuItem>>

    fun observeCartState(): Flow<CartState>

    fun observeOrders(): Flow<List<RestaurantOrder>>

    suspend fun addItemToCart(itemId: String)

    suspend fun removeItemFromCart(itemId: String)

    suspend fun placeCurrentCartOrder(): RestaurantOrder

    suspend fun placeOrderForMenuItem(itemId: String): RestaurantOrder

    suspend fun getLatestOrder(): RestaurantOrder?

    suspend fun cancelLatestOrder(): RestaurantOrder

    suspend fun repeatLatestOrder(): RestaurantOrder

    suspend fun getCurrentOrderStatus(orderId: String? = null): String
}
