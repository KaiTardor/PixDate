package com.example.pixdate.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixdate.data.local.database.AppDatabase
import com.example.pixdate.data.repository.PixDateRepository
import com.example.pixdate.ui.screens.gallery.GalleryScreen
import com.example.pixdate.ui.screens.gallery.GalleryViewMode
import com.example.pixdate.ui.screens.gallery.GalleryViewModel
import com.example.pixdate.ui.screens.gallery.GalleryViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BottomSection {
    GALLERY,
    CAMERA,
    FOLDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixDateApp() {
    var selectedSection by rememberSaveable { mutableStateOf(BottomSection.GALLERY) }

    val context = LocalContext.current

    // Inicializar la base de datos y el repositorio una sola vez
    val database = remember { AppDatabase.getDatabase(context) }

    val repository = remember {
        PixDateRepository(
            photoDao = database.photoDao(),
            photoAnalysisDao = database.photoAnalysisDao(),
            tagDao = database.tagDao(),
            photoTagCrossRefDao = database.photoTagCrossRefDao(),
            folderDao = database.folderDao()
        )
    }

    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(repository)
    )

    val groupedPhotos by galleryViewModel.groupedPhotos.collectAsStateWithLifecycle()
    val viewMode by galleryViewModel.viewMode.collectAsStateWithLifecycle()
    val currentYearMonth by galleryViewModel.currentYearMonth.collectAsStateWithLifecycle()
    val selectedDate by galleryViewModel.selectedDate.collectAsStateWithLifecycle()

    // Auto-importar CSV si la BD está vacía (simula fotos del usuario)
    LaunchedEffect(Unit) {
        galleryViewModel.autoImportIfEmpty(context)
    }

    // Lógica de captura de cámara

    // Estado para guardar la URI y nombre del archivo temporal
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFileName by remember { mutableStateOf("") }

    // Launcher que abre la cámara nativa y recibe el resultado
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoUri != null) {
            galleryViewModel.insertCapturedPhoto(
                uri = pendingPhotoUri.toString(),
                fileName = pendingPhotoFileName
            )
            // Volver a la galería para ver la foto nueva
            selectedSection = BottomSection.GALLERY
        }
    }

    /**
     * Crea un archivo temporal en la caché, obtiene su URI segura
     * via FileProvider, y lanza la cámara nativa.
     */
    fun launchCamera() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PIXDATE_$timestamp.jpg"

        // Crear el directorio photos/ dentro de la caché si no existe
        val photosDir = File(context.cacheDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val photoFile = File(photosDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        pendingPhotoUri = uri
        pendingPhotoFileName = fileName
        cameraLauncher.launch(uri)
    }

    // ##########################################
    // ── UI
    // ##########################################

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary, // Naranja pastel
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
                    Text(
                        text = "PIXDATE",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Row {
                        IconButton(
                            onClick = { galleryViewModel.toggleViewMode() }
                        ) {
                            Icon(
                                imageVector = if (viewMode == GalleryViewMode.CALENDAR) Icons.Default.List else Icons.Default.CalendarMonth,
                                contentDescription = "Cambiar vista",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        IconButton(
                            onClick = { galleryViewModel.insertSamplePhoto() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Añadir ejemplo",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            PixDateBottomBar(
                selectedSection = selectedSection,
                onSectionSelected = { section ->
                    if (section == BottomSection.CAMERA) {
                        launchCamera()
                    } else {
                        selectedSection = section
                    }
                }
            )
        }
    ) { innerPadding ->
        when (selectedSection) {
            BottomSection.GALLERY -> GalleryScreen(
                innerPadding = innerPadding,
                viewMode = viewMode,
                groupedPhotos = groupedPhotos,
                currentYearMonth = currentYearMonth,
                selectedDate = selectedDate,
                onNextMonth = { galleryViewModel.nextMonth() },
                onPrevMonth = { galleryViewModel.prevMonth() },
                onSelectDate = { galleryViewModel.selectDate(it) },
                onPhotoClick = { photo ->
                    galleryViewModel.loadPhotoDetail(photo)
                }
            )

            BottomSection.CAMERA -> {
                // Este caso no se renderiza porque launchCamera()
                // abre directamente la cámara externa y vuelve a GALLERY.
                // Lo dejamos por completitud del when.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABRIENDO CÁMARA...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            BottomSection.FOLDERS -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CARPETAS",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
// Barra inferior personalizada con 3 secciones: GALERÍA, CÁMARA y CARPETAS
fun PixDateBottomBar(
    selectedSection: BottomSection,
    onSectionSelected: (BottomSection) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary, // Naranja
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)) // Borde duro
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
                onClick = { onSectionSelected(BottomSection.GALLERY) }
            )

            // Botón central: CÁMARA
            CameraBarItem(
                modifier = Modifier.weight(0.2f),
                onClick = { onSectionSelected(BottomSection.CAMERA) }
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
                onClick = { onSectionSelected(BottomSection.FOLDERS) }
            )
        }
    }
}

@Composable
// Item individual para GALERÍA y CARPETAS en la barra inferior
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
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
                modifier = Modifier.size(if (selected) 18.dp else 22.dp),
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

@Composable
// Item central personalizado para el botón de CÁMARA
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
                .background(MaterialTheme.colorScheme.surface) // Fondo claro para contrastar con la barra naranja
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                    shape = CutCornerShape(8.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cámara",
                tint = MaterialTheme.colorScheme.onSurface, // Icono oscuro
                modifier = Modifier.size(22.dp)
            )
        }
    }
}