package com.example.pixdate.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pixdate.data.local.dao.*
import com.example.pixdate.data.local.entity.*

@Database(
    entities = [
        PhotoEntity::class,
        PhotoAnalysisEntity::class,
        TagEntity::class,
        PhotoTagCrossRef::class,
        FolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun photoAnalysisDao(): PhotoAnalysisDao
    abstract fun tagDao(): TagDao
    abstract fun photoTagCrossRefDao(): PhotoTagCrossRefDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixdate_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
