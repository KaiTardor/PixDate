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

    /** Inserta una nueva carpeta en la BD. Retorna su ID generado. */
    suspend fun insertFolder(folder: FolderEntity): Long {
        return folderDao.insertFolder(folder)
    }

    /** Inserta una nueva foto. Retorna su ID generado. */
    suspend fun insertPhoto(photo: PhotoEntity): Long {
        return photoDao.insertPhoto(photo)
    }

    /** Elimina una foto por su ID. */
    suspend fun deletePhoto(photoId: Long) {
        photoDao.deletePhoto(photoId)
    }

    /** Guarda el resultado del análisis de IA asociado a una foto. */
    suspend fun insertAnalysis(analysis: PhotoAnalysisEntity): Long {
        return photoAnalysisDao.insertAnalysis(analysis)
    }

    /** Crea un tag nuevo. Retorna su ID generado. */
    suspend fun insertTag(tag: TagEntity): Long {
        return tagDao.insertTag(tag)
    }

    /** Crea la relación (muchos a muchos) entre una foto y un tag. */
    suspend fun insertPhotoTagCrossRef(crossRef: PhotoTagCrossRef) {
        photoTagCrossRefDao.insertPhotoTagCrossRef(crossRef)
    }

    /** Borra todas las relaciones foto-tag de una foto antes de re-insertar tags nuevos. */
    suspend fun deleteTagsForPhoto(photoId: Long) {
        photoTagCrossRefDao.deleteTagsForPhoto(photoId)
    }

    // ── Lectura completa (Flow) ──────────────────────────────────

    /** Observa todas las fotos de la galería ordenadas por fecha de forma reactiva. */
    fun getAllPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getAllPhotosFlow()
    }

    /** Obtiene todas las fotos de forma síncrona (usado en WorkManagers o tareas puntuales). */
    suspend fun getAllPhotosSync(): List<PhotoEntity> {
        return photoDao.getAllPhotosSync()
    }

    // ── Lectura con filtro (Flow) ────────────────────────────────

    /** Observa todas las carpetas ordenadas alfabéticamente. */
    fun getAllFoldersFlow(): Flow<List<FolderEntity>> {
        return folderDao.getAllFoldersFlow()
    }

    // ── Actualizaciones ──────────────────────────────────────────

    /** Actualiza el estado de procesamiento de una foto y le asigna su carpeta. */
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

    /** Modifica los metadatos de una carpeta existente (ej: renombrar). */
    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    /** Elimina una carpeta por completo. */
    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    /** Limpia carpetas que han quedado vacías. */
    suspend fun deleteEmptyFolders() {
        folderDao.deleteEmptyFolders()
    }

    // ── Consultas auxiliares ──────────────────────────────────────

    /** Devuelve la lista completa de carpetas (síncrono). */
    suspend fun getAllFolders(): List<FolderEntity> {
        return folderDao.getAllFolders()
    }

    /** Busca el análisis de IA de una foto concreta. */
    suspend fun getAnalysisByPhotoId(photoId: Long): PhotoAnalysisEntity? {
        return photoAnalysisDao.getAnalysisByPhotoId(photoId)
    }

    /** Busca si un tag ya existe por su nombre. */
    suspend fun getTagByName(tagName: String): TagEntity? {
        return tagDao.getTagByName(tagName)
    }

    /** Busca si una carpeta ya existe por su nombre. */
    suspend fun getFolderByName(folderName: String): FolderEntity? {
        return folderDao.getFolderByName(folderName)
    }

    /** Obtiene todos los tags asociados a una foto. */
    suspend fun getTagsByPhotoId(photoId: Long): List<TagEntity> {
        return tagDao.getTagsByPhotoId(photoId)
    }

    /** Obtiene una carpeta por su ID único. */
    suspend fun getFolderById(folderId: Long): FolderEntity? {
        return folderDao.getFolderById(folderId)
    }

    /** Asigna una lista de fotos a una carpeta concreta. */
    suspend fun assignPhotosToFolder(photoIds: List<Long>, folderId: Long) {
        val now = System.currentTimeMillis()
        photoIds.forEach { photoId ->
            photoDao.assignFolder(photoId, folderId, now)
        }
    }
}
