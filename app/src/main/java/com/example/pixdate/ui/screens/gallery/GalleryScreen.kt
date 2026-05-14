package com.example.pixdate.ui.screens.gallery

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.runtime.mutableIntStateOf
import com.example.pixdate.data.local.entity.PhotoEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

@Composable
fun GalleryScreen(
    innerPadding: PaddingValues,
    viewMode: GalleryViewMode,
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate?,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextWeek: () -> Unit = {},
    onPrevWeek: () -> Unit = {},
    onSelectDate: (LocalDate) -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit,
    onYearMonthSelected: (YearMonth) -> Unit,
    isSyncing: Boolean = false,
    syncProgress: Float = 0f
) {
    var isCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        if (isSyncing) {
            LinearProgressIndicator(
                progress = syncProgress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (groupedPhotos.isEmpty()) {
                EmptyGalleryState()
            } else {
                when (viewMode) {
                    GalleryViewMode.CALENDAR -> {
                        CalendarView(
                            groupedPhotos = groupedPhotos,
                            currentYearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            isCollapsed = isCollapsed,
                            onCollapsedChange = { isCollapsed = it },
                            onNextMonth = if (isCollapsed) onNextWeek else onNextMonth,
                            onPrevMonth = if (isCollapsed) onPrevWeek else onPrevMonth,
                            onSelectDate = onSelectDate,
                            onPhotoClick = onPhotoClick,
                            onYearMonthSelected = onYearMonthSelected
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
}

/**
 * Estado vacío de la galería.
 */
@Composable
private fun EmptyGalleryState() {
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
}

/**
 * Modo secuencial.
 *
 * Muestra las fotos agrupadas por fecha en una cuadrícula de 3 columnas.
 * Los encabezados de fecha ocupan todo el ancho de la grid.
 */
@Composable
fun SequentialView(
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale.getDefault())
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedPhotos.forEach { (date, photos) ->
            item(
                span = {
                    GridItemSpan(maxLineSpan)
                }
            ) {
                DateHeader(
                    text = date.format(dateFormatter).uppercase()
                )
            }

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
}

/**
 * Encabezado de fecha para el modo secuencial.
 */
@Composable
private fun DateHeader(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(
            top = 16.dp,
            bottom = 8.dp,
            start = 8.dp
        )
    )
}

/**
 * Miniatura cuadrada usada en la galería secuencial.
 */
@Composable
fun MinimalPhotoItem(
    photo: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                onClick()
            },
        shape = MaterialTheme.shapes.small,
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

/**
 * Modo calendario.
 *
 * La parte superior muestra el mes. La parte inferior muestra las fotos del día seleccionado.
 * Es decir, modo maestro detalle
 */
@Composable
private fun CalendarView(
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate?,
    isCollapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit,
    onYearMonthSelected: (YearMonth) -> Unit
) {
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    val photoGridState = rememberLazyGridState()

    /*
     * Solo usamos selectedDate para calcular la fila visible si pertenece al mes actual.
     */
    val selectedDateInCurrentMonth = remember(selectedDate, currentYearMonth) {
        selectedDate?.takeIf { date ->
            YearMonth.from(date) == currentYearMonth
        }
    }

    /*
     * Conexión de scroll anidado para detectar scrolls en el grid de fotos y colapsar/expandir la cabecera del calendario.
     * Usamos rememberUpdatedState para que la conexión siempre use la lambda más reciente sin recrearse.
     */
    val currentOnCollapsedChange by androidx.compose.runtime.rememberUpdatedState(onCollapsedChange)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < -5f) {
                    currentOnCollapsedChange(true)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 5f) {
                    currentOnCollapsedChange(false)
                }

                return Offset.Zero
            }
        }
    }

    /*
     * Mostramos el selector de año y mes como un AlertDialog modal.
      - Al seleccionar un mes, se cierra el modal y se notifica al padre para actualizar el mes mostrado.
      - Al abrir el modal, se muestra un dropdown para elegir el año y una grid para elegir el mes.
     */
    if (showDatePicker) {
        YearMonthPickerModal(
            currentYearMonth = currentYearMonth,
            onDismiss = {
                showDatePicker = false
            },
            onYearMonthSelected = onYearMonthSelected
        )
    }

    /*
     * Estructura general del modo calendario:
     * - CalendarHeader: selector de mes, días de la semana y grid mensual.
     * - SelectedDayPhotoSection: muestra las fotos del día seleccionado o mensajes de estado
     */
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CalendarHeader(
            groupedPhotos = groupedPhotos,
            currentYearMonth = currentYearMonth,
            selectedDateInCurrentMonth = selectedDateInCurrentMonth,
            isCollapsed = isCollapsed,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onOpenMonthPicker = {
                showDatePicker = true
            },
            onSelectDate = onSelectDate
        )

        SelectedDayPhotoSection(
            modifier = Modifier.weight(1f),
            selectedDate = selectedDate,
            photosForSelectedDate = selectedDate?.let { date ->
                groupedPhotos[date]
            },
            gridNestedScrollConnection = nestedScrollConnection,
            photoGridState = photoGridState,
            onPhotoClick = onPhotoClick
        )
    }
}

