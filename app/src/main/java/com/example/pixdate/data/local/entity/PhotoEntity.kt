package com.example.pixdate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val photoId: Long = 0,

    val contentUri: String,
    val dateTaken: Long,
    val displayName: String,
    val mimeType: String? = null,
    val isProcessed: Boolean = false,
    val folderId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
