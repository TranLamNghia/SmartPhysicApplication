package com.example.smartphysicapplication.data.models

import androidx.room.Entity

@Entity(tableName = "Class", primaryKeys = ["ClassId"])
data class ClassMODEL (
    val ClassId: String,
    val ClassLevel: Int
)