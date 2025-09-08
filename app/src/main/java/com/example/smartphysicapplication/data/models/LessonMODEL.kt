package com.example.smartphysicapplication.data.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "Lesson", primaryKeys = ["LessonId", "ChapterId", "ClassId"],
    foreignKeys = [ForeignKey(entity = ChapterMODEL::class, parentColumns = ["ChapterId", "ClassId"], childColumns = ["ChapterId", "ClassId"])])

data class LessonMODEL (
    val LessonId: String,
    val ChapterId: String,
    val ClassId: String,
    val LessonName: String,
    val SourceVideo: String?
)