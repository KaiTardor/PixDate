package com.example.pixdate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "photo_tag_cross_ref",
    primaryKeys = ["photoId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["photoId"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photoId"]),
        Index(value = ["tagId"])
    ]
)
data class PhotoTagCrossRef(
    val photoId: Long,
    val tagId: Long
)
