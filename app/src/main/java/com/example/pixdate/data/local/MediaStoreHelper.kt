package com.example.pixdate.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaStoreImage(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val dateAdded: Long
)

object MediaStoreHelper {
    /**
     * Consulta todas las imágenes del dispositivo usando MediaStore.
     * Retorna una lista con su URI, nombre, mimeType y fecha.
     */
    suspend fun getAllImages(context: Context): List<MediaStoreImage> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaStoreImage>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Ordenamos por fecha añadida descendente (más nuevas primero)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Desconocido"
                val mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg"
                // MediaStore.Images.Media.DATE_ADDED está en segundos, lo convertimos a milisegundos
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(
                    MediaStoreImage(
                        uriString = contentUri.toString(),
                        displayName = name,
                        mimeType = mimeType,
                        dateAdded = dateAdded
                    )
                )
            }
        }

        images
    }
}
