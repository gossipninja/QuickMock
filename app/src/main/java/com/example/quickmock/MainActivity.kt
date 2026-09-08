package com.example.quickmock

import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private lateinit var tvCurrentCoords: TextView

    private val slotViews = mutableMapOf<Int, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCurrentCoords = findViewById(R.id.tv_current_coords)
        val btnGetCoords = findViewById<Button>(R.id.btn_get_current_loc_global)
        val btnLookup = findViewById<Button>(R.id.btn_lookup_address)
        val containerSlots = findViewById<LinearLayout>(R.id.container_slots)

        btnGetCoords.setOnClickListener { fetchCurrentLocationAndPromptSlot() }
        btnLookup.setOnClickListener { showNativeGeocoderDialog() }

        val inflater = LayoutInflater.from(this)
        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)

        for (i in 1..9) {
            val cardView = inflater.inflate(R.layout.item_slot_card, containerSlots, false)
            slotViews[i] = cardView

            val tvHeader = cardView.findViewById<TextView>(R.id.tv_slot_header)
            val etName = cardView.findViewById<EditText>(R.id.et_slot_name)
            val etLat = cardView.findViewById<EditText>(R.id.et_slot_lat)
            val etLng = cardView.findViewById<EditText>(R.id.et_slot_lng)
            val btnFillCurrent = cardView.findViewById<Button>(R.id.btn_fill_current_loc)
            val btnSave = cardView.findViewById<Button>(R.id.btn_save_slot)
            val btnClear = cardView.findViewById<Button>(R.id.btn_clear_slot)

            tvHeader.text = "Slot $i"
            etName.setText(prefs.getString("name_$i", "Slot $i"))
            etLat.setText(prefs.getFloat("lat_$i", 0f).toString())
            etLng.setText(prefs.getFloat("lng_$i", 0f).toString())

            btnFillCurrent.setOnClickListener {
                if (currentLat != null && currentLng != null) {
                    etLat.setText(currentLat.toString())
                    etLng.setText(currentLng.toString())
                } else {
                    Toast.makeText(this, "Fetch GPS coordinates first using top button", Toast.LENGTH_SHORT).show()
                }
            }

            btnClear.setOnClickListener {
                etName.setText("Slot $i")
                etLat.setText("0.0")
                etLng.setText("0.0")
                saveSlotData(i, "Slot $i", 0f, 0f)
                Toast.makeText(this, "Cleared Slot $i", Toast.LENGTH_SHORT).show()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val latStr = etLat.text.toString().trim()
                val lngStr = etLng.text.toString().trim()

                if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                    Toast.makeText(this, "Please complete all fields for Slot $i", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val lat = latStr.toFloatOrNull()
                val lng = lngStr.toFloatOrNull()
                if (lat == null || lng == null) {
                    Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val existingLat = prefs.getFloat("lat_$i", 0f)
                val existingLng = prefs.getFloat("lng_$i", 0f)
                val existingName = prefs.getString("name_$i", "Slot $i") ?: "Slot $i"

                if (existingLat != 0f || existingLng != 0f) {
                    AlertDialog.Builder(this)
                        .setTitle("Overwrite Slot $i?")
                        .setMessage("Slot $i is currently set to '$existingName' ($existingLat, $existingLng). Are you sure you want to overwrite it?")
                        .setPositiveButton("Overwrite") { _, _ ->
                            saveSlotData(i, name, lat, lng)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    saveSlotData(i, name, lat, lng)
                }
            }

            containerSlots.addView(cardView)
        }
    }

    private fun showNativeGeocoderDialog() {
        val input = EditText(this).apply {
            hint = "e.g. 1600 Amphitheatre Pkwy, Mountain View, CA"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Lookup Address")
            .setMessage("Enter an address or landmark to retrieve GPS coordinates:")
            .setView(input)
            .setPositiveButton("Lookup") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    performGeocoding(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performGeocoding(query: String) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Geocoder is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    runOnUiThread { handleGeocodeResult(addresses.firstOrNull(), query) }
                }

                override fun onError(errorMessage: String?) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Lookup failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                handleGeocodeResult(addresses?.firstOrNull(), query)
            } catch (e: Exception) {
                Toast.makeText(this, "Geocoding failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGeocodeResult(address: Address?, query: String) {
        if (address != null) {
            val lat = address.latitude.toFloat()
            val lng = address.longitude.toFloat()

            currentLat = address.latitude
            currentLng = address.longitude
            tvCurrentCoords.text = "Current GPS: $lat, $lng"

            AlertDialog.Builder(this)
                .setTitle("Address Found")
                .setMessage("Address: ${address.getAddressLine(0) ?: query}\n\nLat: $lat\nLng: $lng")
                .setPositiveButton("Assign to Slot") { _, _ ->
                    promptSlotSelection(lat, lng)
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            Toast.makeText(this, "No location coordinates found for '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCurrentLocationAndPromptSlot() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (loc != null) {
            currentLat = loc.latitude
            currentLng = loc.longitude
            tvCurrentCoords.text = "Current GPS: ${loc.latitude}, ${loc.longitude}"

            promptSlotSelection(loc.latitude.toFloat(), loc.longitude.toFloat())
        } else {
            Toast.makeText(this, "Unable to determine current GPS location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptSlotSelection(lat: Float, lng: Float) {
        val slotOptions = Array(9) { i -> "Slot ${i + 1}" }

        AlertDialog.Builder(this)
            .setTitle("Select Target Slot")
            .setItems(slotOptions) { _, which ->
                val slot = which + 1
                saveToSlotWithCheck(slot, lat, lng)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToSlotWithCheck(slot: Int, lat: Float, lng: Float) {
        val prefs = getSharedPreferences("quickmock_prefs", Context.MODE_PRIVATE)
        val existingLat = prefs.getFloat("lat_$slot", 0f)
        val existingLng = prefs.getFloat("lng_$slot", 0f)
        val existingName = prefs.getString("name_$slot", "Slot $slot") ?: "Slot $slot"

        if (existingLat != 0f || existingLng != 0f) {
            AlertDialog.Builder(this)
                .setTitle("Slot $slot Occupied")
                .setMessage("Slot $slot currently has saved coordinates ('$existingName': $existingLat, $existingLng). Please clear Slot $slot first before overwriting via GPS fetch.")
                .setPositiveButton("OK", null)
                .show()
        } else {
            val cardView = slotViews[slot]
            cardView?.findViewById<EditText>(R.id.et_slot_lat)?.setText(lat.toString())
            cardView?.findViewById<EditText>(R.id.et_slot_lng)?.setText(lng.toString())

            saveSlotData(slot, existingName, lat, lng)
            Toast.makeText(this, "Saved current GPS to Slot $slot", Toast.LENGTH_SHORT).show()
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
    }
}