package com.example.pixdate.ui.screens.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixdate.data.local.entity.PhotoEntity

@Composable
fun GalleryScreen(
    innerPadding: PaddingValues,
    photos: List<PhotoEntity>,
    onInsertSample: () -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ── Cabecera ─────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Galería",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                onClick = onInsertSample,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir ejemplo"
                )
            }
        }

        // ── Grid de fotos ────────────────────────────────────────
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay fotos todavía",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos, key = { it.photoId }) { photo ->
                    PhotoGridItem(
                        photo = photo,
                        onClick = { onPhotoClick(photo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AssetImage(
                assetPath = photo.displayName,
                contentDescription = photo.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssetImage(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = "file:///android_asset/sample_images/$assetPath",
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}