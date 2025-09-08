package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.smartphysicapplication.data.models.ChapterMODEL

@Dao
interface ChapterDAO {
    @Query("SELECT ChapterID, ChapterName FROM Chapter WHERE ClassId = :classId")
    suspend fun getChaptersByClassId(classId: String): List<ChapterMODEL>

    @Query("SELECT SourceImageFormula FROM Chapter WHERE ClassId= :classId")
    suspend fun getFormulaByClassId(classId: String): String

    @Query("SELECT SourceImageMindMap FROM Chapter WHERE ClassId= :classId")
    suspend fun getMindMapByClassId(classId: String): String

}