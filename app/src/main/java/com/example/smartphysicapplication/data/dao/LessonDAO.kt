package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartphysicapplication.data.models.LessonMODEL

@Dao
interface LessonDAO {
    @Upsert
    suspend fun upsert(vararg items: LessonMODEL)

    @Query("SELECT * FROM Lesson WHERE ClassId = :classId AND ChapterId = :chapterId")
    suspend fun getLessonsNameByClassIdAndChapterId(classId: String, chapterId: String): List<LessonMODEL>

    @Query("SELECT LessonId, LessonName, SourceVideo FROM Lesson WHERE ClassId = :classId AND ChapterId = :chapterId")
    suspend fun getLessonsByClassAndChapter(classId: String, chapterId: String): List<LessonAsset>
}

data class LessonAsset (
    val LessonId: String,
    val LessonName: String,
    val SourceVideo: String?
)