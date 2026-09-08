package com.example.quickmock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import android.widget.RemoteViews

class MockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            
            if (!isGpsEnabled) {
                val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_mocking", false).apply()
            }
            updateAllWidgets(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
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

            val lat = prefs.getFloat("lat_$activeSlot", 0f)
            val lng = prefs.getFloat("lng_$activeSlot", 0f)
            val hasValidLocation = (lat != 0f || lng != 0f)

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsSystemOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val effectiveMocking = isMocking && isGpsSystemOn

            val slotButtons = arrayOf(
                R.id.btn_slot_1, R.id.btn_slot_2, R.id.btn_slot_3,
                R.id.btn_slot_4, R.id.btn_slot_5, R.id.btn_slot_6,
                R.id.btn_slot_7, R.id.btn_slot_8, R.id.btn_slot_9
            )

            // Material Design 3 Tone colors for selection state
            val selectedColor = Color.parseColor("#388E3C") // Material Green 700 container tint
            val defaultColor = Color.parseColor("#424242")  // Material Dark Surface tint

            for (i in slotButtons.indices) {
                val slotNum = i + 1
                val btnId = slotButtons[i]
                if (slotNum == activeSlot) {
                    views.setInt(btnId, "setBackgroundColor", selectedColor)
                } else {
                    views.setInt(btnId, "setBackgroundColor", defaultColor)
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
            }

            // Play / Pause Button Handling
            if (hasValidLocation) {
                views.setBoolean(R.id.btn_toggle_mock, "setEnabled", true)
                if (effectiveMocking) {
                    views.setTextViewText(R.id.btn_toggle_mock, "⏸")
                    views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#C62828")) // Material Red 800
                } else {
                    views.setTextViewText(R.id.btn_toggle_mock, "▶")
                    views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#2E7D32")) // Material Green 800
                }

                val toggleIntent = Intent(context, MockWidgetProvider::class.java).apply {
                    action = "com.example.quickmock.ACTION_TOGGLE_MOCK"
                }
                val togglePi = PendingIntent.getBroadcast(
                    context, 100, toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_toggle_mock, togglePi)
            } else {
                // Grey out and disable if no coordinates set for selected slot
                views.setTextViewText(R.id.btn_toggle_mock, "▶")
                views.setBoolean(R.id.btn_toggle_mock, "setEnabled", false)
                views.setInt(R.id.btn_toggle_mock, "setBackgroundColor", Color.parseColor("#616161")) // Material Grey 700 (Disabled)
                views.setOnClickPendingIntent(R.id.btn_toggle_mock, null)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}