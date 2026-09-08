package com.example.quickmock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class MockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, activeSlot = -1, isMocking = false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MockWidgetProvider::class.java)) ?: intArrayOf()

        when (intent.action) {
            "ACTION_SLOT_CLICK" -> {
                val slot = intent.getIntExtra("SLOT", 1)
                val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
                val name = prefs.getString("name_$slot", "Slot $slot") ?: "Slot $slot"
                val lat = prefs.getFloat("lat_$slot", 0f).toDouble()
                val lng = prefs.getFloat("lng_$slot", 0f).toDouble()

                val serviceIntent = Intent(context, MockLocationService::class.java).apply {
                    action = "START_MOCK"
                    putExtra("LAT", lat)
                    putExtra("LNG", lng)
                    putExtra("NAME", name)
                    putExtra("SLOT", slot)
                }
                context.startForegroundService(serviceIntent)

                for (id in ids) {
                    updateWidget(context, appWidgetManager, id, activeSlot = slot, isMocking = true)
                }
            }
            "ACTION_TOGGLE_CLICK" -> {
                val serviceIntent = Intent(context, MockLocationService::class.java).apply {
                    action = "STOP_MOCK"
                }
                context.startService(serviceIntent)

                for (id in ids) {
                    updateWidget(context, appWidgetManager, id, activeSlot = -1, isMocking = false)
                }
            }
            "UPDATE_WIDGET_STATE" -> {
                val activeSlot = intent.getIntExtra("ACTIVE_SLOT", -1)
                val isMocking = intent.getBooleanExtra("IS_MOCKING", false)
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id, activeSlot, isMocking)
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, activeSlot: Int, isMocking: Boolean) {
        val views = RemoteViews(context.packageName, R.layout.widget_mock)
        val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        val slotButtons = intArrayOf(
            R.id.btn_slot_1, R.id.btn_slot_2, R.id.btn_slot_3,
            R.id.btn_slot_4, R.id.btn_slot_5, R.id.btn_slot_6,
            R.id.btn_slot_7, R.id.btn_slot_8, R.id.btn_slot_9
        )

        for (i in 0 until 9) {
            val slotIdx = i + 1
            val name = prefs.getString("name_$slotIdx", "Slot $slotIdx")
            views.setTextViewText(slotButtons[i], if (name.isNullOrEmpty()) "Slot $slotIdx" else name)

            val clickIntent = Intent(context, MockWidgetProvider::class.java).apply {
                action = "ACTION_SLOT_CLICK"
                putExtra("SLOT", slotIdx)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, slotIdx, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(slotButtons[i], pendingIntent)

            if (slotIdx == activeSlot && isMocking) {
                views.setInt(slotButtons[i], "setBackgroundColor", Color.parseColor("#4CAF50"))
                views.setTextColor(slotButtons[i], Color.WHITE)
            } else {
                views.setInt(slotButtons[i], "setBackgroundColor", Color.parseColor("#333333"))
                views.setTextColor(slotButtons[i], Color.WHITE)
            }
        }

        views.setTextViewText(R.id.btn_toggle_mock, if (isMocking) "Stop Mocking" else "Start Mocking")

        val toggleIntent = Intent(context, MockWidgetProvider::class.java).apply {
            action = "ACTION_TOGGLE_CLICK"
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context, 100, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_toggle_mock, togglePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}