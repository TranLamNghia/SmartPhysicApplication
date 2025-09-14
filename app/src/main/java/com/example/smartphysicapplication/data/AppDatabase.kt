package com.example.smartphysicapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartphysicapplication.data.dao.*
import com.example.smartphysicapplication.data.models.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ClassMODEL::class, ChapterMODEL::class, LessonMODEL::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDAO
    abstract fun chapterDao(): ChapterDAO
    abstract fun lessonDao(): LessonDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context : Context) : AppDatabase {
            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "physic_smart_app_database"
//                ).addCallback(object : RoomDatabase.Callback() {
//                    override fun onCreate(db: SupportSQLiteDatabase) {
//                        super.onCreate(db)
//                        CoroutineScope(Dispatchers.IO).launch {
//                            val app = context.applicationContext
//                            val instance = INSTANCE ?: return@launch
//                            val gson = Gson()
//
//                            val classesJson = app.assets.open("json/classes.json").bufferedReader().use { it.readText() }
//                            val chaptersJson = app.assets.open("json/chapters.json").bufferedReader().use { it.readText() }
//                            val lessonsJson = app.assets.open("json/lessons.json").bufferedReader().use { it.readText() }
//
//                            val listClass = gson.fromJson(classesJson, Array<ClassMODEL>::class.java).toList().toList()
//                            val listChapter = gson.fromJson(chaptersJson, Array<ChapterMODEL>::class.java).toList().toList()
//                            val listLesson = gson.fromJson(lessonsJson, Array<LessonMODEL>::class.java).toList().toList()
//
//                            instance.withTransaction {
//                                instance.classDao().upsert(*listClass.toTypedArray())
//                                instance.chapterDao().upsert(*listChapter.toTypedArray())
//                                instance.lessonDao().upsert(*listLesson.toTypedArray())
//                            }
//                        }
//                    }
//                }).build()
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "physic_smart_app_database"
                )
                .createFromAsset("prepopulated/physic_smart_app_database.db")
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}