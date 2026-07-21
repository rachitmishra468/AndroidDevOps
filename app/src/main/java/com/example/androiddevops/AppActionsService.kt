package com.example.androiddevops

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppActionsService : Service() {

    companion object {
        private const val CHANNEL_ID = "app_actions_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val mockAPIClient = MockAPIClient()
    private val adkAgentHelper = ADKAgentHelper(mockAPIClient)

    private lateinit var appFunctionManager: AppFunctionManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        appFunctionManager = AppFunctionManager(adkAgentHelper)

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        Log.d("AppActionsService", "Service Created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val itemName = intent?.getStringExtra("itemName") ?: "Big Mac"

        Log.d(
            "AppActionsService",
            "Received order command for: $itemName"
        )

        serviceScope.launch {
            try {

                val resultJson =
                    appFunctionManager.orderItemByName(
                        context = createMockContext(),
                        itemName = itemName
                    )

                Log.d(
                    "AppActionsService",
                    "Result: $resultJson"
                )

            } catch (e: Exception) {

                Log.e(
                    "AppActionsService",
                    "Error processing order",
                    e
                )

            } finally {

                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createMockContext(): AppFunctionContext {

        return object : AppFunctionContext {
            override val context
                get() = this@AppActionsService
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("App Action Running")
            .setContentText("Processing Assistant request...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Actions",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AppActionsService", "Service Destroyed")
    }
}