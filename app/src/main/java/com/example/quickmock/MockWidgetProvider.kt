package com.example.quickmock

import android.app.AppOpsManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast

class MockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        when (action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    prefs.edit().putBoolean("is_mocking", false).apply()
                }
                updateAllWidgets(context)
            }
            "com.example.quickmock.ACTION_SELECT_SLOT" -> {
                val slot = intent.getIntExtra("slot_number", 1)
                prefs.edit().putInt("active_slot", slot).apply()

                if (!isMockLocationEnabled(context)) {
                    showDevOptionsPrompt(context)
                } else if (prefs.getBoolean("is_mocking", false)) {
                    applyMockLocation(context, slot)
                }
                updateAllWidgets(context)
            }
            "com.example.quickmock.ACTION_TOGGLE_MOCK" -> {
                if (!isMockLocationEnabled(context)) {
                    showDevOptionsPrompt(context)
                    return
                }

                val currentState = prefs.getBoolean("is_mocking", false)
                val newState = !currentState
                prefs.edit().putBoolean("is_mocking", newState).apply()

                val activeSlot = prefs.getInt("active_slot", 1)
                if (newState) {
                    applyMockLocation(context, activeSlot)
                } else {
                    stopMockLocation(context)
                }
                updateAllWidgets(context)
            }
        }
    }

    private fun showDevOptionsPrompt(context: Context) {
        Toast.makeText(context, "QuickMock is not set as the Mock Location App in Developer Options", Toast.LENGTH_LONG).show()
        val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(devIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open Developer Options directly", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyMockLocation(context: Context, slot: Int) {
        val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat_$slot", 0f).toDouble()
        val lng = prefs.getFloat("lng_$slot", 0f).toDouble()

        if (lat == 0.0 && lng == 0.0) return

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = LocationManager.GPS_PROVIDER

        try {
            @Suppress("DEPRECATION")
            lm.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
            lm.setTestProviderEnabled(provider, true)

            val mockLocation = Location(provider).apply {
                latitude = lat
                longitude = lng
                altitude = 3.0
                time = System.currentTimeMillis()
                accuracy = 1.0f
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            lm.setTestProviderLocation(provider, mockLocation)
        } catch (e: SecurityException) {
            prefs.edit().putBoolean("is_mocking", false).apply()
            showDevOptionsPrompt(context)
        } catch (e: Exception) {
            // Provider already active
        }
    }

    private fun stopMockLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            // Ignore if already removed
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun isMockLocationEnabled(context: Context): Boolean {
            return try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    ) == AppOpsManager.MODE_ALLOWED
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    ) == AppOpsManager.MODE_ALLOWED
                }
            } catch (e: Exception) {
                false
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MockWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_mock)
            val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

            val activeSlot = prefs.getInt("active_slot", 1)
            val isMocking = prefs.getBoolean("is_mocking", false)
            val isMockAllowed = isMockLocationEnabled(context)

            val slotButtons = arrayOf(
                R.id.btn_slot_1, R.id.btn_slot_2, R.id.btn_slot_3,
                R.id.btn_slot_4, R.id.btn_slot_5, R.id.btn_slot_6,
                R.id.btn_slot_7, R.id.btn_slot_8, R.id.btn_slot_9
            )

            for (i in slotButtons.indices) {
                val slotNum = i + 1
                val btnId = slotButtons[i]

                val lat = prefs.getFloat("lat_$slotNum", 0f)
                val lng = prefs.getFloat("lng_$slotNum", 0f)
                val name = prefs.getString("name_$slotNum", "") ?: ""
                val isSet = (lat != 0f || lng != 0f)

                if (isSet) {
                    views.setViewVisibility(btnId, View.VISIBLE)
                    val label = if (name.isNotEmpty()) name else "Slot $slotNum"
                    views.setTextViewText(btnId, label)

                    if (slotNum == activeSlot) {
                        views.setInt(btnId, "setBackgroundColor", Color.parseColor("#1976D2"))
                        views.setTextColor(btnId, Color.WHITE)
                    } else {
                        views.setInt(btnId, "setBackgroundColor", Color.parseColor("#424242"))
                        views.setTextColor(btnId, Color.WHITE)
                    }

                    val slotIntent = Intent(context, MockWidgetProvider::class.java).apply {
                        action = "com.example.quickmock.ACTION_SELECT_SLOT"
                        putExtra("slot_number", slotNum)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, slotNum, slotIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(btnId, pi)
                } else {
                    views.setViewVisibility(btnId, View.GONE)
                }
            }

            // Play / Pause / Warning Controls
            val activeLat = prefs.getFloat("lat_$activeSlot", 0f)
            val activeLng = prefs.getFloat("lng_$activeSlot", 0f)
            val activeHasLocation = (activeLat != 0f || activeLng != 0f)

            if (!isMockAllowed) {
                views.setTextViewText(R.id.btn_toggle_mock, "⚠️ SET MOCK APP")
                views.setBoolean(R.id.btn_toggle_mock, "setEnabled", true)
                views.setTextColor(R.id.btn_toggle_mock, Color.WHITE)
                views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#D32F2F"))
            } else if (activeHasLocation) {
                views.setBoolean(R.id.btn_toggle_mock, "setEnabled", true)
                views.setTextColor(R.id.btn_toggle_mock, Color.WHITE)
                if (isMocking) {
                    views.setTextViewText(R.id.btn_toggle_mock, "⏸ STOP")
                    views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#C62828"))
                } else {
                    views.setTextViewText(R.id.btn_toggle_mock, "▶ START")
                    views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#388E3C"))
                }
            } else {
                views.setTextViewText(R.id.btn_toggle_mock, "▶ NO LOC")
                views.setBoolean(R.id.btn_toggle_mock, "setEnabled", false)
                views.setTextColor(R.id.btn_toggle_mock, Color.parseColor("#9E9E9E"))
                views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#616161"))
            }

            val toggleIntent = Intent(context, MockWidgetProvider::class.java).apply {
                action = "com.example.quickmock.ACTION_TOGGLE_MOCK"
            }
            val togglePi = PendingIntent.getBroadcast(
                context, 100, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_toggle_mock, togglePi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}