/**
 * Cabecera de calendario: selector de mes, días de la semana y grid mensual.
 */
@Composable
private fun CalendarHeader(
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    currentYearMonth: YearMonth,
    selectedDateInCurrentMonth: LocalDate?,
    isCollapsed: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val daysOfWeek = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value
    val leadingEmptyCells = firstDayOfWeek - 1
    val totalCells = daysInMonth + leadingEmptyCells
    val rows = ceil(totalCells / 7.0).toInt()

    val selectedRow = selectedDateInCurrentMonth?.let { date ->
        val selectedCellIndex = date.dayOfMonth + leadingEmptyCells - 1
        selectedCellIndex / 7
    } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
            .padding(8.dp)
            .animateContentSize()
    ) {
        MonthControls(
            currentYearMonth = currentYearMonth,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onOpenMonthPicker = onOpenMonthPicker
        )

        WeekDayHeader(daysOfWeek = daysOfWeek)

        Spacer(modifier = Modifier.height(8.dp))

        for (row in 0 until rows) {
            AnimatedVisibility(
                visible = !isCollapsed || row == selectedRow,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                CalendarWeekRow(
                    row = row,
                    daysInMonth = daysInMonth,
                    leadingEmptyCells = leadingEmptyCells,
                    currentYearMonth = currentYearMonth,
                    groupedPhotos = groupedPhotos,
                    selectedDateInCurrentMonth = selectedDateInCurrentMonth,
                    onSelectDate = onSelectDate
                )
            }
        }
    }
}

/**
 * Controles superiores del calendario: mes anterior, mes actual y mes siguiente.
 */
@Composable
private fun MonthControls(
    currentYearMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit
) {
    val monthName = currentYearMonth.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .uppercase()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Mes anterior"
            )
        }

        Text(
            text = "$monthName ${currentYearMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable {
                    onOpenMonthPicker()
                }
                .padding(4.dp)
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Mes siguiente"
            )
        }
    }
}

/**
 * Fila con iniciales de los días de la semana.
 */
