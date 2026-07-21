package com.example.androiddevops

import org.json.JSONObject

class ADKAgentHelper(private val mockAPIClient: MockAPIClient) {

    fun processOrder(itemName: String): String {
        val response = mockAPIClient.placeOrder(itemName)
        return generateA2UIJson(response)
    }

    private fun generateA2UIJson(order: OrderResponse): String {
        val json = JSONObject()
        json.put("status", "✓ Order Confirmed!")
        json.put("orderId", "Order #${order.orderId}")
        json.put("total", "Total: $${String.format("%.2f", order.total)}")
        json.put("eta", "ETA: ${order.etaMinutes} minutes")
        
        // This is a simplified A2UI-like structure
        val a2ui = JSONObject()
        a2ui.put("template", "order_confirmation")
        a2ui.put("data", json)
        
        return a2ui.toString()
    }
}
