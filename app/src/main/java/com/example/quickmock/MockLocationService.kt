package com.example.quickmock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    private val CHANNEL_ID = "MockLocationServiceChannel"
    private var locationManager: LocationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var activeLat: Double = 0.0
    private var activeLng: Double = 0.0
    private var activeName: String = ""
    private var activeSlot: Int = -1
    private var isMocking = false

    private val mockRunnable = object : Runnable {
        override fun run() {
            if (isMocking) {
                val success = pushMockLocation(activeLat, activeLng)
                if (success) {
                    handler.postDelayed(this, 1000)
                }
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
                activeName = intent.getStringExtra("NAME") ?: "Custom Spot"
                activeSlot = intent.getIntExtra("SLOT", -1)
                isMocking = true

                val notification = createNotification("Mocking: $activeName ($activeLat, $activeLng)")
                startForeground(1, notification)

                handler.removeCallbacks(mockRunnable)
                handler.post(mockRunnable)

                notifyWidgetState(activeSlot, true)
                Toast.makeText(this, "Now mocking $activeName", Toast.LENGTH_SHORT).show()
            }
            "STOP_MOCK" -> {
                isMocking = false
                handler.removeCallbacks(mockRunnable)
                stopMockProvider()
                notifyWidgetState(-1, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                Toast.makeText(this, "Mocking stopped", Toast.LENGTH_SHORT).show()
            }
        }
        return START_NOT_STICKY
    }

    private fun pushMockLocation(lat: Double, lng: Double): Boolean {
        return try {
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
            true
        } catch (e: SecurityException) {
            isMocking = false
            handler.post {
                Toast.makeText(applicationContext, "Error: Set QuickMock as Mock Location App in Developer Settings", Toast.LENGTH_LONG).show()
            }
            notifyWidgetState(-1, false)
            stopSelf()
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun stopMockProvider() {
        try {
            locationManager?.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
    }

    private fun notifyWidgetState(slot: Int, mocking: Boolean) {
        val intent = Intent(this, MockWidgetProvider::class.java).apply {
            action = "UPDATE_WIDGET_STATE"
            putExtra("ACTIVE_SLOT", slot)
            putExtra("IS_MOCKING", mocking)
        }
        sendBroadcast(intent)
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = Intent(this, MockLocationService::class.java).apply { action = "STOP_MOCK" }
        val stopPendingIntent = PendingIntent.getService(
            this, 101, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuickMock Active")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_compass, "Stop Mocking", stopPendingIntent)
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