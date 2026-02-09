package com.example.pillease

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import org.json.JSONObject
import java.util.Calendar

class Medicationreminder : AppCompatActivity() {

    private var selectedHour: Int = -1
    private var selectedMinute: Int = -1
    private lateinit var remindersContainer: LinearLayout
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicationreminder)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get user_id from SharedPreferences
        val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("user_id", -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val etMedicationName = findViewById<EditText>(R.id.etMedicationName)
        val btnSelectTime = findViewById<Button>(R.id.btnSelectTime)
        val spinnerFrequency = findViewById<Spinner>(R.id.spinnerFrequency)
        val btnSaveReminder = findViewById<Button>(R.id.btnSaveReminder)
        remindersContainer = findViewById<LinearLayout>(R.id.remindersContainer)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val frequencies = arrayOf("Once a day", "Twice a day", "Three times a day", "Every 8 hours", "As needed")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrequency.adapter = spinnerAdapter

        // Load existing reminders
        loadMedicationReminders()

        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(this, { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                btnSelectTime.text = String.format("Time: %02d:%02d", hour, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePicker.show()
        }

        btnSaveReminder.setOnClickListener {
            val medName = etMedicationName.text.toString()
            val frequency = spinnerFrequency.selectedItem.toString()

            if (medName.isEmpty()) {
                Toast.makeText(this, "Please enter medication name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedHour == -1) {
                Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
            saveReminder(medName, timeStr, frequency)
            
            // Reset fields
            etMedicationName.text.clear()
            btnSelectTime.text = "Select Time"
            selectedHour = -1
            selectedMinute = -1
        }
    }

    private fun saveReminder(medName: String, time: String, frequency: String) {
        val url = "http://10.0.2.2/pillease_db/medication_reminders.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()
                        setMedicationAlarm(medName, time)
                        loadMedicationReminders() // Reload to show new entry
                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error saving reminder", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "medication_name" to medName,
                    "reminder_time" to "$time:00",
                    "frequency" to frequency
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadMedicationReminders() {
        remindersContainer.removeAllViews()
        
        if (userId == -1) {
            Toast.makeText(this, "Please setup your profile first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2/pillease_db/medication_reminders.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val reminders = json.getJSONArray("data")
                        for (i in 0 until reminders.length()) {
                            val reminder = reminders.getJSONObject(i)
                            displayReminder(reminder)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading reminders", Toast.LENGTH_SHORT).show()
                }
            },
            { error -> }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun displayReminder(reminderData: JSONObject) {
        val reminderId = reminderData.getInt("reminder_id")
        val medName = reminderData.getString("medication_name")
        val time = reminderData.getString("reminder_time").substring(0, 5) // HH:mm
        val frequency = reminderData.getString("frequency")

        val reminderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val reminderView = TextView(this).apply {
            text = "💊 $medName\nTime: $time | $frequency"
            textSize = 16f
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
                deleteReminder(reminderId, reminderLayout)
            }
        }

        reminderLayout.addView(reminderView)
        reminderLayout.addView(deleteButton)
        remindersContainer.addView(reminderLayout, 0)
    }

    private fun deleteReminder(reminderId: Int, reminderLayout: LinearLayout) {
        val url = "http://10.0.2.2/pillease_db/medication_reminders.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        remindersContainer.removeView(reminderLayout)
                        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error deleting reminder", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "action" to "delete",
                    "reminder_id" to reminderId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun setMedicationAlarm(medName: String, time: String) {
        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("MED_NAME", medName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, medName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } catch (e: SecurityException) { }
    }
}
