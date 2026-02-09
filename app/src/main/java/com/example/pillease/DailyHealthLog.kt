package com.example.pillease

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyHealthLog : AppCompatActivity() {
    
    private lateinit var logsContainer: LinearLayout
    private var userId: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_health_log)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get user_id from SharedPreferences
        val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("user_id", -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val moodToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.moodToggleGroup)
        val etTemperature = findViewById<TextInputEditText>(R.id.etTemperature)
        val etHeartRate = findViewById<TextInputEditText>(R.id.etHeartRate)
        val etSymptoms = findViewById<TextInputEditText>(R.id.etSymptoms)
        val btnSaveLog = findViewById<MaterialButton>(R.id.btnSaveLog)
        logsContainer = findViewById<LinearLayout>(R.id.logsContainer)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load existing logs
        loadHealthLogs()

        btnSaveLog.setOnClickListener {
            val selectedMoodId = moodToggleGroup.checkedButtonId
            val moodEmoji = when (selectedMoodId) {
                R.id.btnMoodHappy -> "Happy"
                R.id.btnMoodNeutral -> "Neutral"
                R.id.btnMoodSad -> "Sad"
                else -> "None"
            }
            
            val temp = etTemperature.text.toString()
            val heartRate = etHeartRate.text.toString()
            val symptoms = etSymptoms.text.toString()

            if (temp.isEmpty() || heartRate.isEmpty()) {
                Toast.makeText(this, "Please fill in your vitals", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveHealthLog(moodEmoji, temp, heartRate, symptoms)

            // Clear the form
            etTemperature.text?.clear()
            etHeartRate.text?.clear()
            etSymptoms.text?.clear()
            moodToggleGroup.clearChecked()
        }
    }

    private fun saveHealthLog(mood: String, temp: String, heartRate: String, symptoms: String) {
        val url = "http://10.0.2.2/pillease_db/health_log.php"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        Toast.makeText(this, "Health log saved!", Toast.LENGTH_SHORT).show()
                        loadHealthLogs() // Reload to show new entry
                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error saving log", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "mood" to mood,
                    "temperature" to temp,
                    "heart_rate" to heartRate,
                    "symptoms" to symptoms,
                    "log_date" to dateFormat.format(now),
                    "log_time" to timeFormat.format(now)
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadHealthLogs() {
        logsContainer.removeAllViews()
        
        if (userId == -1) {
            Toast.makeText(this, "Please setup your profile first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2/pillease_db/health_log.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val logs = json.getJSONArray("data")
                        for (i in 0 until logs.length()) {
                            val log = logs.getJSONObject(i)
                            displayLog(log)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading logs", Toast.LENGTH_SHORT).show()
                }
            },
            { error -> }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun displayLog(logData: JSONObject) {
        val logId = logData.getInt("log_id")
        val mood = logData.optString("mood", "None")
        val temp = logData.optString("temperature", "N/A")
        val heartRate = logData.optString("heart_rate", "N/A")
        val symptoms = logData.optString("symptoms", "None")
        val logDate = logData.getString("log_date")
        val logTime = logData.getString("log_time")

        val moodEmoji = when (mood) {
            "Happy" -> "😊"
            "Neutral" -> "😐"
            "Sad" -> "😔"
            else -> ""
        }

        val logLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val logEntry = TextView(this).apply {
            text = "📅 $logDate $logTime\nMood: $moodEmoji $mood | Temp: ${temp}°C | HR: $heartRate\nNotes: $symptoms"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val deleteButton = Button(this).apply {
            text = "🗑️"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                deleteLog(logId, logLayout)
            }
        }

        logLayout.addView(logEntry)
        logLayout.addView(deleteButton)
        logsContainer.addView(logLayout, 0)
    }

    private fun deleteLog(logId: Int, logLayout: LinearLayout) {
        val url = "http://10.0.2.2/pillease_db/health_log.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        logsContainer.removeView(logLayout)
                        Toast.makeText(this, "Log deleted", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error deleting log", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "action" to "delete",
                    "log_id" to logId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
