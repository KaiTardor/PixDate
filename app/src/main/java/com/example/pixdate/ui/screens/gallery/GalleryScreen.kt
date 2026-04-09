package com.example.pixdate.ui.screens.gallery

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixdate.data.local.entity.PhotoEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GalleryScreen(
    innerPadding: PaddingValues,
    viewMode: GalleryViewMode,
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate?,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        if (groupedPhotos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ SIN IMÁGENES ]",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            when (viewMode) {
                GalleryViewMode.CALENDAR -> {
                    CalendarView(
                        groupedPhotos = groupedPhotos,
                        currentYearMonth = currentYearMonth,
                        selectedDate = selectedDate,
                        onNextMonth = onNextMonth,
                        onPrevMonth = onPrevMonth,
                        onSelectDate = onSelectDate,
                        onPhotoClick = onPhotoClick
                    )
                }
                GalleryViewMode.SEQUENTIAL -> {
                    SequentialView(
                        groupedPhotos = groupedPhotos,
                        onPhotoClick = onPhotoClick
                    )
                }
            }
        }
    }
}

// ── MODO 1: SECUENCIAL (Listado Clásico con Sticky Headers falsos en Grid) ──

@Composable
private fun SequentialView(
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale.getDefault())

    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 3 para parecer más una galería clásica
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedPhotos.forEach { (date, photos) ->
            // Date Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = date.format(dateFormatter).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                )
            }

            // Photos for that date
            items(photos, key = { it.photoId }) { photo ->
                MinimalPhotoItem(
                    photo = photo,
                    onClick = { onPhotoClick(photo) }
                )
            }
        }
    }
}

@Composable
private fun MinimalPhotoItem(
    photo: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.small, // 0.dp (cuadrado)
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        PhotoImage(
            photo = photo,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// ── MODO 2: CALENDARIO (Master-Detail) ─────────────────────────

@Composable
private fun CalendarView(
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate?,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // --- MASTER: CALENDARIO (Mitad Superior Funcional) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                .padding(8.dp)
        ) {
            // Controles de mes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mes Previo")
                }
                
                val monthName = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
                Text(
                    text = "$monthName ${currentYearMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = onNextMonth) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Mes Siguiente")
                }
            }

            // Días de la semana
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                for (day in daysOfWeek) {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).substring(0, 1).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Grid funcional del calendario
            val daysInMonth = currentYearMonth.lengthOfMonth()
            val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
            
            val totalCells = daysInMonth + (firstDayOfWeek - 1)
            val rows = Math.ceil(totalCells / 7.0).toInt()

            var currentDay = 1
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val isValiddDay = row != 0 || col >= (firstDayOfWeek - 1)
                        if (isValiddDay && currentDay <= daysInMonth) {
                            val dateToRender = currentYearMonth.atDay(currentDay)
                            val hasPhotos = groupedPhotos.containsKey(dateToRender)
                            val isSelected = dateToRender == selectedDate

                            CalendarDayCell(
                                modifier = Modifier.weight(1f),
                                day = currentDay.toString(),
                                hasPhotos = hasPhotos,
                                isSelected = isSelected,
                                onClick = { onSelectDate(dateToRender) }
                            )
                            currentDay++
                        } else {
                            // Espacio vacío
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

        // --- DETAIL: FOTOS DEL DÍA SELECCIONADO (Mitad Inferior) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val photosForSelectedDate = if (selectedDate != null) groupedPhotos[selectedDate] else null

            if (selectedDate == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("SELECCIONA UN DÍA", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else if (photosForSelectedDate.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NO HAY FOTOS EL ${selectedDate.dayOfMonth}", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photosForSelectedDate, key = { it.photoId }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            onClick = { onPhotoClick(photo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    day: String,
    hasPhotos: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
            .border(
                BorderStroke(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            )
            
            // Punto indicador interactivo (Pixel art style)
            if (hasPhotos) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary) // Punto naranja
                )
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
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            PhotoImage(
                photo = photo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface) 
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                    .padding(8.dp)
            ) {
                Text(
                    text = photo.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PhotoImage(
    photo: PhotoEntity,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val model: Any = if (photo.contentUri.startsWith("file:///android_asset")) {
        "file:///android_asset/sample_images/${photo.displayName}"
    } else {
        Uri.parse(photo.contentUri)
    }

    Box(modifier = modifier.background(Color.LightGray)) {
        AsyncImage(
            model = model,
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}