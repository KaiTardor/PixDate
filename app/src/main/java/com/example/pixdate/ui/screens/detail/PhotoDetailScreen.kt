package com.example.pixdate.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixdate.data.local.entity.PhotoEntity
import com.example.pixdate.data.remote.AIVisionService
import com.example.pixdate.ui.screens.gallery.PhotoDetailInfo

/**
 * Estado del procesamiento de IA
 */
sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Loading : ProcessingState()
    data class Success(val description: String) : ProcessingState()
    data class Comparison(
        val oldAnalysis: AIVisionService.ImageAnalysis,
        val newAnalysis: AIVisionService.ImageAnalysis
    ) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailScreen(
    innerPadding: PaddingValues,
    photo: PhotoEntity,
    detailInfo: PhotoDetailInfo?,
    processingState: ProcessingState,
    onBack: () -> Unit,
    onProcess: () -> Unit,
    onConfirmAnalysis: (AIVisionService.ImageAnalysis) -> Unit,
    onDismissComparison: () -> Unit,
    onSaveEdit: (String, String, String) -> Unit // desc, tags, category
) {
    val context = LocalContext.current

    // ── Estados internos del diálogo ────────────────────────────
    var showMoreInfoDialog by remember { mutableStateOf(false) }
    var showEditMode by remember { mutableStateOf(false) }
    var showMenuExpanded by remember { mutableStateOf(false) }

    // ── Estados de edición ──────────────────────────────────────
    var editDescription by remember(detailInfo) {
        mutableStateOf(detailInfo?.analysis?.description ?: "")
    }
    var editTags by remember(detailInfo) {
        mutableStateOf(detailInfo?.tags?.joinToString(", ") { it.name } ?: "")
    }
    var editCategory by remember(detailInfo) {
        mutableStateOf(detailInfo?.analysis?.mainCategory ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // ══════════════════════════════════════════════════════════
        // ── Barra Superior ───────────────────────────────────────
        // ══════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Botón Volver (izquierda)
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. Botón Reintentar IA
            IconButton(
                onClick = onProcess,
                enabled = processingState !is ProcessingState.Loading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Re-analizar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // 3. Botón Editar
            IconButton(
                onClick = { showEditMode = !showEditMode },
                enabled = photo.isProcessed && detailInfo?.analysis != null
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = if (photo.isProcessed)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                )
            }

            // 4. Menú de 3 puntos
            Box {
                IconButton(onClick = { showMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenuExpanded,
                    onDismissRequest = { showMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    // Compartir imagen
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Compartir",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenuExpanded = false
                            shareImage(context, photo)
                        }
                    )

                    // Copiar descripción (Postear)
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Postear",
                                color = if (detailInfo?.analysis?.description != null)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        enabled = detailInfo?.analysis?.description != null,
                        onClick = {
                            showMenuExpanded = false
                            val desc = detailInfo?.analysis?.description ?: return@DropdownMenuItem
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("PixDate", desc)
                            )
                            Toast.makeText(context, "Descripción copiada", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Más info
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Más info",
                                color = if (detailInfo?.analysis != null)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        enabled = detailInfo?.analysis != null,
                        onClick = {
                            showMenuExpanded = false
                            showMoreInfoDialog = true
                        }
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════
        // ── Imagen Principal (más grande) ────────────────────────
        // ══════════════════════════════════════════════════════════
        val imageModel: Any = if (photo.contentUri.startsWith("file:///android_asset")) {
            "file:///android_asset/sample_images/${photo.displayName}"
        } else {
            Uri.parse(photo.contentUri)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(Color.DarkGray)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Indicador de carga sobre la imagen
            if (processingState is ProcessingState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PROCESANDO...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════
        // ── Contenido debajo de la imagen ────────────────────────
        // ══════════════════════════════════════════════════════════

        if (photo.isProcessed && detailInfo?.analysis != null) {
            // ═══ ESTADO: PROCESADA ═══
            val analysis = detailInfo.analysis

            if (showEditMode) {
                // ── Modo edición ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "EDITAR ANÁLISIS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text("Etiquetas (separadas por coma)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditMode = false }) {
                            Text("CANCELAR")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveEdit(editDescription, editTags, editCategory)
                                showEditMode = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("GUARDAR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ── Vista de solo lectura (compacta) ──────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Descripción
                    Text(
                        text = analysis.description ?: "Sin descripción",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categoría + Tags en una sola fila de chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Chip de categoría (naranja)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = analysis.mainCategory ?: "OTHER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Tags
                        detailInfo.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#${tag.name.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Carpeta (si existe)
                    if (detailInfo.folder != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Carpeta: ${detailInfo.folder.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

        } else if (processingState is ProcessingState.Error) {
            // ═══ ESTADO: ERROR ═══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = processingState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onProcess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("REINTENTAR", style = MaterialTheme.typography.bodyMedium)
                }
            }

        } else if (processingState !is ProcessingState.Loading) {
            // ═══ ESTADO: SIN PROCESAR (Idle) ═══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IMAGEN SIN ANALIZAR",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pulsa ↻ en la barra para analizar con IA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onProcess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANALIZAR CON IA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ══════════════════════════════════════════════════════════════
    // ── Diálogos ─────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════

    // Diálogo: Comparar análisis (Sobreescribir)
    if (processingState is ProcessingState.Comparison) {
        val old = processingState.oldAnalysis
        val new = processingState.newAnalysis

        AlertDialog(
            onDismissRequest = { /* No cerrar si no elige? O permitir cancelar */ },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text("¿CON CUÁL TE QUEDAS?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Opción Antigua
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color.Gray))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ACTUAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(old.description)
                        Text("Categoría: ${old.category}", style = MaterialTheme.typography.labelSmall)
                        Text("Tags: ${old.tags.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDismissComparison, // Descartar el nuevo, volver al actual
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RectangleShape
                        ) {
                            Text("DESCARTAR NUEVO")
                        }
                    }

                    // Opción Nueva
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "NUEVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(new.description)
                        Text("Categoría: ${new.category}", style = MaterialTheme.typography.labelSmall)
                        Text("Tags: ${new.tags.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onConfirmAnalysis(new) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RectangleShape
                        ) {
                            Text("USAR NUEVO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissComparison) { 
                    Text("CANCELAR")
                }
            }
        )
    }

    // Diálogo: Más info (modelo, confianza, etc.)
    if (showMoreInfoDialog && detailInfo?.analysis != null) {
        val analysis = detailInfo.analysis
        AlertDialog(
            onDismissRequest = { showMoreInfoDialog = false },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text("INFORMACIÓN DEL ANÁLISIS", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    InfoRow("Modelo", analysis.modelUsed ?: "Desconocido")
                    if (analysis.confidence != null) {
                        InfoRow("Confianza", "${(analysis.confidence * 100).toInt()}%")
                    }
                    if (analysis.processedAt != null) {
                        val date = java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(analysis.processedAt))
                        InfoRow("Procesado", date)
                    }
                    if (detailInfo.folder != null) {
                        InfoRow("Carpeta", detailInfo.folder.name)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoreInfoDialog = false }) {
                    Text("CERRAR")
                }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// ── Funciones auxiliares ─────────────────────────────────────────
// ═════════════════════════════════════════════════════════════════

private fun shareImage(context: Context, photo: PhotoEntity) {
    try {
        val uri = Uri.parse(photo.contentUri)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = photo.mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo compartir la imagen", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
