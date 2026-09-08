package com.example.quickmock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    private var isMocking = false
    private var mockThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_MOCK") {
            stopMocking()
            stopSelf()
            return START_NOT_STICKY
        }

        val lat = intent?.getDoubleExtra("LAT", 0.0) ?: 0.0
        val lng = intent?.getDoubleExtra("LNG", 0.0) ?: 0.0
        val name = intent?.getStringExtra("NAME") ?: "Mock Location"
        val slot = intent?.getIntExtra("SLOT", -1) ?: -1

        startForegroundServiceNotification(name)
        startMocking(lat, lng)

        notifyWidgetState(slot, true)

        return START_STICKY
    }

    private fun startForegroundServiceNotification(name: String) {
        val channelId = "quickmock_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "QuickMock Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = "STOP_MOCK"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("QuickMock Active")
            .setContentText("Mocking location: $name")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Mocking", stopPendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startMocking(lat: Double, lng: Double) {
        isMocking = false
        mockThread?.interrupt()

        isMocking = true
        mockThread = Thread {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = LocationManager.GPS_PROVIDER

            try {
                @Suppress("DEPRECATION")
                lm.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
                lm.setTestProviderEnabled(provider, true)
            } catch (_: Exception) {}

            while (isMocking) {
                try {
                    val loc = Location(provider).apply {
                        latitude = lat
                        longitude = lng
                        altitude = 3.0
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        accuracy = 1.0f
                    }
                    lm.setTestProviderLocation(provider, loc)
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                } catch (_: Exception) {}
            }
        }
        mockThread?.start()
    }

    private fun stopMocking() {
        isMocking = false
        mockThread?.interrupt()
        notifyWidgetState(-1, false)
    }

    private fun notifyWidgetState(slot: Int, mocking: Boolean) {
        val updateIntent = Intent(this, MockWidgetProvider::class.java).apply {
            action = "UPDATE_WIDGET_STATE"
            putExtra("ACTIVE_SLOT", slot)
            putExtra("IS_MOCKING", mocking)
        }
        sendBroadcast(updateIntent)
    }

    override fun onDestroy() {
        stopMocking()
        super.onDestroy()
    }
}