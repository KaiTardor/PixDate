package com.example.pixdate.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixdate.data.local.database.AppDatabase
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.repository.PixDateRepository
import com.example.pixdate.ui.screens.detail.PhotoDetailScreen
import com.example.pixdate.ui.screens.folders.FolderDetailScreen
import com.example.pixdate.ui.screens.folders.FoldersScreen
import com.example.pixdate.ui.screens.folders.FoldersViewModel
import com.example.pixdate.ui.screens.folders.FoldersViewModelFactory
import com.example.pixdate.ui.screens.gallery.GalleryScreen
import com.example.pixdate.ui.screens.gallery.GalleryViewMode
import com.example.pixdate.ui.screens.gallery.GalleryViewModel
import com.example.pixdate.ui.screens.gallery.GalleryViewModelFactory
import com.example.pixdate.ui.screens.gallery.PhotoFilter
import com.example.pixdate.ui.screens.legal.LegalScreen
import com.example.pixdate.notifications.NotificationHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Secciones principales de la aplicación.
 */
enum class BottomSection {
    GALLERY,
    CAMERA,
    FOLDERS,
    LEGAL
}

@Composable
fun PixDateApp(
    initialPhotoId: Long? = null,
    onPhotoOpened: () -> Unit = {}
) {
    var selectedSection by rememberSaveable {
        mutableStateOf(BottomSection.GALLERY)
    }

    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember {
        AppDatabase.getDatabase(appContext)
    }

    // El repositorio se memoiza con remember para evitar crear múltiples instancias
    val repository = remember(database) {
        PixDateRepository(
            photoDao = database.photoDao(),
            photoAnalysisDao = database.photoAnalysisDao(),
            tagDao = database.tagDao(),
            photoTagCrossRefDao = database.photoTagCrossRefDao(),
            folderDao = database.folderDao()
        )
    }

    val prefs = remember(appContext) {
        appContext.getSharedPreferences("pixdate_prefs", android.content.Context.MODE_PRIVATE)
    }

    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(repository, prefs)
    )

    val foldersViewModel: FoldersViewModel = viewModel(
        factory = FoldersViewModelFactory(repository)
    )

    /*
     * Estado local de navegación dentro de la sección de carpetas.
     */
    var selectedFolder by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var showEditFolderDialog by remember { mutableStateOf(false) }

    /** Indicador de acceso parcial a la galería (Android 14+). */
    var showPartialAccessBanner by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }


    /*
     * Estados observados desde los ViewModels.
     */
    val foldersList by foldersViewModel.folders.collectAsStateWithLifecycle()
    val photosByFolderId by foldersViewModel.photosByFolderId.collectAsStateWithLifecycle()

    val groupedPhotos by galleryViewModel.groupedPhotos.collectAsStateWithLifecycle()
    val viewMode by galleryViewModel.viewMode.collectAsStateWithLifecycle()
    val currentYearMonth by galleryViewModel.currentYearMonth.collectAsStateWithLifecycle()
    val selectedDate by galleryViewModel.selectedDate.collectAsStateWithLifecycle()
    val photoFilter by galleryViewModel.photoFilter.collectAsStateWithLifecycle()
    val selectedPhoto by galleryViewModel.selectedPhoto.collectAsStateWithLifecycle()
    val selectedPhotoDetail by galleryViewModel.selectedPhotoDetail.collectAsStateWithLifecycle()
    val processingState by galleryViewModel.processingState.collectAsStateWithLifecycle()
    
    val isSyncing by galleryViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by galleryViewModel.syncProgress.collectAsStateWithLifecycle()

    /*
     * Carpeta actualmente seleccionada.
     */
    val selectedFolderEntity = remember(foldersList, selectedFolder) {
        foldersList.find { folder ->
            folder.folderId == selectedFolder
        }
    }

    /*
     * Si la carpeta seleccionada ya no existe, limpiamos el estado desde un efecto.
     */
    LaunchedEffect(selectedFolder, selectedFolderEntity) {
        if (selectedFolder != null && selectedFolderEntity == null) {
            selectedFolder = null
        }
    }


    /*
     * Petición de permisos inicial.
     */
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fullAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        // Android 14+: el usuario puede elegir acceso parcial
        val partialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

        when {
            fullAccess -> {
                showPartialAccessBanner = false
                galleryViewModel.syncMediaStore(appContext, force = true)
            }
            partialAccess -> {
                showPartialAccessBanner = true
                galleryViewModel.syncMediaStore(appContext, force = true)
            }
        }
    }

    // Manejo de la navegación hacia atrás del sistema
    BackHandler(
        enabled = selectedPhoto != null || selectedSection != BottomSection.GALLERY || selectedFolder != null
    ) {
        if (selectedPhoto != null) {
            galleryViewModel.clearSelectedPhoto()
        } else if (selectedFolder != null) {
            selectedFolder = null
        } else if (selectedSection != BottomSection.GALLERY) {
            selectedSection = BottomSection.GALLERY
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+: solicitar acceso completo Y permiso de acceso parcial
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        NotificationHelper.createNotificationChannel(appContext)
    }

    // Gestión centralizada del ciclo de vida: foreground flag + sincronización automática al reanudar
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> galleryViewModel.isAppInForeground = true
                Lifecycle.Event.ON_STOP  -> galleryViewModel.isAppInForeground = false
                Lifecycle.Event.ON_RESUME -> {
                    val fullAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                    val partialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED

                    when {
                        fullAccess -> {
                            showPartialAccessBanner = false
                            galleryViewModel.syncMediaStore(appContext)
                        }
                        partialAccess -> {
                            showPartialAccessBanner = true
                            galleryViewModel.syncMediaStore(appContext)
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Banner de acceso parcial a la galería (Android 14+)
    LaunchedEffect(showPartialAccessBanner) {
        if (showPartialAccessBanner) {
            val result = snackbarHostState.showSnackbar(
                message = "Acceso parcial a la galería. Algunas fotos pueden no aparecer.",
                actionLabel = "Ajustes",
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", appContext.packageName, null)
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            }
        }
    }

    /*
     * Algunas pantallas necesitan las fotos en formato plano, remember evita recalcular flatten() en cada recomposición.
     */
    val flatPhotos = remember(groupedPhotos) {
        groupedPhotos.values.flatten()
    }

    // Observar eventos de análisis completado para Snackbar
    LaunchedEffect(galleryViewModel) {
        galleryViewModel.analysisCompletedEvent.collect { event ->
            // Mostrar Snackbar si la app está abierta
            val message = if (event.isSuccess) {
                "¡Análisis completado para ${event.photoName}!"
            } else {
                "No se pudo analizar ${event.photoName}"
            }
            val action = if (event.isSuccess) "Ver resultado" else "Ver"
            
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Seleccionar la foto desde el estado actualizado de la galería
                val currentPhotos = galleryViewModel.groupedPhotos.value.values.flatten()
                val photo = currentPhotos.find { it.photoId == event.photoId }
                if (photo != null) {
                    galleryViewModel.selectPhoto(photo)
                    selectedSection = BottomSection.GALLERY
                }
            }
        }
    }

    /*
     * Estado temporal de la captura con cámara.
     * Guardamos la URI como String para que sea compatible con rememberSaveable.
     */
    var pendingPhotoUriString by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var pendingPhotoFileName by rememberSaveable {
        mutableStateOf("")
    }

    /*
     * Launcher de cámara.
     *
     * ActivityResultContracts.TakePicture requiere una URI ya creada por la app.
     * Si la cámara confirma éxito, insertamos esa foto en la base de datos.
     */
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uriString = pendingPhotoUriString

        if (success && uriString != null) {
            galleryViewModel.insertCapturedPhoto(
                uri = uriString,
                fileName = pendingPhotoFileName
            )

            selectedSection = BottomSection.GALLERY
        }

        // Limpiamos el estado pendiente siempre, tanto en éxito como en fallo
        // para evitar mantener estados huérfanos que se restauren por error.
        pendingPhotoUriString = null
        pendingPhotoFileName = ""
    }

    /**
     * Crea un archivo privado persistente y lanza la cámara nativa.
     */
    fun launchCamera() {
        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val fileName = "PIXDATE_$timestamp.jpg"

        val photosDir = File(appContext.filesDir, "photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }

        val photoFile = File(photosDir, fileName)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        pendingPhotoUriString = uri.toString()
        pendingPhotoFileName = fileName

        cameraLauncher.launch(uri)
    }

    // Gestionar la apertura de foto desde la notificación
    LaunchedEffect(initialPhotoId, flatPhotos) {
        if (initialPhotoId != null) {
            val photo = flatPhotos.find { it.photoId == initialPhotoId }
            if (photo != null) {
                galleryViewModel.selectPhoto(photo)
                selectedSection = BottomSection.GALLERY
                onPhotoOpened()
            }
        }
    }

    val selectedFolderPhotos = remember(flatPhotos, selectedFolder) {
        if (selectedFolder == null) {
            emptyList()
        } else {
            flatPhotos.filter { photo ->
                photo.folderId == selectedFolder
            }
        }
    }

    // Renderizado pagina legal
    if (selectedSection == BottomSection.LEGAL) {
        LegalScreen(
            innerPadding = PaddingValues(0.dp),
            onClose = {
                selectedSection = BottomSection.GALLERY
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectedPhoto == null) {
                    PixDateTopBar(
                        selectedSection = selectedSection,
                        selectedFolder = selectedFolderEntity,
                        photoFilter = photoFilter,
                        viewMode = viewMode,
                        onBackFromFolder = {
                            selectedFolder = null
                        },
                        onEditFolder = {
                            showEditFolderDialog = true
                        },
                        onOpenLegal = {
                            selectedSection = BottomSection.LEGAL
                        },
                        onFilterSelected = { filter ->
                            galleryViewModel.setFilter(filter)
                        },
                        onToggleViewMode = {
                            galleryViewModel.toggleViewMode()
                        }
                    )
                }
            },
            bottomBar = {
                PixDateBottomBar(
                    selectedSection = selectedSection,
                    onSectionSelected = { section ->
                        if (section != BottomSection.CAMERA) {
                            /*
                             * Al cambiar de sección, cerramos el detalle de foto.para mayor "fijacion"
                             */
                            galleryViewModel.clearSelectedPhoto()

                            selectedSection = section

                            if (section == BottomSection.FOLDERS) {
                                foldersViewModel.cleanupEmptyFolders()
                            } else {
                                selectedFolder = null
                            }
                        }
                    },
                    onCameraClick = {
                        launchCamera()
                    }
                )
            }
        ) { innerPadding ->
            when (selectedSection) {
                BottomSection.GALLERY -> {
                    val photo = selectedPhoto

                    // Mostramos el detalle de foto si hay una seleccionada, o la galería si no.
                    if (photo != null) {
                        PhotoDetailScreen(
                            innerPadding = innerPadding,
                            photo = photo,
                            detailInfo = selectedPhotoDetail,
                            processingState = processingState,
                            onBack = {
                                galleryViewModel.clearSelectedPhoto()
                            },
                            onProcess = {
                                galleryViewModel.processPhoto(photo, context)
                            },
                            onConfirmAnalysis = { analysis ->
                                galleryViewModel.applyAnalysis(photo, analysis)
                            },
                            onDismissComparison = {
                                galleryViewModel.resetProcessingState()
                            },
                            onSaveEdit = { desc, tags, cat ->
                                galleryViewModel.manualUpdateAnalysis(photo, desc, tags, cat)
                            }
                        )
                    } else {
                        GalleryScreen(
                            innerPadding = innerPadding,
                            viewMode = viewMode,
                            groupedPhotos = groupedPhotos,
                            currentYearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            onNextMonth = { galleryViewModel.nextMonth() },
                            onPrevMonth = { galleryViewModel.prevMonth() },
                            onNextWeek = { galleryViewModel.nextWeek() },
                            onPrevWeek = { galleryViewModel.prevWeek() },
                            onSelectDate = { date -> galleryViewModel.selectDate(date) },
                            onPhotoClick = { clickedPhoto -> galleryViewModel.selectPhoto(clickedPhoto) },
                            onYearMonthSelected = { yearMonth -> galleryViewModel.setYearMonth(yearMonth) },
                            isSyncing = isSyncing,
                            syncProgress = syncProgress
                        )
                    }
                }

                BottomSection.CAMERA -> {
                    // El botón de cámara lanza directamente una Activity externa;
                    // esta rama nunca se renderiza.
                }

                BottomSection.FOLDERS -> {
                    val photo = selectedPhoto

                    if (photo != null) {
                        PhotoDetailScreen(
                            innerPadding = innerPadding,
                            photo = photo,
                            detailInfo = selectedPhotoDetail,
                            processingState = processingState,
                            onBack = {
                                galleryViewModel.clearSelectedPhoto()
                            },
                            onProcess = {
                                galleryViewModel.processPhoto(photo, context)
                            },
                            onConfirmAnalysis = { analysis ->
                                galleryViewModel.applyAnalysis(photo, analysis)
                            },
                            onDismissComparison = {
                                galleryViewModel.resetProcessingState()
                            },
                            onSaveEdit = { desc, tags, cat ->
                                galleryViewModel.manualUpdateAnalysis(photo, desc, tags, cat)
                            }
                        )
                    } else if (selectedFolder != null && selectedFolderEntity != null) {
                        FolderDetailScreen(
                            innerPadding = innerPadding,
                            folder = selectedFolderEntity,
                            photos = selectedFolderPhotos,
                            allPhotos = flatPhotos,
                            onBack = {
                                selectedFolder = null
                            },
                            onPhotoClick = { clickedPhoto ->
                                galleryViewModel.selectPhoto(clickedPhoto)
                            },
                            onAddPhotos = { photoIds ->
                                foldersViewModel.addPhotosToFolder(photoIds, selectedFolderEntity.folderId)
                            }
                        )
                    } else {
                        FoldersScreen(
                            innerPadding = innerPadding,
                            folders = foldersList,
                            photosByFolderId = photosByFolderId,
                            onCreateFolder = { name ->
                                foldersViewModel.createFolder(name)
                            },
                            onFolderClick = { folderId ->
                                selectedFolder = folderId
                            },
                            onRenameFolder = { folder, newName ->
                                foldersViewModel.renameFolder(folder, newName)
                            },
                            onDeleteFolder = { folder ->
                                foldersViewModel.deleteFolder(folder)
                            }
                        )
                    }
                }

                BottomSection.LEGAL -> {
                // se renderiza fuera :)
                }
            }
        }

        // El diálogo de edición de carpeta se muestra por encima del Scaffold para evitar problemas de navegación interna.
        if (showEditFolderDialog && selectedFolderEntity != null) {
            EditFolderDialog(
                folder = selectedFolderEntity,
                onDismiss = {
                    showEditFolderDialog = false
                },
                onRename = { newName ->
                    foldersViewModel.renameFolder(selectedFolderEntity, newName)
                    showEditFolderDialog = false
                },
                onDelete = {
                    foldersViewModel.deleteFolder(selectedFolderEntity)
                    selectedFolder = null
                    galleryViewModel.clearSelectedPhoto()
                    showEditFolderDialog = false
                }
            )
        }
    }
}

/**
 * Barra superior de la aplicación.
 *
 * Su contenido cambia según el contexto:
 * - Carpeta abierta: botón atrás, nombre de carpeta y editar.
 * - Lista de carpetas: título "CARPETAS".
 * - Galería: título, legal, filtros y cambio de vista.
 */
@Composable
private fun PixDateTopBar(
    selectedSection: BottomSection,
    selectedFolder: FolderEntity?,
    photoFilter: PhotoFilter,
    viewMode: GalleryViewMode,
    onBackFromFolder: () -> Unit,
    onEditFolder: () -> Unit,
    onOpenLegal: () -> Unit,
    onFilterSelected: (PhotoFilter) -> Unit,
    onToggleViewMode: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when {
                selectedSection == BottomSection.FOLDERS && selectedFolder != null -> {
                    FolderDetailTopBarContent(
                        folder = selectedFolder,
                        onBack = onBackFromFolder,
                        onEditFolder = onEditFolder
                    )
                }

                selectedSection == BottomSection.FOLDERS -> {
                    Text(
                        text = "CARPETAS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    GalleryTopBarContent(
                        photoFilter = photoFilter,
                        viewMode = viewMode,
                        onOpenLegal = onOpenLegal,
                        onFilterSelected = onFilterSelected,
                        onToggleViewMode = onToggleViewMode
                    )
                }
            }
        }
    }
}

