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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixdate.data.local.entity.FolderEntity
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.ui.theme.PrimaryPastel

/**
 * Pantalla principal de carpetas.
 */
@Composable
fun FoldersScreen(
    innerPadding: PaddingValues,
    folders: List<FolderEntity>,
    photosByFolderId: Map<Long, List<PhotoEntity>>,
    onFolderClick: (Long) -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        if (folders.isEmpty()) {
            EmptyFoldersState()
        } else {
            FoldersGrid(
                folders = folders,
                photosByFolderId = photosByFolderId,
                onFolderClick = { folder -> onFolderClick(folder.folderId) }
            )
        }

        // Botón estilo "Cámara" para añadir carpetas
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .width(48.dp)
                .height(48.dp)
                .clip(CutCornerShape(8.dp))
                .background(PrimaryPastel)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                    shape = CutCornerShape(8.dp)
                )
                .clickable {
                    showCreateDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear carpeta",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = {
                showCreateDialog = false
            },
            onCreate = { name ->
                onCreateFolder(name)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Estado vacío de la pantalla de carpetas.
 */
@Composable
private fun EmptyFoldersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "[ SIN CARPETAS ]",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/**
 * Grid principal de carpetas.
 */
@Composable
private fun FoldersGrid(
    folders: List<FolderEntity>,
    photosByFolderId: Map<Long, List<PhotoEntity>>,
    onFolderClick: (FolderEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = folders,
            key = { folder -> folder.folderId }
        ) { folder ->
            val folderPhotos = photosByFolderId[folder.folderId].orEmpty()
            val latestPhoto = folderPhotos.maxByOrNull { photo -> photo.dateTaken }

            FolderItem(
                folder = folder,
                photosCount = folderPhotos.size,
                latestPhoto = latestPhoto,
                onClick = {
                    onFolderClick(folder)
                }
            )
        }
    }
}

/**
 * Tarjeta individual de carpeta.
 *
 * Si la carpeta tiene fotos, muestra la imagen más reciente como portada.
 * Si está vacía, muestra un icono de carpeta.
 */
@Composable
private fun FolderItem(
    folder: FolderEntity,
    photosCount: Int,
    latestPhoto: PhotoEntity?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                onClick()
            },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FolderThumbnail(
                photo = latestPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            FolderInfo(
                folder = folder,
                photosCount = photosCount
            )
        }
    }
}

/**
 * Miniatura visual de una carpeta.
 */
@Composable
private fun FolderThumbnail(
    photo: PhotoEntity?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo.toImageModel(),
                contentDescription = "Miniatura de ${photo.displayName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }
}

/**
 * Información textual de una carpeta.
 */
@Composable
private fun FolderInfo(
    folder: FolderEntity,
    photosCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = folder.name.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$photosCount elemento${if (photosCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Diálogo para crear una carpeta manualmente.
 */
@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember {
        mutableStateOf("")
    }

    val trimmedName = folderName.trim()
    val canCreate = trimmedName.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = "NUEVA CARPETA",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { value ->
                    folderName = value
                },
                label = {
                    Text("Nombre de la carpeta")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = canCreate,
                onClick = {
                    onCreate(trimmedName)
                }
            ) {
                Text("CREAR")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("CANCELAR")
            }
        }
    )
}

/**
 * Convierte una PhotoEntity en un modelo válido para Coil.
 *
 * Las fotos importadas desde CSV se guardan como assets. Las capturadas por
 * cámara se guardan como URI real.
 */
private fun PhotoEntity.toImageModel(): Any {
    return Uri.parse(contentUri)
}