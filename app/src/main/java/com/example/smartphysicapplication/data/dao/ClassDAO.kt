package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.smartphysicapplication.data.models.ClassMODEL

@Dao
interface ClassDAO {
    @Query("SELECT * FROM Class")
    suspend fun getAll(): List<ClassMODEL>
}