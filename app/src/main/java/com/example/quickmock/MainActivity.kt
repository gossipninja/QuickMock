package com.example.quickmock

import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editNames: Array<EditText>
    private lateinit var editLats: Array<EditText>
    private lateinit var editLngs: Array<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editNames = arrayOf(
            findViewById(R.id.edit_name_1), findViewById(R.id.edit_name_2), findViewById(R.id.edit_name_3),
            findViewById(R.id.edit_name_4), findViewById(R.id.edit_name_5), findViewById(R.id.edit_name_6),
            findViewById(R.id.edit_name_7), findViewById(R.id.edit_name_8), findViewById(R.id.edit_name_9)
        )

        editLats = arrayOf(
            findViewById(R.id.edit_lat_1), findViewById(R.id.edit_lat_2), findViewById(R.id.edit_lat_3),
            findViewById(R.id.edit_lat_4), findViewById(R.id.edit_lat_5), findViewById(R.id.edit_lat_6),
            findViewById(R.id.edit_lat_7), findViewById(R.id.edit_lat_8), findViewById(R.id.edit_lat_9)
        )

        editLngs = arrayOf(
            findViewById(R.id.edit_lng_1), findViewById(R.id.edit_lng_2), findViewById(R.id.edit_lng_3),
            findViewById(R.id.edit_lng_4), findViewById(R.id.edit_lng_5), findViewById(R.id.edit_lng_6),
            findViewById(R.id.edit_lng_7), findViewById(R.id.edit_lng_8), findViewById(R.id.edit_lng_9)
        )

        loadFromPrefs()

        findViewById<Button>(R.id.btn_find_coords).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.latlong.net/")))
        }

        findViewById<Button>(R.id.btn_save_current_loc).setOnClickListener {
            captureAndSaveGPS()
        }

        findViewById<Button>(R.id.btn_export_json).setOnClickListener {
            exportJSON()
        }

        findViewById<Button>(R.id.btn_import_json).setOnClickListener {
            importJSON()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveToPrefs()
            notifyWidget()
            Toast.makeText(this, "Saved & Widget Updated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFromPrefs() {
        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
        val defaultJsonString = assets.open("defaults.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(defaultJsonString)

        for (i in 0 until 9) {
            val idx = i + 1
            val defaultObj = jsonArray.optJSONObject(i) ?: JSONObject()
            val defName = defaultObj.optString("name", "")
            val defLat = defaultObj.optDouble("lat", 0.0).toFloat()
            val defLng = defaultObj.optDouble("lng", 0.0).toFloat()

            editNames[i].setText(prefs.getString("name_$idx", defName))
            editLats[i].setText(prefs.getFloat("lat_$idx", defLat).toString())
            editLngs[i].setText(prefs.getFloat("lng_$idx", defLng).toString())
        }
    }

    private fun saveToPrefs() {
        val editor = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE).edit()
        for (i in 0 until 9) {
            val idx = i + 1
            editor.putString("name_$idx", editNames[i].text.toString())
            editor.putFloat("lat_$idx", editLats[i].text.toString().toFloatOrNull() ?: 0f)
            editor.putFloat("lng_$idx", editLngs[i].text.toString().toFloatOrNull() ?: 0f)
        }
        editor.apply()
    }

    private fun captureAndSaveGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            Toast.makeText(this, "Could not obtain current GPS location", Toast.LENGTH_SHORT).show()
            return
        }

        val nameInput = EditText(this).apply { hint = "Location Name" }
        val slotOptions = (1..9).map { "Slot $it: ${editNames[it - 1].text}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Save Current Location")
            .setMessage("Lat: ${location.latitude}\nLng: ${location.longitude}")
            .setView(nameInput)
            .setSingleChoiceItems(slotOptions, 0, null)
            .setPositiveButton("Save") { dialog, _ ->
                val selectedSlot = (dialog as AlertDialog).listView.checkedItemPosition
                val name = nameInput.text.toString().ifEmpty { "My Location" }

                editNames[selectedSlot].setText(name)
                editLats[selectedSlot].setText(location.latitude.toString())
                editLngs[selectedSlot].setText(location.longitude.toString())

                saveToPrefs()
                notifyWidget()
                Toast.makeText(this, "Saved to Slot ${selectedSlot + 1}!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportJSON() {
        val jsonArray = JSONArray()
        for (i in 0 until 9) {
            val obj = JSONObject().apply {
                put("slot", i + 1)
                put("name", editNames[i].text.toString())
                put("lat", editLats[i].text.toString().toDoubleOrNull() ?: 0.0)
                put("lng", editLngs[i].text.toString().toDoubleOrNull() ?: 0.0)
            }
            jsonArray.put(obj)
        }
        val file = File(getExternalFilesDir(null), "defaults.json")
        file.writeText(jsonArray.toString(2))
        Toast.makeText(this, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun importJSON() {
        val file = File(getExternalFilesDir(null), "defaults.json")
        if (!file.exists()) {
            Toast.makeText(this, "defaults.json not found in app folder", Toast.LENGTH_SHORT).show()
            return
        }
        val jsonArray = JSONArray(file.readText())
        for (i in 0 until minOf(9, jsonArray.length())) {
            val obj = jsonArray.getJSONObject(i)
            editNames[i].setText(obj.optString("name", ""))
            editLats[i].setText(obj.optDouble("lat", 0.0).toString())
            editLngs[i].setText(obj.optDouble("lng", 0.0).toString())
        }
        saveToPrefs()
        notifyWidget()
        Toast.makeText(this, "Imported from JSON!", Toast.LENGTH_SHORT).show()
    }

    private fun notifyWidget() {
        val intent = Intent(this, MockWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application)
                .getAppWidgetIds(ComponentName(application, MockWidgetProvider::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }
}