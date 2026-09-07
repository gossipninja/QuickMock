package com.example.quickmock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val name1 = prefs.getString("name_1", "Loc 1") ?: "Loc 1"
            val lat1 = prefs.getFloat("lat_1", 41.3686f).toDouble()
            val lng1 = prefs.getFloat("lng_1", -82.1076f).toDouble()

            val name2 = prefs.getString("name_2", "Loc 2") ?: "Loc 2"
            val lat2 = prefs.getFloat("lat_2", 41.5055f).toDouble()
            val lng2 = prefs.getFloat("lng_2", -81.6074f).toDouble()

            val name3 = prefs.getString("name_3", "Loc 3") ?: "Loc 3"
            val lat3 = prefs.getFloat("lat_3", 40.7128f).toDouble()
            val lng3 = prefs.getFloat("lng_3", -74.0060f).toDouble()

            val name4 = prefs.getString("name_4", "Loc 4") ?: "Loc 4"
            val lat4 = prefs.getFloat("lat_4", 34.0522f).toDouble()
            val lng4 = prefs.getFloat("lng_4", -118.2437f).toDouble()

            views.setTextViewText(R.id.btn_loc_1, name1)
            views.setOnClickPendingIntent(R.id.btn_loc_1, createMockIntent(context, lat1, lng1, 1))

            views.setTextViewText(R.id.btn_loc_2, name2)
            views.setOnClickPendingIntent(R.id.btn_loc_2, createMockIntent(context, lat2, lng2, 2))

            views.setTextViewText(R.id.btn_loc_3, name3)
            views.setOnClickPendingIntent(R.id.btn_loc_3, createMockIntent(context, lat3, lng3, 3))

            views.setTextViewText(R.id.btn_loc_4, name4)
            views.setOnClickPendingIntent(R.id.btn_loc_4, createMockIntent(context, lat4, lng4, 4))

            val clearIntent = Intent(context, MockWidgetProvider::class.java).apply {
                action = "com.example.quickmock.ACTION_CLEAR_MOCK"
            }
            val clearPendingIntent = PendingIntent.getBroadcast(
                context, 999, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_clear, clearPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createMockIntent(context: Context, lat: Double, lng: Double, requestCode: Int): PendingIntent {
        val intent = Intent(context, MockWidgetProvider::class.java).apply {
            action = "com.example.quickmock.ACTION_SET_MOCK"
            putExtra("EXTRA_LAT", lat)
            putExtra("EXTRA_LNG", lng)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val serviceIntent = Intent(context, MockLocationService::class.java)

        when (intent.action) {
            "com.example.quickmock.ACTION_SET_MOCK" -> {
                serviceIntent.action = "START_MOCK"
                serviceIntent.putExtra("LAT", intent.getDoubleExtra("EXTRA_LAT", 0.0))
                serviceIntent.putExtra("LNG", intent.getDoubleExtra("EXTRA_LNG", 0.0))
                context.startForegroundService(serviceIntent)
            }
            "com.example.quickmock.ACTION_CLEAR_MOCK" -> {
                serviceIntent.action = "STOP_MOCK"
                context.startService(serviceIntent)
            }
        }
    }
}