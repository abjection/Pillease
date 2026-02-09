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
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class MedicalHistoryTracker : AppCompatActivity() {
    
    private lateinit var historyContainer: LinearLayout
    private var userId: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medical_history_tracker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get user_id from SharedPreferences
        val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("user_id", -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val etRecordName = findViewById<TextInputEditText>(R.id.etRecordName)
        val etRecordDate = findViewById<TextInputEditText>(R.id.etRecordDate)
        val btnAddRecord = findViewById<MaterialButton>(R.id.btnAddRecord)
        historyContainer = findViewById<LinearLayout>(R.id.historyContainer)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load existing records
        loadMedicalHistory()

        btnAddRecord.setOnClickListener {
            val name = etRecordName.text.toString()
            val date = etRecordDate.text.toString()

            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveMedicalRecord(name, date)

            // Clear inputs
            etRecordName.text?.clear()
            etRecordDate.text?.clear()
        }
    }

    private fun saveMedicalRecord(name: String, date: String) {
        val url = "http://10.0.2.2/pillease_db/medical_history.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        Toast.makeText(this, "Medical record added!", Toast.LENGTH_SHORT).show()
                        loadMedicalHistory() // Reload to show new entry
                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error saving record", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "record_name" to name,
                    "record_date" to date
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadMedicalHistory() {
        historyContainer.removeAllViews()
        
        if (userId == -1) {
            Toast.makeText(this, "Please setup your profile first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2/pillease_db/medical_history.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val records = json.getJSONArray("data")
                        for (i in 0 until records.length()) {
                            val record = records.getJSONObject(i)
                            displayRecord(record)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading records", Toast.LENGTH_SHORT).show()
                }
            },
            { error -> }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun displayRecord(recordData: JSONObject) {
        val historyId = recordData.getInt("history_id")
        val recordName = recordData.getString("record_name")
        val recordDate = recordData.getString("record_date")

        val recordLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val recordView = TextView(this).apply {
            text = "📁 $recordName\nDate: $recordDate"
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
                deleteRecord(historyId, recordLayout)
            }
        }

        recordLayout.addView(recordView)
        recordLayout.addView(deleteButton)
        historyContainer.addView(recordLayout, 0)
    }

    private fun deleteRecord(historyId: Int, recordLayout: LinearLayout) {
        val url = "http://10.0.2.2/pillease_db/medical_history.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        historyContainer.removeView(recordLayout)
                        Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error deleting record", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "action" to "delete",
                    "history_id" to historyId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
