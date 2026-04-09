package com.example.pixdate.ui.screens.gallery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixdate.data.local.CsvDataSource
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoAnalysisEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.data.local.entity.TagEntity
import com.example.pixdate.data.repository.PixDateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class GalleryViewMode {
    CALENDAR,
    SEQUENTIAL
}

/**
 * Contiene toda la información relacionada con una foto en la BD.
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

    // ── Estado reactivo de fotos agrupadas ───────────────────────

    // Convierte el flow plano original en un Map agrupado y ordenado
    val groupedPhotos: StateFlow<Map<LocalDate, List<PhotoEntity>>> =
        repository.getAllPhotosFlow()
            .map { list ->
                list.groupBy {
                    Instant.ofEpochMilli(it.dateTaken)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.toSortedMap(compareByDescending { it }) // Las más recientes primero
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    // ── Modos de Vista y Calendario ──────────────────────────────

    private val _viewMode = MutableStateFlow(GalleryViewMode.CALENDAR)
    val viewMode: StateFlow<GalleryViewMode> = _viewMode.asStateFlow()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == GalleryViewMode.CALENDAR) {
            GalleryViewMode.SEQUENTIAL
        } else {
            GalleryViewMode.CALENDAR
        }
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // ── Detalle de foto seleccionada ─────────────────────────────

    private val _selectedPhotoDetail = MutableStateFlow<PhotoDetailInfo?>(null)
    val selectedPhotoDetail: StateFlow<PhotoDetailInfo?> = _selectedPhotoDetail.asStateFlow()

    /**
     * Carga toda la información relacionada con una foto desde Room:
     * - PhotoEntity (la propia foto)
     * - PhotoAnalysisEntity (descripción, categoría, modelo…)
     * - FolderEntity (carpeta a la que pertenece)
     * - List<TagEntity> (etiquetas asociadas via cross-ref)
     */
    fun loadPhotoDetail(photo: PhotoEntity) {
        viewModelScope.launch {
            val analysis = repository.getAnalysisByPhotoId(photo.photoId)
            val folder = photo.folderId?.let { repository.getFolderById(it) }
            val tags = repository.getTagsByPhotoId(photo.photoId)

            val detail = PhotoDetailInfo(
                photo = photo,
                analysis = analysis,
                folder = folder,
                tags = tags
            )

            _selectedPhotoDetail.value = detail

            // También logear todo en Logcat
            Log.d("PIXDATE_DB_UI", "═══════════════════════════════════════════════")
            Log.d("PIXDATE_DB_UI", "FOTO: $photo")
            Log.d("PIXDATE_DB_UI", "ANÁLISIS: $analysis")
            Log.d("PIXDATE_DB_UI", "CARPETA: $folder")
            Log.d("PIXDATE_DB_UI", "TAGS: ${tags.map { it.name }}")
            Log.d("PIXDATE_DB_UI", "═══════════════════════════════════════════════")
        }
    }

    fun clearPhotoDetail() {
        _selectedPhotoDetail.value = null
    }

    // ── Auto-importar CSV al iniciar si la BD está vacía ─────────

    fun autoImportIfEmpty(context: Context) {
        viewModelScope.launch {
            try {
                val count = repository.getPhotoCount()
                if (count > 0) {
                    Log.d("PIXDATE_CSV", "BD ya tiene $count fotos, no se reimporta")
                    return@launch
                }

                Log.d("PIXDATE_CSV", "BD vacía, importando CSV automáticamente...")
                val csvDataSource = CsvDataSource()
                val rows = csvDataSource.readCsvFromAssets(context)

                if (rows.isNotEmpty()) {
                    val inserted = repository.importFromCsv(rows)
                    Log.d("PIXDATE_CSV", "Auto-importación completada: $inserted fotos")
                }
            } catch (e: Exception) {
                Log.e("PIXDATE_CSV", "Error en auto-importación: ${e.message}", e)
            }
        }
    }

    // ── Insertar ejemplo manual (botón temporal +) ───────────────

    fun insertSamplePhoto() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val folderId = repository.insertFolder(
                FolderEntity(
                    name = "ANIMALS_$now",
                    description = "Carpeta generada para prueba",
                    isAutoGenerated = true,
                    createdAt = now
                )
            )

            val photoId = repository.insertPhoto(
                PhotoEntity(
                    contentUri = "file:///android_asset/sample_images/3354474353_daf9e168cf.jpg",
                    dateTaken = now,
                    displayName = "3354474353_daf9e168cf.jpg",
                    mimeType = "image/jpeg",
                    isProcessed = true,
                    folderId = folderId,
                    createdAt = now,
                    updatedAt = now
                )
            )

            repository.insertAnalysis(
                PhotoAnalysisEntity(
                    photoId = photoId,
                    description = "Two dogs walk in the snow, the larger dog has a fish in his mouth.",
                    mainCategory = "ANIMALS",
                    modelUsed = "Salesforce/blip-image-captioning-base",
                    processedAt = now,
                    confidence = 0.91f,
                    errorMessage = null
                )
            )

            Log.d("PIXDATE_DB", "Ejemplo insertado: photoId=$photoId, folderId=$folderId")
        }
    }

    // ── Insertar foto capturada con la cámara ────────────────────

    /**
     * Inserta una foto recién capturada por la cámara en Room.
     * Se guarda como no procesada (pendiente de análisis futuro).
     * La carpeta "CÁMARA" se crea una sola vez (idempotente).
     *
     * @param uri URI (content://) de la foto guardada por FileProvider.
     * @param fileName Nombre del archivo de la foto.
     */
    fun insertCapturedPhoto(uri: String, fileName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Buscar o crear la carpeta CÁMARA (idempotente)
            val folder = repository.getFolderByName("CÁMARA")
            val folderId = folder?.folderId ?: repository.insertFolder(
                FolderEntity(
                    name = "CÁMARA",
                    description = "Fotos capturadas directamente desde la app",
                    isAutoGenerated = true,
                    createdAt = now
                )
            )

            val photoId = repository.insertPhoto(
                PhotoEntity(
                    contentUri = uri,
                    dateTaken = now,
                    displayName = fileName,
                    mimeType = "image/jpeg",
                    isProcessed = false, // Pendiente de análisis
                    folderId = folderId,
                    createdAt = now,
                    updatedAt = now
                )
            )

            Log.d("PIXDATE_CAM", "Foto capturada insertada: photoId=$photoId, uri=$uri")
        }
    }

    fun markAsNotProcessed(photoId: Long) {
        viewModelScope.launch {
            repository.updateProcessedStatus(photoId, false)
        }
    }
}

class GalleryViewModelFactory(
    private val repository: PixDateRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GalleryViewModel(repository) as T
    }
}