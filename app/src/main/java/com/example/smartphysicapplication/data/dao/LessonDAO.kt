package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.smartphysicapplication.data.models.LessonMODEL

@Dao
interface LessonDAO {
    @Query("SELECT LessonName FROM Lesson WHERE ClassId = :classId AND ChapterId = :chapterId")
    suspend fun getLessonsNameByClassIdAndChapterId(classId: String, chapterId: String): String

    @Query("SELECT LessonId, LessonName, SourceVideo FROM Lesson WHERE ClassId = :classId AND ChapterId = :chapterId AND LessonId = :lessonId")
    suspend fun getLessonById(classId: String, chapterId: String, lessonId: String): LessonMODEL
}