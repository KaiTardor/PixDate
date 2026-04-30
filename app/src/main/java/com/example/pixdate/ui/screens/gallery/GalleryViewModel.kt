package com.example.pixdate.ui.screens.gallery

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixdate.data.local.CsvDataSource
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoAnalysisEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.data.local.entity.PhotoTagCrossRef
import com.example.pixdate.data.local.entity.TagEntity
import com.example.pixdate.data.remote.AIVisionService
import com.example.pixdate.data.repository.PixDateRepository
import com.example.pixdate.ui.screens.detail.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class GalleryViewMode {
    CALENDAR,
    SEQUENTIAL
}

enum class PhotoFilter {
    ALL,
    PROCESSED,
    UNPROCESSED
}

/**
 * Modelo de UI para la pantalla de detalle.
 */
data class PhotoDetailInfo(
    val photo: PhotoEntity,
    val analysis: PhotoAnalysisEntity?,
    val folder: FolderEntity?,
    val tags: List<TagEntity>
)

class GalleryViewModel(
    private val repository: PixDateRepository
) : ViewModel() {

    companion object {
        private const val TAG_DB_UI = "PIXDATE_DB_UI"
        private const val TAG_AI = "PIXDATE_AI"
        private const val TAG_CSV = "PIXDATE_CSV"
        private const val TAG_CAMERA = "PIXDATE_CAM"
        private const val TAG_DB = "PIXDATE_DB"

        private const val CAMERA_FOLDER_NAME = "CÁMARA"
        private const val CAMERA_FOLDER_DESCRIPTION = "Fotos capturadas directamente desde la app"

        private const val AI_FOLDER_DESCRIPTION = "Carpeta auto-generada por IA"
        private const val AI_TAG_SOURCE = "AI"

        private const val AI_MODEL_USED = "gemini flask?"
    }

    // -------------------------------------------------------------------------
    // Filtros
    // -------------------------------------------------------------------------

    private val _photoFilter = MutableStateFlow(PhotoFilter.ALL)
    val photoFilter: StateFlow<PhotoFilter> = _photoFilter.asStateFlow()

    private val _folderFilter = MutableStateFlow<Long?>(null)

    /**
     * Filtra la galería por carpeta.
     *
     * null significa "sin filtro de carpeta".
     */
    fun setFolderFilter(folderId: Long?) {
        _folderFilter.value = folderId
    }

    /**
     * Cicla entre todos los filtros disponibles.
     */
    fun cycleFilter() {
        _photoFilter.value = when (_photoFilter.value) {
            PhotoFilter.ALL -> PhotoFilter.PROCESSED
            PhotoFilter.PROCESSED -> PhotoFilter.UNPROCESSED
            PhotoFilter.UNPROCESSED -> PhotoFilter.ALL
        }
    }

    fun setFilter(filter: PhotoFilter) {
        _photoFilter.value = filter
    }

    // -------------------------------------------------------------------------
    // Fotos agrupadas por fecha
    // -------------------------------------------------------------------------

    /**
     * Estado principal de fotos para la galería.
     */
    val groupedPhotos: StateFlow<Map<LocalDate, List<PhotoEntity>>> =
        combine(
            repository.getAllPhotosFlow(),
            _photoFilter,
            _folderFilter
        ) { photos, filter, folderId ->
            photos
                .filterByProcessingState(filter)
                .filterByFolder(folderId)
                .groupBy { photo ->
                    photo.dateTaken.toLocalDate()
                }
                .toSortedMap(compareByDescending { date -> date })
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    // -------------------------------------------------------------------------
    // Estado de vista/calendario
    // -------------------------------------------------------------------------

    private val _viewMode = MutableStateFlow(GalleryViewMode.CALENDAR)
    val viewMode: StateFlow<GalleryViewMode> = _viewMode.asStateFlow()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            GalleryViewMode.CALENDAR -> GalleryViewMode.SEQUENTIAL
            GalleryViewMode.SEQUENTIAL -> GalleryViewMode.CALENDAR
        }
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    /**
     * Cambia el mes visible y limpia el día seleccionado.
     */
    fun setYearMonth(yearMonth: YearMonth) {
        _currentYearMonth.value = yearMonth
        _selectedDate.value = null
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // -------------------------------------------------------------------------
    // Foto seleccionada y detalle
    // -------------------------------------------------------------------------

    private val _selectedPhoto = MutableStateFlow<PhotoEntity?>(null)
    val selectedPhoto: StateFlow<PhotoEntity?> = _selectedPhoto.asStateFlow()

    private val _selectedPhotoDetail = MutableStateFlow<PhotoDetailInfo?>(null)
    val selectedPhotoDetail: StateFlow<PhotoDetailInfo?> = _selectedPhotoDetail.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val aiService = AIVisionService()

    /**
     * Selecciona una foto y carga su detalle extendido.
     */
    fun selectPhoto(photo: PhotoEntity) {
        _selectedPhoto.value = photo
        _selectedPhotoDetail.value = null
        _processingState.value = ProcessingState.Idle

        loadPhotoDetail(photo)
    }

    /**
     * Limpia selección y estados asociados.
     */
    fun clearSelectedPhoto() {
        _selectedPhoto.value = null
        _selectedPhotoDetail.value = null
        _processingState.value = ProcessingState.Idle
    }

    /**
     * Resetea el estado de procesamiento a Idle.
     */
    fun resetProcessingState() {
        _processingState.value = ProcessingState.Idle
    }

    /**
     * Limpia solo el detalle, manteniendo la foto seleccionada.
     */
    fun clearPhotoDetail() {
        _selectedPhotoDetail.value = null
    }

    /**
     * Carga análisis, carpeta y tags de una foto.
     *
     * Si el usuario selecciona otra foto antes de que termine la consulta,
     * no sobrescribimos el detalle con datos obsoletos.
     */
    fun loadPhotoDetail(photo: PhotoEntity) {
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    val analysis = repository.getAnalysisByPhotoId(photo.photoId)
                    val folder = photo.folderId?.let { folderId ->
                        repository.getFolderById(folderId)
                    }
                    val tags = repository.getTagsByPhotoId(photo.photoId)

                    PhotoDetailInfo(
                        photo = photo,
                        analysis = analysis,
                        folder = folder,
                        tags = tags
                    )
                }

                if (_selectedPhoto.value?.photoId == photo.photoId) {
                    _selectedPhotoDetail.value = detail
                    logPhotoDetail(detail)
                }
            } catch (e: Exception) {
                Log.e(
                    TAG_DB_UI,
                    "Error cargando detalle de foto ${photo.photoId}: ${e.message}",
                    e
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Procesamiento IA
    // -------------------------------------------------------------------------

    /**
     * Procesa una foto con IA y persiste el resultado.
     *
     * Flujo:
     * 1. Lee los bytes de la imagen.
     * 2. Genera una descripción.
     * 3. Clasifica la descripción en categoría y tags.
     * 4. Guarda análisis, tags y relaciones.
     * 5. Crea/reutiliza carpeta de categoría.
     * 6. Marca la foto como procesada y guarda su folderId.
     * 7. Refresca la UI de detalle.
     */
    fun processPhoto(photo: PhotoEntity, context: Context) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading

            try {
                val appContext = context.applicationContext
                val now = System.currentTimeMillis()

                val imageBytes = readImageBytes(
                    context = appContext,
                    photo = photo
                )

                Log.d(TAG_AI, "Imagen leída: ${imageBytes.size} bytes")

                val existingFolders = withContext(Dispatchers.IO) {
                    repository.getAllFolders().map { it.name }
                }

                val analysis = withContext(Dispatchers.IO) {
                    aiService.analyze(imageBytes, existingFolders).getOrThrow()
                }

                Log.d(
                    TAG_AI,
                    "Análisis de Gemini: category=${analysis.category}, tags=${analysis.tags}"
                )

                // Si ya estaba procesada, entramos en modo comparación
                val currentDetail = _selectedPhotoDetail.value
                if (photo.isProcessed && currentDetail?.analysis != null) {
                    val oldAnalysis = AIVisionService.ImageAnalysis(
                        description = currentDetail.analysis.description ?: "",
                        category = currentDetail.analysis.mainCategory ?: "OTHER",
                        tags = currentDetail.tags.map { it.name }
                    )
                    _processingState.value = ProcessingState.Comparison(oldAnalysis, analysis)
                } else {
                    // Si es nueva, guardamos directamente
                    applyAnalysis(photo, analysis)
                }
            } catch (e: Exception) {
                Log.e(TAG_AI, "Error procesando ${photo.displayName}: ${e.message}", e)
                _processingState.value = ProcessingState.Error(
                    e.message ?: "Error desconocido"
                )
            }
        }
    }

    /**
     * Aplica un análisis de IA (nuevo o sobreescrito) a la base de datos.
     */
    fun applyAnalysis(photo: PhotoEntity, analysis: AIVisionService.ImageAnalysis) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading
            try {
                val now = System.currentTimeMillis()
                val updatedPhoto = withContext(Dispatchers.IO) {
                    saveAnalysis(
                        photo = photo,
                        description = analysis.description,
                        category = analysis.category,
                        now = now
                    )

                    saveTags(
                        photoId = photo.photoId,
                        tags = analysis.tags
                    )

                    val folderId = getOrCreateAiFolder(
                        category = analysis.category,
                        now = now
                    )

                    repository.updateProcessedStatusAndFolder(
                        photoId = photo.photoId,
                        isProcessed = true,
                        folderId = folderId
                    )

                    photo.copy(
                        isProcessed = true,
                        folderId = folderId,
                        updatedAt = now
                    )
                }

                _selectedPhoto.value = updatedPhoto
                _processingState.value = ProcessingState.Success(analysis.description)
                loadPhotoDetail(updatedPhoto)
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error("Error al guardar: ${e.message}")
            }
        }
    }

    /**
     * Actualización manual desde la UI de edición.
     */
    fun manualUpdateAnalysis(photo: PhotoEntity, description: String, tagsStr: String, category: String) {
        val tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val analysis = AIVisionService.ImageAnalysis(description, category, tags)
        applyAnalysis(photo, analysis)
    }

    // -------------------------------------------------------------------------
    // Importación inicial desde CSV
    // -------------------------------------------------------------------------

    /**
     * Importa las fotos de ejemplo desde assets si la base de datos está vacía.
     *
     * Está pensada para llamarse al inicio de la app. La comprobación de cantidad
     * evita duplicados en reinicios normales.
     */
    fun autoImportIfEmpty(context: Context) {
        viewModelScope.launch {
            try {
                val appContext = context.applicationContext

                val inserted = withContext(Dispatchers.IO) {
                    val count = repository.getPhotoCount()

                    if (count > 0) {
                        Log.d(TAG_CSV, "BD ya tiene $count fotos. No se reimporta.")
                        return@withContext 0
                    }

                    Log.d(TAG_CSV, "BD vacía. Importando CSV automáticamente...")

                    val csvDataSource = CsvDataSource()
                    val rows = csvDataSource.readCsvFromAssets(appContext)

                    if (rows.isEmpty()) {
                        0
                    } else {
                        repository.importFromCsv(rows)
                    }
                }

                if (inserted > 0) {
                    Log.d(TAG_CSV, "Auto-importación completada: $inserted fotos")
                }
            } catch (e: Exception) {
                Log.e(TAG_CSV, "Error en auto-importación: ${e.message}", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fotos capturadas con cámara
    // -------------------------------------------------------------------------

    /**
     * Inserta una foto recién capturada con la cámara.
     * Se guarda como no procesada y se asocia a la carpeta CÁMARA.
     */
    fun insertCapturedPhoto(uri: String, fileName: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                val photoId = withContext(Dispatchers.IO) {
                    val folderId = getOrCreateCameraFolder(now)

                    repository.insertPhoto(
                        PhotoEntity(
                            contentUri = uri,
                            dateTaken = now,
                            displayName = fileName,
                            mimeType = "image/jpeg",
                            isProcessed = false,
                            folderId = folderId,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

                Log.d(TAG_CAMERA, "Foto capturada insertada: photoId=$photoId, uri=$uri")
            } catch (e: Exception) {
                Log.e(TAG_CAMERA, "Error insertando foto capturada: ${e.message}", e)
            }
        }
    }

    /**
     * Marca una foto como no procesada.
     *
     * Nota: este método no borra análisis, tags ni carpeta. Solo cambia el flag.
     */
    fun markAsNotProcessed(photoId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProcessedStatus(photoId, false)
                }
            } catch (e: Exception) {
                Log.e(TAG_DB, "Error marcando foto como no procesada: ${e.message}", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private fun List<PhotoEntity>.filterByProcessingState(
        filter: PhotoFilter
    ): List<PhotoEntity> {
        return when (filter) {
            PhotoFilter.ALL -> this
            PhotoFilter.PROCESSED -> filter { photo -> photo.isProcessed }
            PhotoFilter.UNPROCESSED -> filter { photo -> !photo.isProcessed }
        }
    }

    private fun List<PhotoEntity>.filterByFolder(
        folderId: Long?
    ): List<PhotoEntity> {
        return if (folderId == null) {
            this
        } else {
            filter { photo -> photo.folderId == folderId }
        }
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private suspend fun readImageBytes(
        context: Context,
        photo: PhotoEntity
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            if (photo.contentUri.startsWith("file:///android_asset")) {
                val assetPath = "sample_images/${photo.displayName}"

                context.assets.open(assetPath).use { inputStream ->
                    inputStream.readBytes()
                }
            } else {
                val uri = Uri.parse(photo.contentUri)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: error("No se pudo leer la imagen")
            }
        }
    }

    private suspend fun saveAnalysis(
        photo: PhotoEntity,
        description: String,
        category: String,
        now: Long
    ) {
        repository.insertAnalysis(
            PhotoAnalysisEntity(
                photoId = photo.photoId,
                description = description,
                mainCategory = category,
                modelUsed = AI_MODEL_USED,
                processedAt = now,
                confidence = null,
                errorMessage = null
            )
        )
    }

    private suspend fun saveTags(
        photoId: Long,
        tags: List<String>
    ) {
        tags.forEach { rawTagName ->
            val tagName = rawTagName.trim()

            if (tagName.isNotEmpty()) {
                val existingTag = repository.getTagByName(tagName)

                val tagId = existingTag?.tagId ?: repository.insertTag(
                    TagEntity(
                        name = tagName,
                        source = AI_TAG_SOURCE
                    )
                )

                repository.insertPhotoTagCrossRef(
                    PhotoTagCrossRef(
                        photoId = photoId,
                        tagId = tagId
                    )
                )
            }
        }
    }

    private suspend fun getOrCreateAiFolder(
        category: String,
        now: Long
    ): Long {
        val folderName = category.trim().ifEmpty {
            "OTROS"
        }

        val existingFolder = repository.getFolderByName(folderName)

        return existingFolder?.folderId ?: repository.insertFolder(
            FolderEntity(
                name = folderName,
                description = AI_FOLDER_DESCRIPTION,
                isAutoGenerated = true,
                createdAt = now
            )
        )
    }

    private suspend fun getOrCreateCameraFolder(
        now: Long
    ): Long {
        val existingFolder = repository.getFolderByName(CAMERA_FOLDER_NAME)

        return existingFolder?.folderId ?: repository.insertFolder(
            FolderEntity(
                name = CAMERA_FOLDER_NAME,
                description = CAMERA_FOLDER_DESCRIPTION,
                isAutoGenerated = true,
                createdAt = now
            )
        )
    }

    private fun logPhotoDetail(detail: PhotoDetailInfo) {
        Log.d(TAG_DB_UI, "═══════════════════════════════════════════════")
        Log.d(TAG_DB_UI, "FOTO: ${detail.photo}")
        Log.d(TAG_DB_UI, "ANÁLISIS: ${detail.analysis}")
        Log.d(TAG_DB_UI, "CARPETA: ${detail.folder}")
        Log.d(TAG_DB_UI, "TAGS: ${detail.tags.map { tag -> tag.name }}")
        Log.d(TAG_DB_UI, "═══════════════════════════════════════════════")
    }
}

class GalleryViewModelFactory(
    private val repository: PixDateRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}