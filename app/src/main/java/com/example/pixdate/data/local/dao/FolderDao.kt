package com.example.pixdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pixdate.data.local.entity.FolderEntity

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE name = :folderName LIMIT 1")
    suspend fun getFolderByName(folderName: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE folderId = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FolderEntity?
}
