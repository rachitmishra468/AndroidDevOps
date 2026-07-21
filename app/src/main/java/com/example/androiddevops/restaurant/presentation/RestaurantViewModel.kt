package com.example.androiddevops.restaurant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androiddevops.restaurant.domain.RestaurantRepository
import com.example.androiddevops.restaurant.model.CartState
import com.example.androiddevops.restaurant.model.MenuItem
import com.example.androiddevops.restaurant.model.RestaurantOrder
import com.example.androiddevops.restaurant.domain.usecase.AddItemToCartUseCase
import com.example.androiddevops.restaurant.domain.usecase.PlaceOrderUseCase
import com.example.androiddevops.restaurant.domain.usecase.RemoveItemFromCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TWO_HOURS_MILLIS = 60 * 60 * 1000L

data class RestaurantUiState(
    val menuItems: List<MenuItem> = emptyList(),
    val cart: CartState = CartState(),
    val currentOrders: List<RestaurantOrder> = emptyList(),
    val pastOrders: List<RestaurantOrder> = emptyList(),
    val toastMessage: String? = null,
)

@HiltViewModel
class RestaurantViewModel
    @Inject
    constructor(
        repository: RestaurantRepository,
        private val addItemToCart: AddItemToCartUseCase,
        private val removeItemFromCart: RemoveItemFromCartUseCase,
        private val placeOrder: PlaceOrderUseCase,
    ) : ViewModel() {
        private val ticker = MutableStateFlow(System.currentTimeMillis())
        private val messageFlow = MutableStateFlow<String?>(null)

        val uiState: StateFlow<RestaurantUiState> =
            combine(
                repository.observeMenuItems(),
                repository.observeCartState(),
                repository.observeOrders(),
                ticker,
                messageFlow,
            ) { menuItems, cart, orders, now, message ->
                val currentOrders = orders.filter { now - it.placedAtEpochMillis < TWO_HOURS_MILLIS }
                val pastOrders = orders.filter { now - it.placedAtEpochMillis >= TWO_HOURS_MILLIS }
                RestaurantUiState(
                    menuItems = menuItems,
                    cart = cart,
                    currentOrders = currentOrders,
                    pastOrders = pastOrders,
                    toastMessage = message,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = RestaurantUiState(),
            )

        init {
            viewModelScope.launch {
                while (true) {
                    delay(60_000)
                    ticker.value = System.currentTimeMillis()
                }
            }
        }

        fun onAddToCart(itemId: String) {
            viewModelScope.launch {
                addItemToCart(itemId)
                messageFlow.value = "Item added to cart"
            }
        }

        fun onRemoveFromCart(itemId: String) {
            viewModelScope.launch {
                removeItemFromCart(itemId)
            }
        }

        fun onPlaceOrder() {
            viewModelScope.launch {
                runCatching { placeOrder() }
                    .onSuccess { order ->
                        messageFlow.value = "Order placed: ${order.id}"
                        ticker.value = System.currentTimeMillis()
                    }
                    .onFailure {
                        messageFlow.value = it.message ?: "Unable to place order"
                    }
            }
        }

        fun consumeToast() {
            messageFlow.update { null }
        }
    }

