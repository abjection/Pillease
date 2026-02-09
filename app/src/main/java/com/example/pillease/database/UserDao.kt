package com.example.pillease.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pillease.entities.User

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
}
