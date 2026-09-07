package com.example.quickmock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // HARDCODED COORDINATES
            views.setOnClickPendingIntent(R.id.btn_loc_1, createMockIntent(context, 41.3686, -82.1076, 1))
            views.setOnClickPendingIntent(R.id.btn_loc_2, createMockIntent(context, 41.5055, -81.6074, 2))
            views.setOnClickPendingIntent(R.id.btn_loc_3, createMockIntent(context, 40.7128, -74.0060, 3))

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
