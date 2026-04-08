package com.example.pixdate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_analysis",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["photoId"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["photoId"], unique = true)]
)
data class PhotoAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val analysisId: Long = 0,

    val photoId: Long,
    val description: String? = null,
    val mainCategory: String? = null,
    val modelUsed: String? = null,
    val processedAt: Long? = null,
    val confidence: Float? = null,
    val errorMessage: String? = null
)
