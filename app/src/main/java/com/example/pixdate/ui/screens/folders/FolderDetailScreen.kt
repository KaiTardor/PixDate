package com.example.pixdate.ui.screens.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.ui.screens.gallery.MinimalPhotoItem

/**
 * Pantalla de detalle de una carpeta.
 *
 * Esta pantalla no conoce el ViewModel. Recibe directamente las fotos que debe
 * mostrar y notifica eventos hacia arriba.
 */
@Composable
fun FolderDetailScreen(
    innerPadding: PaddingValues,
    folder: FolderEntity,
    photos: List<PhotoEntity>,
    onBack: () -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        if (photos.isEmpty()) {
            EmptyFolderState(
                folderName = folder.name
            )
        } else {
            FolderPhotoGrid(
                photos = photos,
                onPhotoClick = onPhotoClick
            )
        }
    }
}

@Composable
private fun EmptyFolderState(
    folderName: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay fotos en \"$folderName\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun FolderPhotoGrid(
    photos: List<PhotoEntity>,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = photos,
            key = { photo -> photo.photoId }
        ) { photo ->
            MinimalPhotoItem(
                photo = photo,
                onClick = {
                    onPhotoClick(photo)
                }
            )
        }
    }
}