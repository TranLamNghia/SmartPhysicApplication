package com.example.smartphysicapplication.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Chapter", primaryKeys = ["ChapterId", "ClassId"],
    foreignKeys = [ForeignKey(entity = ClassMODEL::class, parentColumns = ["ClassId"], childColumns = ["ClassId"])])

data class ChapterMODEL(
    @SerializedName("chapterId") val ChapterId: String,
    @SerializedName("classId")   val ClassId: String,
    @SerializedName("chapterName") val ChapterName: String,
    @SerializedName("sourceImageFormula") val SourceImageFormula: String?,
    @SerializedName("sourceImageMindMap") val SourceImageMindMap: String?
)