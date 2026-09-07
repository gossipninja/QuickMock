package com.example.quickmock

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        val eName1 = findViewById<EditText>(R.id.et_name_1)
        val eLat1 = findViewById<EditText>(R.id.et_lat_1)
        val eLng1 = findViewById<EditText>(R.id.et_lng_1)

        val eName2 = findViewById<EditText>(R.id.et_name_2)
        val eLat2 = findViewById<EditText>(R.id.et_lat_2)
        val eLng2 = findViewById<EditText>(R.id.et_lng_2)

        val eName3 = findViewById<EditText>(R.id.et_name_3)
        val eLat3 = findViewById<EditText>(R.id.et_lat_3)
        val eLng3 = findViewById<EditText>(R.id.et_lng_3)

        val eName4 = findViewById<EditText>(R.id.et_name_4)
        val eLat4 = findViewById<EditText>(R.id.et_lat_4)
        val eLng4 = findViewById<EditText>(R.id.et_lng_4)

        // Load existing values or defaults
        eName1.setText(prefs.getString("name_1", "Home"))
        eLat1.setText(prefs.getFloat("lat_1", 41.3686f).toString())
        eLng1.setText(prefs.getFloat("lng_1", -82.1076f).toString())

        eName2.setText(prefs.getString("name_2", "Work"))
        eLat2.setText(prefs.getFloat("lat_2", 41.5055f).toString())
        eLng2.setText(prefs.getFloat("lng_2", -81.6074f).toString())

        eName3.setText(prefs.getString("name_3", "NYC"))
        eLat3.setText(prefs.getFloat("lat_3", 40.7128f).toString())
        eLng3.setText(prefs.getFloat("lng_3", -74.0060f).toString())

        eName4.setText(prefs.getString("name_4", "LA"))
        eLat4.setText(prefs.getFloat("lat_4", 34.0522f).toString())
        eLng4.setText(prefs.getFloat("lng_4", -118.2437f).toString())

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            prefs.edit().apply {
                putString("name_1", eName1.text.toString())
                putFloat("lat_1", eLat1.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_1", eLng1.text.toString().toFloatOrNull() ?: 0f)

                putString("name_2", eName2.text.toString())
                putFloat("lat_2", eLat2.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_2", eLng2.text.toString().toFloatOrNull() ?: 0f)

                putString("name_3", eName3.text.toString())
                putFloat("lat_3", eLat3.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_3", eLng3.text.toString().toFloatOrNull() ?: 0f)

                putString("name_4", eName4.text.toString())
                putFloat("lat_4", eLat4.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_4", eLng4.text.toString().toFloatOrNull() ?: 0f)
                apply()
            }

            // Trigger Widget Update
            val intent = Intent(this, MockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(
                    ComponentName(application, MockWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}