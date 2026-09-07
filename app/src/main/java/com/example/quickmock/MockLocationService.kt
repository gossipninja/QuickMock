package com.example.quickmock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    private val CHANNEL_ID = "MockLocationServiceChannel"
    private var locationManager: LocationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var activeLat: Double = 0.0
    private var activeLng: Double = 0.0
    private var isMocking = false

    private val mockRunnable = object : Runnable {
        override fun run() {
            if (isMocking) {
                pushMockLocation(activeLat, activeLng)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MOCK" -> {
                activeLat = intent.getDoubleExtra("LAT", 0.0)
                activeLng = intent.getDoubleExtra("LNG", 0.0)
                isMocking = true

                val notification = createNotification("Active: $activeLat, $activeLng")
                startForeground(1, notification)

                handler.removeCallbacks(mockRunnable)
                handler.post(mockRunnable)
            }
            "STOP_MOCK" -> {
                isMocking = false
                handler.removeCallbacks(mockRunnable)
                stopMockProvider()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun pushMockLocation(lat: Double, lng: Double) {
        try {
            val provider = LocationManager.GPS_PROVIDER
            @Suppress("DEPRECATION")
            locationManager?.addTestProvider(
                provider, false, false, false, false, true, true, true, 1, 1
            )
            locationManager?.setTestProviderEnabled(provider, true)

            val mockLocation = Location(provider).apply {
                latitude = lat
                longitude = lng
                altitude = 3.0
                time = System.currentTimeMillis()
                accuracy = 1.0f
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            locationManager?.setTestProviderLocation(provider, mockLocation)
        } catch (_: Exception) {}
    }

    private fun stopMockProvider() {
        try {
            locationManager?.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuickMock Spoofing Active")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Mock Location Channel", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}