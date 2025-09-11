package com.example.smartphysicapplication.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Lesson", primaryKeys = ["LessonId", "ChapterId", "ClassId"],
    foreignKeys = [ForeignKey(entity = ChapterMODEL::class, parentColumns = ["ChapterId", "ClassId"], childColumns = ["ChapterId", "ClassId"])])

data class LessonMODEL (
    @SerializedName("lessonId") val LessonId: String,
    @SerializedName("chapterId") val ChapterId: String,
    @SerializedName("classId")   val ClassId: String,
    @SerializedName("lessonName") val LessonName: String,
    @SerializedName("sourceVideo") val SourceVideo: String?
)