package com.example.androiddevops.restaurant.domain.usecase

import com.example.androiddevops.restaurant.domain.RestaurantRepository
import com.example.androiddevops.restaurant.model.RestaurantOrder
import javax.inject.Inject

class AddItemToCartUseCase
    @Inject
    constructor(
        private val repository: RestaurantRepository,
    ) {
        suspend operator fun invoke(itemId: String) = repository.addItemToCart(itemId)
    }

class RemoveItemFromCartUseCase
    @Inject
    constructor(
        private val repository: RestaurantRepository,
    ) {
        suspend operator fun invoke(itemId: String) = repository.removeItemFromCart(itemId)
    }

class PlaceOrderUseCase
    @Inject
    constructor(
        private val repository: RestaurantRepository,
    ) {
        suspend operator fun invoke(): RestaurantOrder = repository.placeCurrentCartOrder()
    }

