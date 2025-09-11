package com.example.smartphysicapplication.data.models

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Class", primaryKeys = ["ClassId"])
data class ClassMODEL(
    @SerializedName("classId") val ClassId: String,
    @SerializedName("classLevel") val ClassLevel: Int
)