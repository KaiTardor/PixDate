package com.example.pixdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pixdate.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos ORDER BY dateTaken DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>


    @Query("UPDATE photos SET isProcessed = :isProcessed, updatedAt = :updatedAt WHERE photoId = :photoId")
    suspend fun updateProcessedStatus(photoId: Long, isProcessed: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getPhotoCount(): Int

    @Query("""
    UPDATE photos
    SET isProcessed = :isProcessed,
        folderId = :folderId,
        updatedAt = :updatedAt
    WHERE photoId = :photoId
""")
    suspend fun updateProcessedStatusAndFolder(
        photoId: Long,
        isProcessed: Boolean,
        folderId: Long?,
        updatedAt: Long
    )
}
