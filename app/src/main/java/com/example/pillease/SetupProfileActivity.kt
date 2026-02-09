package com.example.pillease

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.pillease.databinding.ActivitySetupProfileBinding
import org.json.JSONObject

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupProfileBinding
    private var userId: Int? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if editing existing profile
        userId = intent.getIntExtra("user_id", -1).takeIf { it != -1 }
        isEditMode = userId != null

        // Toolbar back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Profile" else "Setup Profile"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Setup Gender Spinner
        val genderOptions = arrayOf("Select Gender", "Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        binding.spinnerGender.adapter = genderAdapter

        // Setup Blood Type Spinner
        val bloodTypeOptions = arrayOf("Select Blood Type", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypeOptions)
        binding.spinnerBloodType.adapter = bloodTypeAdapter

        // Load existing profile if editing
        if (isEditMode) {
            loadUserProfile()
            binding.btnSaveProfile.text = "Update Profile"
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserProfile() {
        val url = "http://10.0.2.2/pillease_db/users.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val userData = json.getJSONObject("data")
                        
                        binding.etFullName.setText(userData.getString("full_name"))
                        binding.etAge.setText(userData.getString("age"))
                        binding.etEmail.setText(userData.getString("email"))
                        binding.etEmergencyContact.setText(userData.optString("emergency_contact", ""))
                        
                        // Set gender spinner
                        val gender = userData.optString("gender", "")
                        if (gender.isNotEmpty()) {
                            val genderPosition = (binding.spinnerGender.adapter as ArrayAdapter<String>).getPosition(gender)
                            if (genderPosition >= 0) binding.spinnerGender.setSelection(genderPosition)
                        }
                        
                        // Set blood type spinner
                        val bloodType = userData.optString("blood_type", "")
                        if (bloodType.isNotEmpty()) {
                            val bloodPosition = (binding.spinnerBloodType.adapter as ArrayAdapter<String>).getPosition(bloodType)
                            if (bloodPosition >= 0) binding.spinnerBloodType.setSelection(bloodPosition)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        
        // Get spinner values
        val gender = binding.spinnerGender.selectedItem.toString()
        val bloodType = binding.spinnerBloodType.selectedItem.toString()
        val emergencyContact = binding.etEmergencyContact.text.toString().trim()
        
        // Set to empty if default selection
        val genderValue = if (gender == "Select Gender") "" else gender
        val bloodTypeValue = if (bloodType == "Select Blood Type") "" else bloodType

        if (fullName.isEmpty() || ageStr.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show()
            return
        }

        // Emulator → localhost
        val url = "http://10.0.2.2/pillease_db/users.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.getBoolean("success")
                    val message = json.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    if (success) {
                        // Save user_id to SharedPreferences if creating new user
                        if (!isEditMode && json.has("user_id")) {
                            val newUserId = json.getInt("user_id")
                            val sharedPrefs = getSharedPreferences("PilleasePrefs", MODE_PRIVATE)
                            sharedPrefs.edit().putInt("user_id", newUserId).apply()
                        }
                        
                        startActivity(Intent(this, MainActivity2::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                val errorMsg = when {
                    error.networkResponse != null -> "Server error: ${error.networkResponse.statusCode}"
                    error.cause != null -> "Error: ${error.cause?.message}"
                    else -> "Network error: ${error.message}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                android.util.Log.e("SetupProfile", "Volley Error: ", error)
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = hashMapOf(
                    "full_name" to fullName,
                    "age" to age.toString(),
                    "email" to email,
                    "gender" to genderValue,
                    "blood_type" to bloodTypeValue,
                    "emergency_contact" to emergencyContact
                )
                
                // Add action and user_id if editing
                if (isEditMode && userId != null) {
                    params["action"] = "update"
                    params["user_id"] = userId.toString()
                }
                
                return params
            }
        }

        Volley.newRequestQueue(applicationContext).add(request)
    }
}
