package com.example.pixdate.data.local

/**
 * Representación intermedia de una fila del CSV antes de convertirla en entities Room.
 * Se usa como paso intermedio entre el parseo del CSV y la inserción en base de datos.
 */
data class CsvPhotoRow(
    val sampleId: Int,
    val fileName: String,
    val captionReference: String,
    val mainCategory: String,
    val tags: List<String>
)
