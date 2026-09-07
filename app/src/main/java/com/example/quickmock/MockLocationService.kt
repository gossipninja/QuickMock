package com.example.quickmock

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import java.util.Timer
import java.util.TimerTask

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var timer: Timer? = null
    private val providerName = LocationManager.GPS_PROVIDER

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MOCK" -> {
                val lat = intent.getDoubleExtra("LAT", 0.0)
                val lng = intent.getDoubleExtra("LNG", 0.0)
                startMocking(lat, lng)
            }
            "STOP_MOCK" -> stopMocking()
        }
        return START_STICKY
    }

    private fun startMocking(lat: Double, lng: Double) {
        timer?.cancel()
        try {
            try { locationManager.removeTestProvider(providerName) } catch (_: Exception) {}
            
            locationManager.addTestProvider(
                providerName, false, false, false, false, 
                true, true, true, 1, 1
            )
            locationManager.setTestProviderEnabled(providerName, true)

            timer = Timer().apply {
                scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        val mockLocation = Location(providerName).apply {
                            latitude = lat
                            longitude = lng
                            altitude = 10.0
                            accuracy = 1.0f
                            time = System.currentTimeMillis()
                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        }
                        locationManager.setTestProviderLocation(providerName, mockLocation)
                    }
                }, 0, 1000)
            }
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopMocking() {
        timer?.cancel()
        try {
            locationManager.removeTestProvider(providerName)
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "mock_location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Mock GPS Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Mock GPS Active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