@Composable
private fun WeekDayHeader(
    daysOfWeek: List<DayOfWeek>
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .take(1)
                    .uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Una fila del calendario mensual.
 */
@Composable
private fun CalendarWeekRow(
    row: Int,
    daysInMonth: Int,
    leadingEmptyCells: Int,
    currentYearMonth: YearMonth,
    groupedPhotos: Map<LocalDate, List<PhotoEntity>>,
    selectedDateInCurrentMonth: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (col in 0 until 7) {
            val cellIndex = row * 7 + col
            val dayOfMonth = cellIndex - leadingEmptyCells + 1
            val isValidDay = dayOfMonth in 1..daysInMonth

            if (isValidDay) {
                val dateToRender = currentYearMonth.atDay(dayOfMonth)

                CalendarDayCell(
                    modifier = Modifier.weight(1f),
                    day = dayOfMonth.toString(),
                    hasPhotos = groupedPhotos.containsKey(dateToRender),
                    isSelected = dateToRender == selectedDateInCurrentMonth,
                    onClick = {
                        onSelectDate(dateToRender)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

/**
 * Parte inferior del modo calendario.
 *
 * Muestra:
 * - Mensaje si no hay día seleccionado.
 * - Mensaje si el día no tiene fotos.
 * - Grid de fotos si el día seleccionado contiene imágenes.
 */
@Composable
private fun SelectedDayPhotoSection(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate?,
    photosForSelectedDate: List<PhotoEntity>?,
    gridNestedScrollConnection: NestedScrollConnection,
    photoGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            selectedDate == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(gridNestedScrollConnection),
                    contentAlignment = Alignment.Center
                ) {
                    CenterMessage(text = "SELECCIONA UN DÍA")
                }
            }

            photosForSelectedDate.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(gridNestedScrollConnection),
                    contentAlignment = Alignment.Center
                ) {
                    CenterMessage(text = "NO HAY FOTOS EL ${selectedDate.dayOfMonth}")
                }
            }

            else -> {
                LazyVerticalGrid(
                    state = photoGridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(gridNestedScrollConnection),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = photosForSelectedDate,
                        key = { photo -> photo.photoId }
                    ) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            onClick = {
                                onPhotoClick(photo)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mensaje centrado reutilizable.
 */
@Composable
private fun CenterMessage(
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/**
 * Celda individual del calendario.
 */
@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    day: String,
    hasPhotos: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = if (isSelected) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    }
                )
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            if (hasPhotos) {
                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Tarjeta de foto usada en la zona inferior del calendario.
 */
@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        PhotoImage(
            photo = photo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Renderizado común de imagen.
 *
 * Soporta dos orígenes:
 * - Assets de ejemplo: file:///android_asset...
 * - URIs reales guardadas en base de datos.
 */
@Composable
private fun PhotoImage(
    photo: PhotoEntity,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val model: Any = remember(photo.contentUri, photo.displayName) {
        if (photo.contentUri.startsWith("file:///android_asset")) {
            "file:///android_asset/sample_images/${photo.displayName}"
        } else {
            Uri.parse(photo.contentUri)
        }
    }

    Box(
        modifier = modifier.background(Color.LightGray)
    ) {
        AsyncImage(
            model = model,
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )

        if (photo.isProcessed) {
            ProcessedPhotoBadge(
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Marca visual para fotos procesadas.
 *
 * Dibuja un triángulo en la esquina inferior derecha con un icono encima.
 */
@Composable
private fun ProcessedPhotoBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(24.dp)
    ) {
        val triangleColor = MaterialTheme.colorScheme.primary

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val path = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            }

            drawPath(
                path = path,
                color = triangleColor
            )
        }

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Procesada",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 2.dp, end = 2.dp)
                .size(12.dp)
        )
    }
}

/**
 * Selector modal de año y mes.
 */
@Composable
fun YearMonthPickerModal(
    currentYearMonth: YearMonth,
    onDismiss: () -> Unit,
    onYearMonthSelected: (YearMonth) -> Unit
) {
    var selectedYear by remember {
        mutableIntStateOf(currentYearMonth.year)
    }

    var showYearDropdown by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.ui.graphics.RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            YearSelectorHeader(
                selectedYear = selectedYear,
                showYearDropdown = showYearDropdown,
                onOpenDropdown = {
                    showYearDropdown = true
                },
                onDismissDropdown = {
                    showYearDropdown = false
                },
                onYearSelected = { year ->
                    selectedYear = year
                    showYearDropdown = false
                }
            )
        },
        text = {
            MonthGrid(
                selectedYear = selectedYear,
                currentYearMonth = currentYearMonth,
                onMonthSelected = { month ->
                    onYearMonthSelected(
                        YearMonth.of(selectedYear, month)
                    )
                    onDismiss()
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

/**
 * Cabecera del modal para seleccionar año.
 */
@Composable
private fun YearSelectorHeader(
    selectedYear: Int,
    showYearDropdown: Boolean,
    onOpenDropdown: () -> Unit,
    onDismissDropdown: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val availableYears = remember(currentYear) {
        (currentYear downTo currentYear - 20).toList()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier.clickable {
                    onOpenDropdown()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar año"
                )
            }

            DropdownMenu(
                expanded = showYearDropdown,
                onDismissRequest = onDismissDropdown
            ) {
                availableYears.forEach { year ->
                    DropdownMenuItem(
                        text = {
                            Text(year.toString())
                        },
                        onClick = {
                            onYearSelected(year)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Grid de meses del modal.
 */
@Composable
private fun MonthGrid(
    selectedYear: Int,
    currentYearMonth: YearMonth,
    onMonthSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(12) { monthIndex ->
            val month = monthIndex + 1
            val monthName = Month.of(month)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .uppercase()

            val isCurrentMonth = selectedYear == currentYearMonth.year &&
                    month == currentYearMonth.monthValue

            MonthCard(
                monthName = monthName,
                isSelected = isCurrentMonth,
                onClick = {
                    onMonthSelected(month)
                }
            )
        }
    }
}

/**
 * Tarjeta individual de mes dentro del selector.
 */
@Composable
private fun MonthCard(
    monthName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clickable {
                onClick()
            },
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthName,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}