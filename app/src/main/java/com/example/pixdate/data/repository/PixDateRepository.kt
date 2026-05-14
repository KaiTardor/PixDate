package com.example.pixdate.data.repository

import com.example.pixdate.data.local.dao.FolderDao
import com.example.pixdate.data.local.dao.PhotoAnalysisDao
import com.example.pixdate.data.local.dao.PhotoDao
import com.example.pixdate.data.local.dao.PhotoTagCrossRefDao
import com.example.pixdate.data.local.dao.TagDao
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoAnalysisEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.data.local.entity.PhotoTagCrossRef
import com.example.pixdate.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

class PixDateRepository(
    private val photoDao: PhotoDao,
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val tagDao: TagDao,
    private val photoTagCrossRefDao: PhotoTagCrossRefDao,
    private val folderDao: FolderDao
) {

    // ── Escritura individual ──────────────────────────────────────

    suspend fun insertFolder(folder: FolderEntity): Long {
        return folderDao.insertFolder(folder)
    }

    suspend fun insertPhoto(photo: PhotoEntity): Long {
        return photoDao.insertPhoto(photo)
    }

    suspend fun deletePhoto(photoId: Long) {
        photoDao.deletePhoto(photoId)
    }

    suspend fun insertAnalysis(analysis: PhotoAnalysisEntity): Long {
        return photoAnalysisDao.insertAnalysis(analysis)
    }

    suspend fun insertTag(tag: TagEntity): Long {
        return tagDao.insertTag(tag)
    }

    suspend fun insertPhotoTagCrossRef(crossRef: PhotoTagCrossRef) {
        photoTagCrossRefDao.insertPhotoTagCrossRef(crossRef)
    }

    /** Borra todas las relaciones foto-tag de una foto antes de re-insertar tags nuevos. */
    suspend fun deleteTagsForPhoto(photoId: Long) {
        photoTagCrossRefDao.deleteTagsForPhoto(photoId)
    }

    // ── Lectura completa (Flow) ──────────────────────────────────

    fun getAllPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getAllPhotosFlow()
    }

    suspend fun getAllPhotosSync(): List<PhotoEntity> {
        return photoDao.getAllPhotosSync()
    }

    // ── Lectura con filtro (Flow) ────────────────────────────────

    fun getAllFoldersFlow(): Flow<List<FolderEntity>> {
        return folderDao.getAllFoldersFlow()
    }

    // ── Actualizaciones ──────────────────────────────────────────

    suspend fun updateProcessedStatus(photoId: Long, isProcessed: Boolean) {
        photoDao.updateProcessedStatus(
            photoId = photoId,
            isProcessed = isProcessed,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateProcessedStatusAndFolder(
        photoId: Long,
        isProcessed: Boolean,
        folderId: Long?
    ) {
        photoDao.updateProcessedStatusAndFolder(
            photoId = photoId,
            isProcessed = isProcessed,
            folderId = folderId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    suspend fun deleteEmptyFolders() {
        folderDao.deleteEmptyFolders()
    }

    // ── Consultas auxiliares ──────────────────────────────────────

    suspend fun getAllFolders(): List<FolderEntity> {
        return folderDao.getAllFolders()
    }

    suspend fun getAnalysisByPhotoId(photoId: Long): PhotoAnalysisEntity? {
        return photoAnalysisDao.getAnalysisByPhotoId(photoId)
    }

    suspend fun getTagByName(tagName: String): TagEntity? {
        return tagDao.getTagByName(tagName)
    }

    suspend fun getFolderByName(folderName: String): FolderEntity? {
        return folderDao.getFolderByName(folderName)
    }


    suspend fun getTagsByPhotoId(photoId: Long): List<TagEntity> {
        return tagDao.getTagsByPhotoId(photoId)
    }

    suspend fun getFolderById(folderId: Long): FolderEntity? {
        return folderDao.getFolderById(folderId)
    }
}
