package com.example.pixdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pixdate.data.local.entity.PhotoTagCrossRef

@Dao
interface PhotoTagCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoTagCrossRef(crossRef: PhotoTagCrossRef)

    @Query("SELECT * FROM photo_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<PhotoTagCrossRef>

    @Query("SELECT * FROM photo_tag_cross_ref WHERE photoId = :photoId")
    suspend fun getTagsForPhoto(photoId: Long): List<PhotoTagCrossRef>
}
