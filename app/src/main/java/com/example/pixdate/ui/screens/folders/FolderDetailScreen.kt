package com.example.pixdate.ui.screens.folders

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.ui.screens.gallery.MinimalPhotoItem

/**
 * Pantalla de detalle de una carpeta.
 *
 * Muestra las fotos de la carpeta en un grid de 3 columnas con un tile "+"
 * al final que permite añadir fotos desde la galería del dispositivo.
 *
 * Esta pantalla no conoce el ViewModel. Recibe directamente las fotos que debe
 * mostrar y notifica eventos hacia arriba.
 */
@Composable
fun FolderDetailScreen(
    innerPadding: PaddingValues,
    folder: FolderEntity,
    photos: List<PhotoEntity>,
    allPhotos: List<PhotoEntity>,
    onBack: () -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit,
    onAddPhotos: (List<Long>) -> Unit
) {
    var showPhotoPickerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        if (photos.isEmpty()) {
            EmptyFolderState(
                folderName = folder.name,
                onAddClick = { showPhotoPickerDialog = true }
            )
        } else {
            FolderPhotoGrid(
                photos = photos,
                onPhotoClick = onPhotoClick,
                onAddClick = { showPhotoPickerDialog = true }
            )
        }
    }

    if (showPhotoPickerDialog) {
        // Fotos que ya están en esta carpeta (excluidas del picker)
        val photosInFolder = photos.map { it.photoId }.toSet()
        val availablePhotos = allPhotos.filter { it.photoId !in photosInFolder }

        PhotoPickerDialog(
            availablePhotos = availablePhotos,
            onDismiss = { showPhotoPickerDialog = false },
            onConfirm = { selectedIds ->
                onAddPhotos(selectedIds)
                showPhotoPickerDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyFolderState(
    folderName: String,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No hay fotos en \"$folderName\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            shape = RectangleShape,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "  AÑADIR FOTOS",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FolderPhotoGrid(
    photos: List<PhotoEntity>,
    onPhotoClick: (PhotoEntity) -> Unit,
    onAddClick: () -> Unit
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
                onClick = { onPhotoClick(photo) }
            )
        }

        // Tile "+" al final del grid
        item(key = "add_button") {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir fotos a esta carpeta",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Diálogo selector de fotos para añadir a una carpeta.
 *
 * Muestra todas las fotos que aún no pertenecen a esta carpeta en un grid.
 * El usuario puede seleccionar múltiples fotos tocándolas (se marcan con un tick)
 * y confirmar la selección con el botón AÑADIR.
 */
@Composable
private fun PhotoPickerDialog(
    availablePhotos: List<PhotoEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AÑADIR FOTOS", fontWeight = FontWeight.Bold)
                if (selectedIds.isNotEmpty()) {
                    Text(
                        text = "${selectedIds.size} sel.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            if (availablePhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todas las fotos ya están en esta carpeta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(360.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(
                        items = availablePhotos,
                        key = { it.photoId }
                    ) { photo ->
                        val isSelected = photo.photoId in selectedIds

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                                .clickable {
                                    selectedIds = if (isSelected) {
                                        selectedIds - photo.photoId
                                    } else {
                                        selectedIds + photo.photoId
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = Uri.parse(photo.contentUri),
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Tick de selección
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seleccionada",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                shape = RectangleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("AÑADIR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}