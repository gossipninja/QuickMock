package com.example.quickmock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MockService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        if (action == "ACTION_STOP_MOCK") {
            prefs.edit().putBoolean("is_mocking", false).apply()
            stopMocking()
            MockWidgetProvider.updateAllWidgets(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val stopIntent = Intent(this, MockService::class.java).apply {
            this.action = "ACTION_STOP_MOCK"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "mock_channel")
            .setContentTitle("QuickMock Active")
            .setContentText("Mock location is currently active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_media_pause, "Stop Mocking", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun stopMocking() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        try {
            lm.removeTestProvider(android.location.LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            // Ignored if already removed
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "mock_channel",
                "Mock Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}