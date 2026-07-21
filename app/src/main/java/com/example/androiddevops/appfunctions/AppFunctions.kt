
package com.example.androiddevops.appfunctions

import android.util.Log
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import com.example.androiddevops.restaurant.domain.RestaurantRepository
import kotlinx.coroutines.flow.firstOrNull
import com.example.androiddevops.restaurant.model.MenuItem
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Provides AppFunctions for a restaurant ordering demo app. */
class AppFunctions
    @Inject
    constructor(
        private val restaurantRepository: RestaurantRepository,
    ) {
        private val demoStores =
            listOf(
                DemoStore("McDonald's MG Road", "MG Road", 12.9756, 77.6050),
                DemoStore("McDonald's Koramangala", "Koramangala", 12.9352, 77.6245),
                DemoStore("McDonald's Indiranagar", "Indiranagar", 12.9784, 77.6408),
            )

        /** Returns the latest order from repository history. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun findLastOrder(_appFunctionContext: AppFunctionContext): LastOrderResult {
            Log.d(TAG, "findLastOrder called")
            val latestOrder =
                restaurantRepository.getLatestOrder()
                    ?: throw AppFunctionElementNotFoundException("No previous order found.")
            Log.d(TAG, "findLastOrder: Found order ${latestOrder.id}")
            return LastOrderResult(
                orderId = latestOrder.id,
                itemName = latestOrder.items.joinToString { "${it.itemName} x${it.quantity}" },
                totalAmountUsd = latestOrder.totalAmountUsd,
                storeName = "McDonald's",
                orderedAtEpochMillis = latestOrder.placedAtEpochMillis,
            )
        }

        /** Cancels the most recent order and returns a confirmation. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun cancelLastOrder(_appFunctionContext: AppFunctionContext): CancelLastOrderResult {
            Log.d(TAG, "cancelLastOrder called")
            val latestOrder =
                restaurantRepository.getLatestOrder()
                    ?: throw AppFunctionElementNotFoundException("No previous order found to cancel.")
            restaurantRepository.cancelLatestOrder()
            Log.d(TAG, "cancelLastOrder: Cancelled order ${latestOrder.id}")
            return CancelLastOrderResult(
                cancelledOrderId = latestOrder.id,
                cancelledItemName = latestOrder.items.joinToString { "${it.itemName} x${it.quantity}" },
                refundedAmountUsd = latestOrder.totalAmountUsd,
                confirmationMessage = "Cancelled your last order successfully.",
            )
        }

        /** Places the same order as the most recent one and returns a confirmation. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun repeatLastOrder(_appFunctionContext: AppFunctionContext): RepeatLastOrderResult {
            Log.d(TAG, "repeatLastOrder called")
            val repeatedOrder = restaurantRepository.repeatLatestOrder()
            Log.d(TAG, "repeatLastOrder: Repeated order. New ID: ${repeatedOrder.id}")
            return RepeatLastOrderResult(
                newOrderId = repeatedOrder.id,
                repeatedItemName = repeatedOrder.items.joinToString { "${it.itemName} x${it.quantity}" },
                totalAmountUsd = repeatedOrder.totalAmountUsd,
                confirmationMessage = "Repeated your last order successfully.",
            )
        }

        /** Finds the nearest McDonald's store based on user coordinates. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun findNearestMcdStore(
            _appFunctionContext: AppFunctionContext,
            userLatitude: Double,
            userLongitude: Double,
        ): NearestStoreResult {
            Log.d(TAG, "findNearestMcdStore called with userLocation: ($userLatitude, $userLongitude)")
            if (demoStores.isEmpty()) {
                throw AppFunctionElementNotFoundException("No McDonald's stores configured.")
            }

            val nearestStore =
                demoStores
                    .map {
                        val distanceKm =
                            haversineDistanceKm(
                                userLatitude,
                                userLongitude,
                                it.latitude,
                                it.longitude,
                            )
                        it to distanceKm
                    }
                    .minByOrNull { it.second }
                    ?: throw AppFunctionElementNotFoundException("Unable to calculate nearest store.")

            return NearestStoreResult(
                storeName = nearestStore.first.storeName,
                area = nearestStore.first.area,
                distanceKm = nearestStore.second,
            )
        }

        /** Returns the menu item with the lowest price, optionally within a category. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun findLowestPriceItem(
            _appFunctionContext: AppFunctionContext,
            category: String? = null,
        ): LowestPriceItemResult {
            Log.d(TAG, "findLowestPriceItem called with category: $category")
            val lowest = findLowestMenuItem(category)
            Log.d(TAG, "findLowestPriceItem: Found ${lowest.name} in category ${lowest.category}")
            return LowestPriceItemResult(
                itemName = lowest.name,
                category = lowest.category,
                priceUsd = lowest.priceUsd,
            )
        }

        /** Places an order for a menu item by name. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun orderItemByName(
            _appFunctionContext: AppFunctionContext,
            itemName: String,
        ): PlaceOrderResult {
            Log.d(TAG, "orderItemByName called with itemName: $itemName")
            val selectedItem = findMenuItemByName(itemName)
            val placedOrder = restaurantRepository.placeOrderForMenuItem(selectedItem.id)
            Log.d(TAG, "orderItemByName: Placed order ${placedOrder.id} for ${selectedItem.name}")
            val etaMinutes = ((placedOrder.estimatedReadyAtEpochMillis - placedOrder.placedAtEpochMillis) / 60000).coerceAtLeast(0)
            return PlaceOrderResult(
                orderId = placedOrder.id,
                itemName = selectedItem.name,
                totalAmountUsd = placedOrder.totalAmountUsd,
                etaMinutes = etaMinutes,
                confirmationMessage = "Ordered ${selectedItem.name} successfully.",
            )
        }

        /** Finds the lowest priced item and places an order for it in one step. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun orderLowestPriceItem(
            _appFunctionContext: AppFunctionContext,
            category: String? = null,
        ): PlaceOrderResult {
            Log.d(TAG, "orderLowestPriceItem called with category: $category")
            val lowest = findLowestMenuItem(category)
            val placedOrder = restaurantRepository.placeOrderForMenuItem(lowest.id)
            Log.d(TAG, "orderLowestPriceItem: Placed order ${placedOrder.id} for ${lowest.name}")
            val etaMinutes = ((placedOrder.estimatedReadyAtEpochMillis - placedOrder.placedAtEpochMillis) / 60000).coerceAtLeast(0)
            return PlaceOrderResult(
                orderId = placedOrder.id,
                itemName = lowest.name,
                totalAmountUsd = placedOrder.totalAmountUsd,
                etaMinutes = etaMinutes,
                confirmationMessage = "Ordered lowest-price item ${lowest.name} successfully.",
            )
        }

        /** Returns status text for the active order, or a specific current order ID. */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun getCurrentOrderStatus(
            _appFunctionContext: AppFunctionContext,
            orderId: String? = null,
        ): CurrentOrderStatusResult {
            Log.d(TAG, "getCurrentOrderStatus called with orderId: $orderId")
            return CurrentOrderStatusResult(
                status = restaurantRepository.getCurrentOrderStatus(orderId),
            )
        }

        private suspend fun findLowestMenuItem(category: String?): MenuItem {
            val menuItems = restaurantRepository.observeMenuItems().firstOrNull().orEmpty()
            val eligibleItems =
                if (category.isNullOrBlank()) {
                    menuItems
                } else {
                    menuItems.filter { it.category.equals(category.trim(), ignoreCase = true) }
                }

            return eligibleItems.minByOrNull { it.priceUsd }
                ?: throw AppFunctionElementNotFoundException(
                    "No menu item found${if (category.isNullOrBlank()) "" else " for category: $category"}.",
                )
        }

        private suspend fun findMenuItemByName(itemName: String): MenuItem {
            val normalizedName = itemName.trim()
            if (normalizedName.isEmpty()) {
                throw AppFunctionInvalidArgumentException("Item name cannot be blank.")
            }

            val menuItems = restaurantRepository.observeMenuItems().firstOrNull().orEmpty()
            val exactMatches = menuItems.filter { it.name.equals(normalizedName, ignoreCase = true) }
            if (exactMatches.size == 1) {
                return exactMatches.first()
            }
            if (exactMatches.size > 1) {
                throw AppFunctionInvalidArgumentException("Multiple items matched '$itemName'. Please provide exact item name.")
            }

            val partialMatches = menuItems.filter { it.name.contains(normalizedName, ignoreCase = true) }
            if (partialMatches.size == 1) {
                return partialMatches.first()
            }
            if (partialMatches.size > 1) {
                throw AppFunctionInvalidArgumentException("Multiple items matched '$itemName'. Please be more specific.")
            }

            throw AppFunctionElementNotFoundException("No menu item found with name '$itemName'.")
        }

        private fun haversineDistanceKm(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val earthRadiusKm = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val originLat = Math.toRadians(lat1)
            val destinationLat = Math.toRadians(lat2)

            val a =
                sin(dLat / 2).pow(2) +
                    cos(originLat) * cos(destinationLat) * sin(dLon / 2).pow(2)
            val c = 2 * asin(sqrt(a))
            return earthRadiusKm * c
        }

        companion object {
            private const val TAG = "AppFunctions"
        }


        private data class DemoStore(
            val storeName: String,
            val area: String,
            val latitude: Double,
            val longitude: Double,
        )

        /** Represents details of the latest order in history. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class LastOrderResult(
            val orderId: String,
            val itemName: String,
            val totalAmountUsd: Double,
            val storeName: String,
            val orderedAtEpochMillis: Long,
        )

        /** Represents the response after repeating the last order. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class RepeatLastOrderResult(
            val newOrderId: String,
            val repeatedItemName: String,
            val totalAmountUsd: Double,
            val confirmationMessage: String,
        )

        /** Represents the response after cancelling the last order. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class CancelLastOrderResult(
            val cancelledOrderId: String,
            val cancelledItemName: String,
            val refundedAmountUsd: Double,
            val confirmationMessage: String,
        )

        /** Represents the nearest McDonald's store. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class NearestStoreResult(
            val storeName: String,
            val area: String,
            val distanceKm: Double,
        )

        /** Represents the cheapest menu item from a list of demo items. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class LowestPriceItemResult(
            val itemName: String,
            val category: String,
            val priceUsd: Double,
        )

        /** Represents response after placing an order for a menu item. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class PlaceOrderResult(
            val orderId: String,
            val itemName: String,
            val totalAmountUsd: Double,
            val etaMinutes: Long,
            val confirmationMessage: String,
        )

        /** Represents the live status of a current order. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class CurrentOrderStatusResult(
            val status: String,
        )

        // Kept for compatibility with older chat-related classes that are still in the project.
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class ContactSearchResult(
            val endpointValue: String,
            val endpointType: String,
            val displayName: String,
        )

        // Kept for compatibility with older chat-related classes that are still in the project.
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class Result(
            val messageId: String,
            val message: String,
        )

        // Kept for compatibility with older chat-related classes that are still in the project.
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class Recipient(
            val id: String,
            val name: String,
            val email: String,
        )

        // Kept for compatibility with older chat-related classes that are still in the project.
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class ChatGroup(
            val id: String,
            val name: String,
            val recipients: List<Recipient>,
        )
    }
