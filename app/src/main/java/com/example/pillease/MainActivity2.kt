package com.example.pillease

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cardReminder = findViewById<CardView>(R.id.cardReminder)
        val cardHealthLog = findViewById<CardView>(R.id.cardHealthLog)
        val cardHistory = findViewById<CardView>(R.id.cardHistory)
        val cardSetupProfile = findViewById<CardView>(R.id.cardSetupProfile)

        // Connect to Setup Profile Activity via Card
        cardSetupProfile.setOnClickListener {
            // Check if user exists, if so edit, otherwise create new
            val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
            val userId = sharedPrefs.getInt("user_id", -1)
            
            val intent = Intent(this, SetupProfileActivity::class.java)
            if (userId != -1) {
                intent.putExtra("user_id", userId)
            }
            startActivity(intent)
        }
        
        // Long press to clear profile (reset to first-time setup)
        cardSetupProfile.setOnLongClickListener {
            showClearProfileDialog()
            true
        }

        // Connect to Medicationreminder Activity
        cardReminder.setOnClickListener {
            val intent = Intent(this, Medicationreminder::class.java)
            startActivity(intent)
        }

        // Connect to DailyHealthLog Activity
        cardHealthLog.setOnClickListener {
            val intent = Intent(this, DailyHealthLog::class.java)
            startActivity(intent)
        }

        // Connect to MedicalHistoryTracker Activity
        cardHistory.setOnClickListener {
            val intent = Intent(this, MedicalHistoryTracker::class.java)
            startActivity(intent)
        }
    }
    
    private fun showClearProfileDialog() {
        val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getInt("user_id", -1)
        
        if (userId == -1) {
            Toast.makeText(this, "No profile to clear", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Clear Profile")
            .setMessage("Are you sure you want to clear your profile? This will reset the app to first-time setup. Your data on the server will not be deleted.")
            .setPositiveButton("Clear") { _, _ ->
                // Clear user_id from SharedPreferences
                sharedPrefs.edit().remove("user_id").apply()
                Toast.makeText(this, "Profile cleared! You can now set up a new profile.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