/**
 * Contenido de la top bar cuando el usuario está dentro de una carpeta.
 */
@Composable
private fun RowScope.FolderDetailTopBarContent(
    folder: FolderEntity,
    onBack: () -> Unit,
    onEditFolder: () -> Unit
) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Atrás",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }

    Text(
        text = folder.name.uppercase(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
    )

    IconButton(onClick = onEditFolder) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Editar carpeta",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Contenido de la top bar para la galería principal.
 */
@Composable
private fun GalleryTopBarContent(
    photoFilter: PhotoFilter,
    viewMode: GalleryViewMode,
    onOpenLegal: () -> Unit,
    onFilterSelected: (PhotoFilter) -> Unit,
    onToggleViewMode: () -> Unit
) {
    Text(
        text = "PIXDATE",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onPrimary
    )

    Row {
        IconButton(onClick = onOpenLegal) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info y legal",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        PhotoFilterMenu(
            currentFilter = photoFilter,
            onFilterSelected = onFilterSelected
        )

        IconButton(onClick = onToggleViewMode) {
            Icon(
                imageVector = if (viewMode == GalleryViewMode.CALENDAR) {
                    Icons.Default.List
                } else {
                    Icons.Default.CalendarMonth
                },
                contentDescription = "Cambiar vista",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Menú desplegable de filtros de la galería.
 */
@Composable
private fun PhotoFilterMenu(
    currentFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit
) {
    var showFilterMenu by remember {
        mutableStateOf(false)
    }

    val filterOptions = listOf(
        PhotoFilter.ALL to "Todas",
        PhotoFilter.PROCESSED to "Procesadas",
        PhotoFilter.UNPROCESSED to "Sin procesar"
    )

    Box {
        IconButton(
            onClick = {
                showFilterMenu = true
            }
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Abrir filtros",
                tint = if (currentFilter == PhotoFilter.ALL) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                }
            )
        }

        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = {
                showFilterMenu = false
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            filterOptions.forEach { (filter, label) ->
                val selected = currentFilter == filter

                DropdownMenuItem(
                    modifier = Modifier.background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    text = {
                        Text(
                            text = label,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        showFilterMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Diálogo para renombrar o eliminar una carpeta.
 *
 * Validaciones aplicadas:
 * - No permite guardar nombres vacíos.
 * - No permite guardar si el nombre no ha cambiado.
 */
@Composable
fun EditFolderDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var newName by remember(folder.folderId) {
        mutableStateOf(folder.name)
    }

    var showDeleteConfirm by remember {
        mutableStateOf(false)
    }

    val trimmedName = newName.trim()
    val canSave = trimmedName.isNotEmpty() && trimmedName != folder.name

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
            },
            shape = androidx.compose.ui.graphics.RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = "ELIMINAR CARPETA",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Estás seguro de eliminar la carpeta '${folder.name}'? PD: Las fotos no se borrarán."
                )
            },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "ELIMINAR",
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                    }
                ) {
                    Text("CANCELAR")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = androidx.compose.ui.graphics.RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDITAR CARPETA",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { value ->
                            newName = value
                        },
                        label = {
                            Text("Nombre de la carpeta")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = true
                        }
                    ) {
                        Text(
                            text = "ELIMINAR",
                            color = Color.Red
                        )
                    }

                    TextButton(
                        enabled = canSave,
                        onClick = {
                            onRename(trimmedName)
                        }
                    ) {
                        Text("GUARDAR")
                    }
                }
            },
            dismissButton = null
        )
    }
}

/**
 * Barra inferior principal.
 *
 * Contiene dos destinos reales —Galería y Carpetas— y una acción central
 * para abrir la cámara.
 */
@Composable
fun PixDateBottomBar(
    selectedSection: BottomSection,
    onSectionSelected: (BottomSection) -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                modifier = Modifier.weight(0.4f),
                selected = selectedSection == BottomSection.GALLERY,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Galería"
                    )
                },
                text = "Galería",
                onClick = {
                    onSectionSelected(BottomSection.GALLERY)
                }
            )

            CameraBarItem(
                modifier = Modifier.weight(0.2f),
                onClick = onCameraClick
            )

            BottomBarItem(
                modifier = Modifier.weight(0.4f),
                selected = selectedSection == BottomSection.FOLDERS,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Carpetas"
                    )
                },
                text = "Carpetas",
                onClick = {
                    onSectionSelected(BottomSection.FOLDERS)
                }
            )
        }
    }
}

/**
 * Item lateral de la barra inferior.
 *
 * El icono mantiene tamaño constante; el estado seleccionado se comunica
 * mostrando también el texto.
 */
@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            if (selected) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Botón central de cámara.
 */
@Composable
private fun CameraBarItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp)
                .clip(CutCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                    shape = CutCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cámara",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}