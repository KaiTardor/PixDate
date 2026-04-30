package com.example.pixdate.data.remote

/**
 * Post-procesador que analiza la descripción generada por AIVisionService
 * y extrae una categoría principal y tags relevantes.
 *
 * Funciona por análisis de palabras clave (keywords) sin modelo adicional.
 */
object CategoryClassifier {

    // Mapa de keywords → categoría
    private val categoryKeywords = mapOf(
        "ANIMALS" to listOf("dog", "cat", "bird", "horse", "cow", "sheep", "fish", "animal",
            "puppy", "kitten", "elephant", "bear", "rabbit", "deer", "lion", "tiger",
            "giraffe", "zebra", "monkey", "duck", "chicken", "pet", "wildlife"),
        "PEOPLE" to listOf("man", "woman", "person", "people", "child", "boy", "girl",
            "baby", "group", "couple", "crowd", "player", "worker", "kid", "lady"),
        "LANDSCAPE" to listOf("mountain", "beach", "ocean", "sea", "river", "lake", "forest",
            "sky", "sunset", "sunrise", "field", "valley", "hill", "desert", "snow",
            "cloud", "tree", "garden", "park", "nature", "landscape", "waterfall"),
        "FOOD" to listOf("food", "pizza", "cake", "plate", "bowl", "fruit", "vegetable",
            "sandwich", "bread", "coffee", "drink", "meal", "restaurant", "kitchen",
            "cooking", "wine", "cheese", "salad", "dessert"),
        "SPORTS" to listOf("sport", "ball", "soccer", "football", "basketball", "tennis",
            "baseball", "surfing", "skiing", "skateboard", "bike", "bicycle", "running",
            "swimming", "game", "playing", "athlete", "team", "court", "field"),
        "VEHICLES" to listOf("car", "truck", "bus", "train", "plane", "airplane", "boat",
            "ship", "motorcycle", "bicycle", "vehicle", "road", "street", "highway",
            "traffic", "parking", "driving"),
        "BUILDINGS" to listOf("building", "house", "church", "city", "tower", "bridge",
            "castle", "room", "window", "door", "wall", "street", "architecture",
            "office", "store", "shop", "hotel")
    )

    // Palabras irrelevantes que no aportan como tags
    private val stopWords = setOf(
        "a", "an", "the", "is", "are", "in", "on", "at", "of", "to", "and",
        "with", "that", "this", "it", "for", "from", "by", "as", "or", "be",
        "was", "were", "has", "have", "had", "do", "does", "did", "will",
        "would", "can", "could", "should", "may", "might", "very", "some",
        "there", "their", "they", "its", "his", "her", "your", "our"
    )

    data class ClassificationResult(
        val category: String,
        val tags: List<String>
    )

    /**
     * Clasifica la descripción generada por el modelo en una categoría
     * y extrae tags relevantes.
     */
    fun classify(description: String): ClassificationResult {
        val words = description.lowercase().split(Regex("[\\s,.:;!?]+"))
            .filter { it.length > 1 && it !in stopWords }

        // Buscar la categoría con más coincidencias
        var bestCategory = "OTHER"
        var bestScore = 0

        for ((category, keywords) in categoryKeywords) {
            val score = words.count { word -> keywords.any { keyword -> word.contains(keyword) } }
            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }

        // Extraer tags: palabras significativas únicas (máx 5)
        val tags = words
            .filter { it.length > 2 }
            .distinct()
            .take(5)

        return ClassificationResult(
            category = bestCategory,
            tags = tags
        )
    }
}
