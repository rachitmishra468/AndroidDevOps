package com.example.androiddevops

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class AppFunctionManager(private val adkAgentHelper: ADKAgentHelper) {

    /**
     * Orders a menu item by its name.
     * 
     * @param context The AppFunction context.
     * @param itemName The name of the item to order (e.g., "Big Mac").
     * @return A JSON string in A2UI format representing the order confirmation.
     */

    suspend fun orderItemByName(context: AppFunctionContext, itemName: String): String {
        // Logic processing via ADKAgentHelper
        return adkAgentHelper.processOrder(itemName)
    }
}
