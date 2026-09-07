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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        val editName1 = findViewById<EditText>(R.id.edit_name_1)
        val editLat1 = findViewById<EditText>(R.id.edit_lat_1)
        val editLng1 = findViewById<EditText>(R.id.edit_lng_1)

        val editName2 = findViewById<EditText>(R.id.edit_name_2)
        val editLat2 = findViewById<EditText>(R.id.edit_lat_2)
        val editLng2 = findViewById<EditText>(R.id.edit_lng_2)

        val editName3 = findViewById<EditText>(R.id.edit_name_3)
        val editLat3 = findViewById<EditText>(R.id.edit_lat_3)
        val editLng3 = findViewById<EditText>(R.id.edit_lng_3)

        val editName4 = findViewById<EditText>(R.id.edit_name_4)
        val editLat4 = findViewById<EditText>(R.id.edit_lat_4)
        val editLng4 = findViewById<EditText>(R.id.edit_lng_4)

        // Load existing
        editName1.setText(prefs.getString("name_1", "Loc 1"))
        editLat1.setText(prefs.getFloat("lat_1", 41.3686f).toString())
        editLng1.setText(prefs.getFloat("lng_1", -82.1076f).toString())

        editName2.setText(prefs.getString("name_2", "Loc 2"))
        editLat2.setText(prefs.getFloat("lat_2", 41.5055f).toString())
        editLng2.setText(prefs.getFloat("lng_2", -81.6074f).toString())

        editName3.setText(prefs.getString("name_3", "Loc 3"))
        editLat3.setText(prefs.getFloat("lat_3", 40.7128f).toString())
        editLng3.setText(prefs.getFloat("lng_3", -74.0060f).toString())

        editName4.setText(prefs.getString("name_4", "Loc 4"))
        editLat4.setText(prefs.getFloat("lat_4", 34.0522f).toString())
        editLng4.setText(prefs.getFloat("lng_4", -118.2437f).toString())

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            prefs.edit().apply {
                putString("name_1", editName1.text.toString())
                putFloat("lat_1", editLat1.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_1", editLng1.text.toString().toFloatOrNull() ?: 0f)

                putString("name_2", editName2.text.toString())
                putFloat("lat_2", editLat2.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_2", editLng2.text.toString().toFloatOrNull() ?: 0f)

                putString("name_3", editName3.text.toString())
                putFloat("lat_3", editLat3.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_3", editLng3.text.toString().toFloatOrNull() ?: 0f)

                putString("name_4", editName4.text.toString())
                putFloat("lat_4", editLat4.text.toString().toFloatOrNull() ?: 0f)
                putFloat("lng_4", editLng4.text.toString().toFloatOrNull() ?: 0f)
                apply()
            }

            // Trigger widget refresh
            val intent = Intent(this, MockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(application)
                    .getAppWidgetIds(ComponentName(application, MockWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}