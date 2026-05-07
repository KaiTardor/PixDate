package com.example.pixdate.data.remote

import android.util.Base64
import android.util.Log
import com.example.pixdate.BuildConfig
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Servicio que conecta con la API de Google Gemini para analizar imágenes.
 *
 * Devuelve un objeto [ImageAnalysis] con descripción, categoría y tags extraídos
 * directamente del JSON que Gemini genera (ya no hay post-procesado heurístico).
 */
class AIVisionService {

    companion object {
        private const val TAG = "PIXDATE_AI"
        private const val MODEL = "gemini-2.5-flash"
    }

    data class ImageAnalysis(
        val description: String,
        val category: String,
        val tags: List<String>
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * Envía los bytes de una imagen a Gemini y devuelve un [ImageAnalysis] estructurado.
     *
     * @param imageBytes Bytes de la imagen.
     * @param existingFolders Lista de nombres de carpetas que ya existen en la app.
     * @param mimeType MIME type real de la imagen.
     */
    fun analyze(
        imageBytes: ByteArray,
        existingFolders: List<String>,
        mimeType: String? = null
    ): Result<ImageAnalysis> {
        val token = BuildConfig.GEMINI_API_KEY
        if (token.isBlank()) {
            return Result.failure(
                Exception(
                    "Consigue tu API Key de Gemini en https://aistudio.google.com/app/apikey"
                )
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$token"
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // Resolvemos el MIME type: usamos el del caller, o lo detectamos de los magic bytes
        val resolvedMimeType = mimeType?.takeIf { it.isNotBlank() }
            ?: detectMimeType(imageBytes)

        // Construimos la lista de carpetas para el prompt
        val foldersPrompt = if (existingFolders.isNotEmpty()) {
            "Existing categories you can reuse: ${existingFolders.joinToString(", ")}."
        } else {
            "No categories exist yet."
        }

        val prompt = """
            Analyze this photo and respond ONLY with a valid JSON object. Do not add any explanation, markdown, or text outside the JSON.

            $foldersPrompt

            Use this exact schema:
            {
              "description": "<one concise sentence describing what is in the image, in English>",
              "category": "<category name>",
              "tags": ["<tag1>", "<tag2>", "<tag3>", "<tag4>", "<tag5>"]
            }

            Rules for "category":
            1. If the image clearly fits into one of the existing categories listed above, use that EXACT name.
            2. If it does NOT fit well, create a NEW, single-word category name in ALL CAPS (e.g., 'TRAVEL', 'NIGHT', 'ART').
            3. Use the most logical and general category possible.

            Rules for others:
            - "description": one concise sentence in English.
            - "tags": 3 to 5 lowercase English keywords.
            - Output only the JSON object.
        """.trimIndent()
        // ────────────────────────────────────────────────────────────────────────

        val jsonBody = """
        {
          "contents": [
            {
              "parts": [
                {"text": ${jsonStringLiteral(prompt)}},
                {
                  "inline_data": {
                    "mime_type": "$resolvedMimeType",
                    "data": "$base64Image"
                  }
                }
              ]
            }
          ],
          "generationConfig": {
            "temperature": 0.2,
            "response_mime_type": "application/json"
          }
        }
        """.trimIndent()

        Log.d(TAG, "Enviando petición a Gemini... (Modelo: $MODEL)")
        
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Respuesta recibida de la red. Código: ${response.code}")
                
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    // 429: rate limit de la API — mensaje específico para que el usuario entienda
                    if (response.code == 429) {
                        return Result.failure(
                            Exception("Límite de peticiones alcanzado. Espera unos minutos y vuelve a intentarlo.")
                        )
                    }
                    Log.e(TAG, "Error Gemini HTTP ${response.code}: $body")
                    return Result.failure(Exception("Error de Gemini HTTP ${response.code}"))
                }

                val analysis = parseGeminiResponse(body)
                Log.d(TAG, "Análisis completado: category=${analysis.category}, tags=${analysis.tags}")
                Result.success(analysis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red o parseo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extrae el texto generado de la respuesta de Gemini y lo parsea como [ImageAnalysis].
     * Si el JSON de Gemini no tiene el campo esperado, aplica valores de fallback.
     */
    private fun parseGeminiResponse(rawBody: String): ImageAnalysis {
        val root = JsonParser.parseString(rawBody).asJsonObject
        val candidates = root.getAsJsonArray("candidates")

        val text = candidates[0]
            .asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")[0]
            .asJsonObject
            .get("text").asString.trim()

        Log.d(TAG, "Texto bruto de Gemini: $text")

        return try {
            val parsed = JsonParser.parseString(text).asJsonObject

            val description = parsed.get("description")?.asString?.trim()
                ?: "No description available"

            val category = parsed.get("category")?.asString?.trim()?.uppercase() ?: "OTHER"

            val tags = parsed.getAsJsonArray("tags")
                ?.mapNotNull { it.asString?.lowercase()?.trim() }
                ?.filter { it.isNotBlank() }
                ?.take(5)
                ?: emptyList()

            ImageAnalysis(description = description, category = category, tags = tags)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo parsear el JSON de Gemini, usando fallback. Raw: $text")
            // Fallback: si Gemini no devuelve JSON válido por algún motivo,
            // usamos el texto entero como descripción y "OTHER" como categoría.
            ImageAnalysis(description = text, category = "OTHER", tags = emptyList())
        }
    }

    /**
     * Escapa una cadena para insertarla de forma segura dentro de un JSON string literal.
     */
    private fun jsonStringLiteral(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    /**
     * Detecta el MIME type de una imagen a partir de sus primeros bytes (magic bytes).
     */
    private fun detectMimeType(bytes: ByteArray): String {
        if (bytes.size < 4) return "image/jpeg"
        return when {
            // JPEG: FF D8 FF
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() ->
                "image/jpeg"

            // PNG: 89 50 4E 47
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() ->
                "image/png"

            // WEBP: 52 49 46 46 ... 57 45 42 50
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() ->
                "image/webp"

            // GIF: 47 49 46 38
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() ->
                "image/gif"

            else -> "image/jpeg"
        }
    }
}
