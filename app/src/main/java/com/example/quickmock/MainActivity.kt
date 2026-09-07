package com.example.quickmock

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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

        val editNames = arrayOf(
            findViewById<EditText>(R.id.edit_name_1), findViewById<EditText>(R.id.edit_name_2), findViewById<EditText>(R.id.edit_name_3),
            findViewById<EditText>(R.id.edit_name_4), findViewById<EditText>(R.id.edit_name_5), findViewById<EditText>(R.id.edit_name_6),
            findViewById<EditText>(R.id.edit_name_7), findViewById<EditText>(R.id.edit_name_8), findViewById<EditText>(R.id.edit_name_9)
        )

        val editLats = arrayOf(
            findViewById<EditText>(R.id.edit_lat_1), findViewById<EditText>(R.id.edit_lat_2), findViewById<EditText>(R.id.edit_lat_3),
            findViewById<EditText>(R.id.edit_lat_4), findViewById<EditText>(R.id.edit_lat_5), findViewById<EditText>(R.id.edit_lat_6),
            findViewById<EditText>(R.id.edit_lat_7), findViewById<EditText>(R.id.edit_lat_8), findViewById<EditText>(R.id.edit_lat_9)
        )

        val editLngs = arrayOf(
            findViewById<EditText>(R.id.edit_lng_1), findViewById<EditText>(R.id.edit_lng_2), findViewById<EditText>(R.id.edit_lng_3),
            findViewById<EditText>(R.id.edit_lng_4), findViewById<EditText>(R.id.edit_lng_5), findViewById<EditText>(R.id.edit_lng_6),
            findViewById<EditText>(R.id.edit_lng_7), findViewById<EditText>(R.id.edit_lng_8), findViewById<EditText>(R.id.edit_lng_9)
        )

        val defaultNames = arrayOf(
            "Sam's Sandusky", "Cedar Point", "Sam's Sheffield", "Sam's Brooklyn",
            "", "", "", "", ""
        )
        val defaultLats = floatArrayOf(41.4186f, 41.4822f, 41.4239f, 41.4222f, 0f, 0f, 0f, 0f, 0f)
        val defaultLngs = floatArrayOf(-82.6844f, -82.6835f, -82.1082f, -81.7423f, 0f, 0f, 0f, 0f, 0f)

        for (i in 0 until 9) {
            val idx = i + 1
            editNames[i].setText(prefs.getString("name_$idx", defaultNames[i]))
            editLats[i].setText(prefs.getFloat("lat_$idx", defaultLats[i]).toString())
            editLngs[i].setText(prefs.getFloat("lng_$idx", defaultLngs[i]).toString())
        }

        findViewById<Button>(R.id.btn_find_coords).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.latlong.net/"))
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val editor = prefs.edit()
            for (i in 0 until 9) {
                val idx = i + 1
                editor.putString("name_$idx", editNames[i].text.toString())
                editor.putFloat("lat_$idx", editLats[i].text.toString().toFloatOrNull() ?: 0f)
                editor.putFloat("lng_$idx", editLngs[i].text.toString().toFloatOrNull() ?: 0f)
            }
            editor.apply()

            val intent = Intent(this, MockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(application)
                    .getAppWidgetIds(ComponentName(application, MockWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Saved & Widget Updated!", Toast.LENGTH_SHORT).show()
        }
    }
}