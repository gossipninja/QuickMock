package com.example.quickmock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews

class MockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        val btnIds = intArrayOf(
            R.id.btn_loc_1, R.id.btn_loc_2, R.id.btn_loc_3,
            R.id.btn_loc_4, R.id.btn_loc_5, R.id.btn_loc_6,
            R.id.btn_loc_7, R.id.btn_loc_8, R.id.btn_loc_9
        )

        val defaultNames = arrayOf(
            "Sam's Sandusky", "Cedar Point", "Sam's Sheffield", "Sam's Brooklyn",
            "", "", "", "", ""
        )
        val defaultLats = floatArrayOf(41.4186f, 41.4822f, 41.4239f, 41.4222f, 0f, 0f, 0f, 0f, 0f)
        val defaultLngs = floatArrayOf(-82.6844f, -82.6835f, -82.1082f, -81.7423f, 0f, 0f, 0f, 0f, 0f)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            for (i in 0 until 9) {
                val idx = i + 1
                val name = prefs.getString("name_$idx", defaultNames[i]) ?: ""
                val lat = prefs.getFloat("lat_$idx", defaultLats[i]).toDouble()
                val lng = prefs.getFloat("lng_$idx", defaultLngs[i]).toDouble()

                if (name.isNotEmpty() && (lat != 0.0 || lng != 0.0)) {
                    views.setViewVisibility(btnIds[i], View.VISIBLE)
                    views.setTextViewText(btnIds[i], name)
                    views.setOnClickPendingIntent(btnIds[i], createMockIntent(context, lat, lng, idx))
                } else {
                    views.setViewVisibility(btnIds[i], View.GONE)
                }
            }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            "com.example.quickmock.ACTION_CLEAR_MOCK" -> {
                serviceIntent.action = "STOP_MOCK"
                context.startService(serviceIntent)
            }
        }
    }
}