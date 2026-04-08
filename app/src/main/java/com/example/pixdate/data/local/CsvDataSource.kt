package com.example.pixdate.data.local

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Lee y parsea el CSV de datos de ejemplo desde la carpeta assets/.
 * Convierte cada fila del CSV en un CsvPhotoRow.
 */
class CsvDataSource {

    companion object {
        private const val TAG = "PIXDATE_CSV"
        private const val CSV_PATH = "sample_data/flickr8k_random_15.csv"
    }

    /**
     * Lee el CSV desde assets y devuelve una lista de CsvPhotoRow.
     * @param context Contexto de la aplicación para acceder a AssetManager.
     * @return Lista de filas parseadas del CSV.
     */
    fun readCsvFromAssets(context: Context): List<CsvPhotoRow> {
        val rows = mutableListOf<CsvPhotoRow>()

        try {
            val inputStream = context.assets.open(CSV_PATH)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Saltar la cabecera
            val header = reader.readLine()
            Log.d(TAG, "Cabecera CSV: $header")

            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val row = parseCsvLine(line)
                    if (row != null) {
                        rows.add(row)
                        Log.d(TAG, "Fila parseada: ${row.sampleId} - ${row.fileName}")
                    }
                }
                line = reader.readLine()
            }

            reader.close()
            Log.d(TAG, "Total filas leídas del CSV: ${rows.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo CSV desde assets: ${e.message}", e)
        }

        return rows
    }

    /**
     * Parsea una línea del CSV respetando comillas (campos que contienen comas).
     * Formato: sampleId,fileName,"captionReference",mainCategory,"tag1, tag2, tag3"
     */
    private fun parseCsvLine(line: String): CsvPhotoRow? {
        return try {
            val fields = splitCsvLine(line)

            if (fields.size < 5) {
                Log.w(TAG, "Línea con campos insuficientes: $line")
                return null
            }

            val sampleId = fields[0].trim().toInt()
            val fileName = fields[1].trim()
            val captionReference = fields[2].trim()
            val mainCategory = fields[3].trim()
            val tags = fields[4].trim()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            CsvPhotoRow(
                sampleId = sampleId,
                fileName = fileName,
                captionReference = captionReference,
                mainCategory = mainCategory,
                tags = tags
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parseando línea CSV: $line — ${e.message}")
            null
        }
    }

    /**
     * Divide una línea CSV respetando campos entre comillas.
     * Ejemplo: 1,archivo.jpg,"texto con , coma",CAT,"a, b, c"
     * Resultado: [1, archivo.jpg, texto con , coma, CAT, a, b, c]
     */
    private fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        // Añadir el último campo
        fields.add(current.toString())

        return fields
    }
}
