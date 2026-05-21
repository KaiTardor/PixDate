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

    @Query("DELETE FROM photos WHERE photoId = :photoId")
    suspend fun deletePhoto(photoId: Long)

    @Query("SELECT * FROM photos ORDER BY dateTaken DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotosSync(): List<PhotoEntity>

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
