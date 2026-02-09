package com.example.pillease.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val user_id: Int = 0,
    val full_name: String,
    val age: Int,
    val email: String,
    val created_at: Long = System.currentTimeMillis(),
    val gender: String? = null,
    val blood_type: String? = null,
    val emergency_contact: String? = null,
    val password_hash: String? = null
)
