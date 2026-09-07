package com.example.quickmock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    private val CHANNEL_ID = "MockLocationServiceChannel"
    private var locationManager: LocationManager? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "START_MOCK") {
            val lat = intent.getDoubleExtra("LAT", 0.0)
            val lng = intent.getDoubleExtra("LNG", 0.0)

            val notification = createNotification("Mocking Location: $lat, $lng")
            startForeground(1, notification)

            setMockLocation(lat, lng)
        } else if (action == "STOP_MOCK") {
            stopMockLocation()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun setMockLocation(lat: Double, lng: Double) {
        try {
            val provider = LocationManager.GPS_PROVIDER
            locationManager?.addTestProvider(
                provider,
                false, false, false, false, true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMockLocation() {
        try {
            val provider = LocationManager.GPS_PROVIDER
            locationManager?.removeTestProvider(provider)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuickMock Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mock Location Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}