package com.example.pixdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pixdate.data.local.entity.TagEntity

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :tagName LIMIT 1")
    suspend fun getTagByName(tagName: String): TagEntity?

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN photo_tag_cross_ref ref ON t.tagId = ref.tagId
        WHERE ref.photoId = :photoId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsByPhotoId(photoId: Long): List<TagEntity>
}
