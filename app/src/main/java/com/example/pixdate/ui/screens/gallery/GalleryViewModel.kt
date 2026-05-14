package com.example.pixdate.ui.screens.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoAnalysisEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.data.local.entity.PhotoTagCrossRef
import com.example.pixdate.data.local.entity.TagEntity
import com.example.pixdate.data.remote.AIVisionService
import com.example.pixdate.data.repository.PixDateRepository
import com.example.pixdate.ui.screens.detail.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

/**
 * Análisis pendiente de revisión por el usuario.
 * Se almacena en memoria cuando la IA termina pero el usuario ya no está viendo esa foto.
 */
data class PendingAnalysis(
    val photoId: Long,
    val photoName: String,
    val analysis: AIVisionService.ImageAnalysis,
    val isReprocessed: Boolean,
    val oldAnalysis: AIVisionService.ImageAnalysis?
)

/**
 * Evento emitido cuando la IA termina de analizar una foto o falla.
 * La UI lo observa para decidir si mostrar Snackbar o notificación del sistema.
 */
data class AnalysisCompletedEvent(
    val photoId: Long,
    val photoName: String,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

class GalleryViewModel(
    private val repository: PixDateRepository,
    private val prefs: android.content.SharedPreferences
) : ViewModel() {

    companion object {
        private const val TAG_AI = "PIXDATE_AI"
        private const val TAG_CAMERA = "PIXDATE_CAM"
        private const val TAG_DB = "PIXDATE_DB"

        private const val CAMERA_FOLDER_NAME = "CÁMARA"
        private const val CAMERA_FOLDER_DESCRIPTION = "Fotos capturadas directamente desde la app"

        private const val IMPORTED_FOLDER_NAME = "IMPORTADAS"
        private const val IMPORTED_FOLDER_DESCRIPTION = "Fotos importadas desde la galería nativa"

        private const val AI_FOLDER_DESCRIPTION = "Carpeta auto-generada por IA"
        private const val AI_TAG_SOURCE = "AI"

        private const val AI_MODEL_USED = "gemini-2.5-flash"
    }

    // -------------------------------------------------------------------------
    // Filtros
    // -------------------------------------------------------------------------

    private val _photoFilter = MutableStateFlow(PhotoFilter.ALL)
    val photoFilter: StateFlow<PhotoFilter> = _photoFilter.asStateFlow()

    fun setFilter(filter: PhotoFilter) {
        _photoFilter.value = filter
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    var isAppInForeground: Boolean = true

    // -------------------------------------------------------------------------
    // Fotos agrupadas por fecha
    // -------------------------------------------------------------------------

    /**
     * Estado principal de fotos para la galería.
     */
    val groupedPhotos: StateFlow<Map<LocalDate, List<PhotoEntity>>> =
        repository.getAllPhotos()
            .combine(_photoFilter) { photos, filter ->
                photos
                    .filterByProcessingState(filter)
                    .groupBy { photo -> photo.dateTaken.toLocalDate() }
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

    fun nextWeek() {
        val current = _selectedDate.value ?: LocalDate.now()
        val next = current.plusWeeks(1)
        _selectedDate.value = next
        _currentYearMonth.value = YearMonth.from(next)
    }

    fun prevWeek() {
        val current = _selectedDate.value ?: LocalDate.now()
        val prev = current.minusWeeks(1)
        _selectedDate.value = prev
        _currentYearMonth.value = YearMonth.from(prev)
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

    /** Mapa de Jobs activos indexados por photoId para soportar procesamiento concurrente. */
    private val processingJobs = mutableMapOf<Long, Job>()

    /** Mapa de análisis pendientes de revisión, indexado por photoId. */
    private val _pendingAnalyses = MutableStateFlow<Map<Long, PendingAnalysis>>(emptyMap())
    val pendingAnalyses: StateFlow<Map<Long, PendingAnalysis>> = _pendingAnalyses.asStateFlow()

    /** Evento one-shot emitido cuando la IA termina un análisis. */
    private val _analysisCompletedEvent = MutableSharedFlow<AnalysisCompletedEvent>(extraBufferCapacity = 5)
    val analysisCompletedEvent: SharedFlow<AnalysisCompletedEvent> = _analysisCompletedEvent.asSharedFlow()

    private val aiService = AIVisionService()

    init {
        loadPendingAnalysesFromPrefs()
    }

    private fun loadPendingAnalysesFromPrefs() {
        val saved = prefs.getString("pending_analyses", null)
        if (saved != null) {
            try {
                val jsonArray = org.json.JSONArray(saved)
                val map = mutableMapOf<Long, PendingAnalysis>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    
                    val tagsArray = obj.getJSONArray("tags")
                    val tags = mutableListOf<String>()
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                    val analysis = AIVisionService.ImageAnalysis(
                        description = obj.getString("desc"),
                        category = obj.getString("cat"),
                        tags = tags
                    )
                    
                    val oldAnalysisObj = obj.optJSONObject("oldAnalysis")
                    val oldAnalysis = if (oldAnalysisObj != null) {
                        val oldTagsArray = oldAnalysisObj.getJSONArray("tags")
                        val oldTags = mutableListOf<String>()
                        for (j in 0 until oldTagsArray.length()) {
                            oldTags.add(oldTagsArray.getString(j))
                        }
                        AIVisionService.ImageAnalysis(
                            description = oldAnalysisObj.getString("desc"),
                            category = oldAnalysisObj.getString("cat"),
                            tags = oldTags
                        )
                    } else null

                    val pending = PendingAnalysis(
                        photoId = obj.getLong("id"),
                        photoName = obj.getString("name"),
                        analysis = analysis,
                        isReprocessed = obj.getBoolean("isReprocessed"),
                        oldAnalysis = oldAnalysis
                    )
                    map[pending.photoId] = pending
                }
                _pendingAnalyses.value = map
            } catch (e: Exception) {
                Log.e(TAG_AI, "Error loading pending analyses", e)
            }
        }
    }

    private fun savePendingAnalysesToPrefs() {
        try {
            val jsonArray = org.json.JSONArray()
            for ((_, pending) in _pendingAnalyses.value) {
                val obj = org.json.JSONObject()
                obj.put("id", pending.photoId)
                obj.put("name", pending.photoName)
                obj.put("isReprocessed", pending.isReprocessed)
                
                obj.put("desc", pending.analysis.description)
                obj.put("cat", pending.analysis.category)
                val tagsArray = org.json.JSONArray()
                pending.analysis.tags.forEach { tagsArray.put(it) }
                obj.put("tags", tagsArray)
                
                if (pending.oldAnalysis != null) {
                    val oldObj = org.json.JSONObject()
                    oldObj.put("desc", pending.oldAnalysis.description)
                    oldObj.put("cat", pending.oldAnalysis.category)
                    val oldTagsArray = org.json.JSONArray()
                    pending.oldAnalysis.tags.forEach { oldTagsArray.put(it) }
                    oldObj.put("tags", oldTagsArray)
                    obj.put("oldAnalysis", oldObj)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("pending_analyses", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG_AI, "Error saving pending analyses", e)
        }
    }

    /**
     * Selecciona una foto y carga su detalle extendido.
     */
    fun selectPhoto(photo: PhotoEntity) {
        _selectedPhoto.value = photo
        _selectedPhotoDetail.value = null

        // Si existe un análisis pendiente para esta foto, restaurar el estado de comparación
        val pending = _pendingAnalyses.value[photo.photoId]
        if (pending != null) {
            if (pending.isReprocessed && pending.oldAnalysis != null) {
                _processingState.value = ProcessingState.Comparison(pending.oldAnalysis, pending.analysis)
            } else {
                _processingState.value = ProcessingState.Comparison(
                    AIVisionService.ImageAnalysis("", "NUEVO", emptyList()),
                    pending.analysis
                )
            }
        } else {
            _processingState.value = ProcessingState.Idle
        }

        loadPhotoDetail(photo)
    }

    /**
     * Limpia selección y estados asociados.
     * NO cancela el procesamiento en curso: la IA sigue en segundo plano.
     */
    fun clearSelectedPhoto() {
        _selectedPhoto.value = null
        _selectedPhotoDetail.value = null
        // Solo reseteamos el state visual
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
                    TAG_DB,
                    "Error cargando detalle de foto ${photo.photoId}: ${e.message}",
                    e
                )
            }
        }
    }

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
        val photoId = photo.photoId
        processingJobs[photoId]?.cancel()
        val job = viewModelScope.launch {
            // Solo actualizamos la UI visual a Loading si es la foto seleccionada
            if (_selectedPhoto.value?.photoId == photoId) {
                _processingState.value = ProcessingState.Loading
            }

            try {
                val appContext = context.applicationContext

                val imageBytes = readImageBytes(
                    context = appContext,
                    photo = photo
                )

                Log.d(TAG_AI, "Imagen leída: ${imageBytes.size} bytes")

                // Comprimimos antes de codificar en Base64 para reducir el payload
                val compressedBytes = compressImageForAI(imageBytes)
                Log.d(TAG_AI, "Imagen comprimida: ${compressedBytes.size} bytes (${compressedBytes.size * 100 / imageBytes.size}% del original)")

                val existingFolders = withContext(Dispatchers.IO) {
                    repository.getAllFolders().map { it.name }
                }

                // Pasamos el MIME type real de la foto en lugar de asumir siempre JPEG
                val mimeType = photo.mimeType ?: "image/jpeg"

                val analysis = withContext(Dispatchers.IO) {
                    aiService.analyze(compressedBytes, existingFolders, mimeType).getOrThrow()
                }

                Log.d(
                    TAG_AI,
                    "Análisis de Gemini: category=${analysis.category}, tags=${analysis.tags}"
                )

                // Comprobamos si el usuario sigue viendo esta foto
                val currentSelectedId = _selectedPhoto.value?.photoId
                val userIsViewing = currentSelectedId == photo.photoId

                if (userIsViewing) {
                    // El usuario sigue aquí: mostramos comparación directamente
                    val currentDetail = _selectedPhotoDetail.value
                    if (photo.isProcessed && currentDetail?.analysis != null) {
                        val oldAnalysis = AIVisionService.ImageAnalysis(
                            description = currentDetail.analysis.description ?: "",
                            category = currentDetail.analysis.mainCategory ?: "OTHER",
                            tags = currentDetail.tags.map { it.name }
                        )
                        _processingState.value = ProcessingState.Comparison(oldAnalysis, analysis)
                    } else {
                        applyAnalysis(photo, analysis)
                    }
                } else {
                    // El usuario salió: guardamos en el mapa de pendientes obteniendo la info antigua de BD
                    val dbAnalysis = repository.getAnalysisByPhotoId(photo.photoId)
                    val dbTags = repository.getTagsByPhotoId(photo.photoId)
                    
                    val isReprocessed = photo.isProcessed && dbAnalysis != null
                    val oldAnalysis = if (isReprocessed && dbAnalysis != null) {
                        AIVisionService.ImageAnalysis(
                            description = dbAnalysis.description ?: "",
                            category = dbAnalysis.mainCategory ?: "OTHER",
                            tags = dbTags.map { it.name }
                        )
                    } else null

                    val pending = PendingAnalysis(
                        photoId = photo.photoId,
                        photoName = photo.displayName ?: "Foto",
                        analysis = analysis,
                        isReprocessed = isReprocessed,
                        oldAnalysis = oldAnalysis
                    )

                    _pendingAnalyses.value = _pendingAnalyses.value + (photo.photoId to pending)
                    savePendingAnalysesToPrefs()
                    _processingState.value = ProcessingState.Idle

                    if (isAppInForeground) {
                        // Emitimos evento para que la UI lance Snackbar
                        _analysisCompletedEvent.emit(
                            AnalysisCompletedEvent(
                                photoId = photo.photoId,
                                photoName = photo.displayName ?: "Foto",
                                isSuccess = true
                            )
                        )
                    } else {
                        // Enviar notificación del sistema si la app está en fondo
                        com.example.pixdate.notifications.NotificationHelper.showAnalysisCompleteNotification(
                            context = context,
                            photoId = photo.photoId,
                            photoName = photo.displayName ?: "Foto",
                            isSuccess = true
                        )
                    }

                    Log.d(TAG_AI, "Análisis guardado como pendiente para foto ${photo.photoId}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (_selectedPhoto.value?.photoId == photoId) {
                    _processingState.value = ProcessingState.Idle
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG_AI, "Error procesando ${photo.displayName}: ${e.message}", e)
                val isViewing = _selectedPhoto.value?.photoId == photoId

                if (isViewing) {
                    _processingState.value = ProcessingState.Error(e.message ?: "Error desconocido")
                } else {
                    if (isAppInForeground) {
                        _analysisCompletedEvent.emit(
                            AnalysisCompletedEvent(
                                photoId = photoId,
                                photoName = photo.displayName ?: "Foto",
                                isSuccess = false,
                                errorMessage = e.message
                            )
                        )
                    } else {
                        com.example.pixdate.notifications.NotificationHelper.showAnalysisCompleteNotification(
                            context = context,
                            photoId = photoId,
                            photoName = photo.displayName ?: "Foto",
                            isSuccess = false,
                            errorMessage = e.message
                        )
                    }
                }
            } finally {
                processingJobs.remove(photoId)
            }
        }
        processingJobs[photoId] = job
    }

    /**
     * Aplica un análisis de IA (nuevo o sobreescrito) a la base de datos.
     * También limpia el análisis pendiente asociado si existía.
     */
    fun applyAnalysis(photo: PhotoEntity, analysis: AIVisionService.ImageAnalysis) {
        val photoId = photo.photoId
        processingJobs[photoId]?.cancel()
        val job = viewModelScope.launch {
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

                // Limpiamos el análisis pendiente si existía
                clearPendingAnalysis(photo.photoId)
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error("Error al guardar: ${e.message}")
            } finally {
                processingJobs.remove(photoId)
            }
        }
        processingJobs[photoId] = job
    }

    /**
     * Elimina un análisis pendiente del mapa (tras aceptarlo o descartarlo).
     */
    fun clearPendingAnalysis(photoId: Long) {
        _pendingAnalyses.value = _pendingAnalyses.value - photoId
        savePendingAnalysesToPrefs()
    }

    /**
     * Actualización manual desde la UI de edición.
     */
    fun manualUpdateAnalysis(photo: PhotoEntity, description: String, tagsStr: String, category: String) {
        val tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val analysis = AIVisionService.ImageAnalysis(description, category, tags)
        applyAnalysis(photo, analysis)
    }

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

    private var syncJob: kotlinx.coroutines.Job? = null

    /**
     * Sincroniza todas las imágenes del dispositivo desde MediaStore hacia la base de datos de Room.
     */
    fun syncMediaStore(context: Context) {
        if (syncJob?.isActive == true) {
            Log.d(TAG_DB, "Sincronización ya en curso. Omitiendo...")
            return
        }

        _isSyncing.value = true
        _syncProgress.value = 0f

        syncJob = viewModelScope.launch {
            try {
                Log.d(TAG_DB, "Iniciando sincronización de MediaStore...")
                _syncProgress.value = 0.1f
                
                // 1. Limpiar duplicados que se hayan colado previamente por doble ejecución
                val initialDbPhotos = repository.getAllPhotosSync()
                
                val uniqueUris = mutableSetOf<String>()
                val duplicatesToDelete = mutableListOf<Long>()
                
                for (photo in initialDbPhotos) {
                    if (photo.contentUri in uniqueUris) {
                        duplicatesToDelete.add(photo.photoId)
                    } else {
                        uniqueUris.add(photo.contentUri)
                    }
                }
                
                if (duplicatesToDelete.isNotEmpty()) {
                    Log.d(TAG_DB, "Limpiando ${duplicatesToDelete.size} fotos duplicadas...")
                    withContext(Dispatchers.IO) {
                        duplicatesToDelete.forEach { id ->
                            repository.deletePhoto(id)
                        }
                    }
                }

                // 2. Traer imágenes locales y de la BD ya limpia
                val localImages = com.example.pixdate.data.local.MediaStoreHelper.getAllImages(context)
                val dbPhotos = repository.getAllPhotosSync()
                _syncProgress.value = 0.4f
                
                // Mapeamos las URIs existentes para búsqueda rápida
                val localImageUris = localImages.map { it.uriString }.toSet()
                val existingUris = dbPhotos.map { it.contentUri }.toSet()
                
                // 3. Eliminar de Room las fotos de MediaStore que el usuario haya borrado de su dispositivo
                val photosToDelete = dbPhotos.filter { 
                    it.contentUri.startsWith("content://media/") && it.contentUri !in localImageUris 
                }
                
                if (photosToDelete.isNotEmpty()) {
                    Log.d(TAG_DB, "Eliminando ${photosToDelete.size} fotos que ya no están en el dispositivo.")
                    withContext(Dispatchers.IO) {
                        photosToDelete.forEach { photo ->
                            repository.deletePhoto(photo.photoId)
                        }
                    }
                }
                
                // 4. Insertar las nuevas
                val newImages = localImages.filter { it.uriString !in existingUris }
                
                if (newImages.isNotEmpty()) {
                    Log.d(TAG_DB, "Encontradas ${newImages.size} imágenes nuevas para sincronizar.")
                    
                    withContext(Dispatchers.IO) {
                        newImages.forEach { mediaImage ->
                            repository.insertPhoto(
                                PhotoEntity(
                                    contentUri = mediaImage.uriString,
                                    dateTaken = mediaImage.dateAdded,
                                    displayName = mediaImage.displayName,
                                    mimeType = mediaImage.mimeType,
                                    isProcessed = false,
                                    folderId = null,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                
                // Marcamos el final coordinado
                _syncProgress.value = 1.0f
                delay(300) // Pequeña pausa para que se vea el 100%
                
                Log.d(TAG_DB, "Sincronización completada con éxito.")
            } catch (e: Exception) {
                Log.e(TAG_DB, "Error sincronizando MediaStore: ${e.message}", e)
            } finally {
                _isSyncing.value = false
                _syncProgress.value = 0f
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
            val uri = Uri.parse(photo.contentUri)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: error("No se pudo leer la imagen")
        }
    }

    /**
     * Escala la imagen al máximo [maxSizePx] en su lado más largo y la recomprime
     * como JPEG al [quality]% indicado.
     *
     * Si los bytes no son decodificables como Bitmap (formato desconocido),
     * se devuelven sin modificar.
     */
    private suspend fun compressImageForAI(
        bytes: ByteArray,
        maxSizePx: Int = 1024,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.Default) {
        // Pasada 1: leer dimensiones sin decodificar píxeles (coste ~0 de RAM)
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return@withContext bytes  // formato no soportado
        }

        // Pasada 2: decodificar con inSampleSize para reducir RAM desde el origen
        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxSizePx)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return@withContext bytes

        // Pasada 3: ajuste fino si la imagen, tras el muestreo, sigue siendo mayor que maxSizePx
        val ratio = minOf(
            maxSizePx.toFloat() / sampled.width,
            maxSizePx.toFloat() / sampled.height,
            1f
        )
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                sampled,
                (sampled.width * ratio).toInt(),
                (sampled.height * ratio).toInt(),
                true
            )
        } else {
            sampled
        }

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            // Liberamos la memoria nativa de los bitmaps explícitamente en lugar
            // de esperar al GC, ya que el byte array resultante es lo único que necesitamos.
            if (scaled !== sampled) scaled.recycle()
            sampled.recycle()
            out.toByteArray()
        }
    }

    /**
     * Calcula el [inSampleSize] óptimo para decodificar una imagen directamente
     * a una resolución inferior o igual a [maxPx] en su dimensión más larga.
     *
     * inSampleSize debe ser potencia de 2 para que BitmapFactory lo aplique eficientemente.
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxPx: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)
        while (maxDimension / (sampleSize * 2) > maxPx) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Guarda el análisis de IA en la base de datos, asociado a la foto.
     */
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

    /**
     * Guarda los tags asociados a una foto, creando los tags si no existen y estableciendo la relación.
     */
    private suspend fun saveTags(
        photoId: Long,
        tags: List<String>
    ) {
        // Borramos primero los tags anteriores para evitar que se acumulen
        // cuando una foto se re-analiza con resultados diferentes.
        repository.deleteTagsForPhoto(photoId)

        tags.forEach { rawTagName ->
            val tagName = rawTagName.trim().lowercase()

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

    /**
     * Obtiene el folderId de la carpeta de categoría generada por IA, creando la carpeta si no existe.
     */
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

    /**
     * Obtiene el folderId de la carpeta CÁMARA, creando la carpeta si no existe.
     */
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

    /**
     * Loguea en detalle la información de una foto, incluyendo su análisis, carpeta y tags.
     */
    private fun logPhotoDetail(detail: PhotoDetailInfo) {
        Log.d(TAG_DB, "═══════════════════════════════════════════════")
        Log.d(TAG_DB, "FOTO: ${detail.photo}")
        Log.d(TAG_DB, "ANÁLISIS: ${detail.analysis}")
        Log.d(TAG_DB, "CARPETA: ${detail.folder}")
        Log.d(TAG_DB, "TAGS: ${detail.tags.map { tag -> tag.name }}")
        Log.d(TAG_DB, "═══════════════════════════════════════════════")
    }
}

/**
 * Factory para crear el GalleryViewModel con su dependencia de repositorio.
 */
class GalleryViewModelFactory(
    private val repository: PixDateRepository,
    private val prefs: android.content.SharedPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(repository, prefs) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}