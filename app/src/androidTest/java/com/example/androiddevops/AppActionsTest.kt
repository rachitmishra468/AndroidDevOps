package com.example.androiddevops

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.assistant.appactions.testing.aatl.AppActionsTestManager
import com.google.assistant.appactions.testing.aatl.fulfillment.AppActionsFulfillmentIntentResult
import com.google.assistant.appactions.testing.aatl.fulfillment.FulfillmentType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppActionsTest {

    private lateinit var aatl: AppActionsTestManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        aatl = AppActionsTestManager(context)
    }

    @Test
    fun testOrderMenuItemFulfillment() {
        val intentName = "actions.intent.ORDER_MENU_ITEM"
        val intentParams = mapOf("menuItem.name" to "Big Mac")

        val result = aatl.fulfill(intentName, intentParams)

        assertEquals(FulfillmentType.INTENT, result.getFulfillmentType())

        val intentResult = result as AppActionsFulfillmentIntentResult
        val intent = intentResult.intent

        assertEquals("android.intent.action.VIEW", intent.action)
        assertEquals("com.example.androiddevops", intent.`package`)
        assertEquals("com.example.androiddevops.AppActionsService", intent.component?.className)
        assertEquals("Big Mac", intent.getStringExtra("itemName"))
    }
}
