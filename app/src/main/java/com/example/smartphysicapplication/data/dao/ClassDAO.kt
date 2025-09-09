package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartphysicapplication.data.models.ClassMODEL

@Dao
interface ClassDAO {
    @Upsert
    suspend fun upsert(vararg items: ClassMODEL)

    @Query("SELECT * FROM Class")
    suspend fun getAll(): List<ClassMODEL>
}