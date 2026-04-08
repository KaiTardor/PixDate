package com.example.pixdate.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixdate.data.local.database.AppDatabase
import com.example.pixdate.data.repository.PixDateRepository
import com.example.pixdate.ui.screens.gallery.GalleryScreen
import com.example.pixdate.ui.screens.gallery.GalleryViewModel
import com.example.pixdate.ui.screens.gallery.GalleryViewModelFactory

enum class BottomSection {
    GALLERY,
    ADD,
    FOLDERS
}

@Composable
fun PixDateApp() {
    var selectedSection by rememberSaveable { mutableStateOf(BottomSection.GALLERY) }

    val context = LocalContext.current

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

    val photos by galleryViewModel.photos.collectAsStateWithLifecycle()

    // Auto-importar CSV si la BD está vacía (simula fotos del usuario)
    LaunchedEffect(Unit) {
        galleryViewModel.autoImportIfEmpty(context)
    }

    Scaffold(
        bottomBar = {
            PixDateBottomBar(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
        }
    ) { innerPadding ->
        when (selectedSection) {
            BottomSection.GALLERY -> GalleryScreen(
                innerPadding = innerPadding,
                photos = photos,
                onInsertSample = {
                    galleryViewModel.insertSamplePhoto()
                },
                onPhotoClick = { photo ->
                    galleryViewModel.loadPhotoDetail(photo)
                }
            )

            BottomSection.ADD -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pantalla cámara",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            BottomSection.FOLDERS -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pantalla carpetas",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
fun PixDateBottomBar(
    selectedSection: BottomSection,
    onSectionSelected: (BottomSection) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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

            AddBarItem(
                modifier = Modifier.weight(0.2f),
                selected = selectedSection == BottomSection.ADD,
                onClick = { onSectionSelected(BottomSection.ADD) }
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
            .clip(RoundedCornerShape(16.dp))
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
private fun AddBarItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val iconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}