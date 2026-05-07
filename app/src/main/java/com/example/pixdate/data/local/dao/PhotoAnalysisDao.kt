package com.example.pixdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pixdate.data.local.entity.PhotoAnalysisEntity

@Dao
interface PhotoAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: PhotoAnalysisEntity): Long

    @Query("SELECT * FROM photo_analysis WHERE photoId = :photoId LIMIT 1")
    suspend fun getAnalysisByPhotoId(photoId: Long): PhotoAnalysisEntity?
}
