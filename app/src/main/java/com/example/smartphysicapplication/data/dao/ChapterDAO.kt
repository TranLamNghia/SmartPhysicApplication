package com.example.smartphysicapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartphysicapplication.data.models.ChapterMODEL

@Dao
interface ChapterDAO {
    @Upsert
    suspend fun upsert(vararg items: ChapterMODEL)

    @Query("SELECT ChapterID, ChapterName FROM Chapter WHERE ClassId = :classId")
    suspend fun getChaptersByClassId(classId: String): List<ChapterMODEL>

    @Query("SELECT ChapterName, SourceImageFormula FROM Chapter WHERE ClassId= :classId")
    suspend fun getFormulaByClassId(classId: String): String

    @Query("SELECT ChapterName, SourceImageMindMap FROM Chapter WHERE ClassId= :classId")
    suspend fun getMindMapByClassId(classId: String): String

}