package com.example.androiddevops.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.androiddevops.restaurant.model.CartState
import com.example.androiddevops.restaurant.model.MenuItem
import com.example.androiddevops.restaurant.model.OrderLine
import com.example.androiddevops.restaurant.model.RestaurantOrder
import com.example.androiddevops.restaurant.presentation.RestaurantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class RestaurantTab {
    ITEMS,
    CURRENT_ORDER,
    PAST_ORDERS,
}

private const val PAST_ORDERS_LABEL = "Past Orders"
private val PriceColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantHomeScreen(viewModel: RestaurantViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(RestaurantTab.ITEMS) }

    LaunchedEffect(uiState.toastMessage) {
        val message = uiState.toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeToast()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Restaurant Demo") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == RestaurantTab.ITEMS,
                    onClick = { selectedTab = RestaurantTab.ITEMS },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Item List") },
                    label = { Text("Item List") },
                )
                NavigationBarItem(
                    selected = selectedTab == RestaurantTab.CURRENT_ORDER,
                    onClick = { selectedTab = RestaurantTab.CURRENT_ORDER },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Current Order") },
                    label = { Text("Current Order") },
                )
                NavigationBarItem(
                    selected = selectedTab == RestaurantTab.PAST_ORDERS,
                    onClick = { selectedTab = RestaurantTab.PAST_ORDERS },
                    icon = { Icon(Icons.Filled.Info, contentDescription = PAST_ORDERS_LABEL) },
                    label = { Text(PAST_ORDERS_LABEL) },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when (selectedTab) {
            RestaurantTab.ITEMS -> {
                ItemListTab(
                    modifier = Modifier.padding(innerPadding),
                    menuItems = uiState.menuItems,
                    onAddToCart = viewModel::onAddToCart,
                )
            }

            RestaurantTab.CURRENT_ORDER -> {
                CurrentOrderTab(
                    modifier = Modifier.padding(innerPadding),
                    cart = uiState.cart,
                    currentOrders = uiState.currentOrders,
                    onRemoveFromCart = viewModel::onRemoveFromCart,
                    onPlaceOrder = viewModel::onPlaceOrder,
                )
            }

            RestaurantTab.PAST_ORDERS -> {
                PastOrderTab(
                    modifier = Modifier.padding(innerPadding),
                    pastOrders = uiState.pastOrders,
                )
            }
        }
    }
}

@Composable
private fun ItemListTab(
    modifier: Modifier = Modifier,
    menuItems: List<MenuItem>,
    onAddToCart: (String) -> Unit,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(menuItems) { item ->
            MenuItemCard(
                item = item,
                onAddToCart = { onAddToCart(item.id) },
            )
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onAddToCart: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$${"%.2f".format(item.priceUsd)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = PriceColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(onClick = onAddToCart) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentOrderTab(
    modifier: Modifier = Modifier,
    cart: CartState,
    currentOrders: List<RestaurantOrder>,
    onRemoveFromCart: (String) -> Unit,
    onPlaceOrder: () -> Unit,
) {
    val cartShape = RoundedCornerShape(16.dp)
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (cart.items.isEmpty()) {
            item {
                Card(shape = cartShape) {
                    Text(
                        text = "Cart is empty. Add items from Item List.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(cart.items) { line ->
                CartLineCard(
                    line = line,
                    onRemoveFromCart = { onRemoveFromCart(line.itemId) },
                )
            }
            item {
                Card(
                    shape = cartShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Cart Total", fontWeight = FontWeight.Bold)
                        Text(formatUsd(cart.totalAmountUsd), fontWeight = FontWeight.Bold, color = PriceColor)
                    }
                }
            }
            item {
                Button(
                    onClick = onPlaceOrder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Place Order")
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Current Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (currentOrders.isEmpty()) {
            item {
                Card(shape = cartShape) {
                    Text(
                        text = "No current orders.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(currentOrders) { order ->
                OrderCard(order = order, showEta = true)
            }
        }
    }
}

@Composable
private fun PastOrderTab(
    modifier: Modifier = Modifier,
    pastOrders: List<RestaurantOrder>,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(PAST_ORDERS_LABEL, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (pastOrders.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "No past orders yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(pastOrders) { order ->
                OrderCard(order = order, showEta = false)
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: OrderLine,
    onRemoveFromCart: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = line.imageUrl,
                contentDescription = line.itemName,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = line.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Qty ${line.quantity} x ${formatUsd(line.unitPriceUsd)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Line total: ${formatUsd(line.quantity * line.unitPriceUsd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = PriceColor,
                )
            }
            FilledTonalIconButton(onClick = onRemoveFromCart) {
                Text(text = "-", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: RestaurantOrder,
    showEta: Boolean,
) {
    val minutesAgo = ((System.currentTimeMillis() - order.placedAtEpochMillis) / 60000.0).roundToInt()
    val etaMinutes = ((order.estimatedReadyAtEpochMillis - System.currentTimeMillis()) / 60000.0).roundToInt()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(order.id, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    formatUsd(order.totalAmountUsd),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PriceColor,
                )
            }
            HorizontalDivider()
            order.items.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = line.imageUrl,
                        contentDescription = line.itemName,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.itemName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${line.quantity} x ${formatUsd(line.unitPriceUsd)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = formatUsd(line.quantity * line.unitPriceUsd),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PriceColor,
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Placed", style = MaterialTheme.typography.labelMedium)
                Text(formatDateTime(order.placedAtEpochMillis), style = MaterialTheme.typography.labelMedium)
            }
            if (showEta) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status", style = MaterialTheme.typography.labelMedium)
                    val statusText = if (etaMinutes > 0) "Ready in ~$etaMinutes min" else "Ready for pickup"
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(statusText) },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    )
                }
            } else {
                Text("Placed $minutesAgo min ago", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun formatUsd(amount: Double): String = "$${"%.2f".format(amount)}"

private fun formatDateTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
