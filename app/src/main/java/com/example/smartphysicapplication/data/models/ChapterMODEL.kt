package com.example.smartphysicapplication.data.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "Chapter", primaryKeys = ["ChapterId", "ClassId"],
    foreignKeys = [ForeignKey(entity = ClassMODEL::class, parentColumns = ["ClassId"], childColumns = ["ClassId"])])

data class ChapterMODEL(
    val ChapterId: String,
    val ClassId: String,
    val ChapterName: String,
    val SourceImageFormula: String?,
    val SourceImageMindMap: String?
)