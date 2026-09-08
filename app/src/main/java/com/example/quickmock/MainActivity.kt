package com.example.quickmock

import android.app.AlertDialog
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

        val etSlotNumber = findViewById<EditText>(R.id.et_slot_number)
        val etName = findViewById<EditText>(R.id.et_location_name)
        val etLat = findViewById<EditText>(R.id.et_latitude)
        val etLng = findViewById<EditText>(R.id.et_longitude)
        val btnSave = findViewById<Button>(R.id.btn_save_slot)

        btnSave.setOnClickListener {
            val slotStr = etSlotNumber.text.toString().trim()
            val name = etName.text.toString().trim()
            val latStr = etLat.text.toString().trim()
            val lngStr = etLng.text.toString().trim()

            if (slotStr.isEmpty() || name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val slot = slotStr.toIntOrNull()
            if (slot == null || slot !in 1..9) {
                Toast.makeText(this, "Slot must be between 1 and 9", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lat = latStr.toFloatOrNull()
            val lng = lngStr.toFloatOrNull()
            if (lat == null || lng == null) {
                Toast.makeText(this, "Invalid Latitude/Longitude", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
            val existingName = prefs.getString("name_$slot", null)

            if (!existingName.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Overwrite Slot $slot?")
                    .setMessage("Slot $slot already contains '$existingName'. Are you sure you want to overwrite it?")
                    .setPositiveButton("Overwrite") { _, _ ->
                        saveSlotData(slot, name, lat, lng)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                saveSlotData(slot, name, lat, lng)
            }
        }
    }

    private fun saveSlotData(slot: Int, name: String, lat: Float, lng: Float) {
        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name_$slot", name)
            putFloat("lat_$slot", lat)
            putFloat("lng_$slot", lng)
            apply()
        }

        val intent = Intent(this, MockWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(applicationContext)
                .getAppWidgetIds(ComponentName(applicationContext, MockWidgetProvider::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)

        Toast.makeText(this, "Saved to Slot $slot", Toast.LENGTH_SHORT).show()
    }
}