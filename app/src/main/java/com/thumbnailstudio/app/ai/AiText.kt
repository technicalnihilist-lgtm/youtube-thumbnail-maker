package com.thumbnailstudio.app.ai

/**
 * Pure post-processing for the on-device OCR result. Kept free of Android and
 * ML Kit types so it can be unit-tested on the JVM.
 */
object AiText {

    private val filler = setOf(
        "the", "and", "a", "an", "of", "to", "in", "on", "for", "with", "is", "are"
    )

    /** Collapses OCR noise into at most [maxLines] usable headline lines. */
    fun headlineFromOcr(raw: String, maxLines: Int = 2): String {
        val lines = raw.lineSequence()
            .map { clean(it) }
            .filter { it.length >= 3 }
            .filter { line -> line.any { it.isLetter() } }
            .toList()
        if (lines.isEmpty()) return ""

        // Prefer the longest few lines: those are usually the real headline,
        // while OCR also picks up watermarks, timestamps and channel names.
        val picked = lines.sortedByDescending { it.length }.take(maxLines)
        val ordered = lines.filter { picked.contains(it) }
        return ordered.joinToString("\n").uppercase().take(90)
    }

    /** Single line summary used for the layer name. */
    fun layerName(text: String): String =
        text.lineSequence().firstOrNull()?.take(24)?.ifBlank { "Text from photo" } ?: "Text from photo"

    fun clean(line: String): String =
        line.replace(Regex("[\\u0000-\\u001F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('|', '-', '_', '.', ',', ':', ';', '·', '•')
            .trim()

    /** True when the line looks like a real caption rather than boilerplate. */
    fun looksLikeCaption(line: String): Boolean {
        val words = line.split(' ').filter { it.isNotBlank() }
        if (words.size < 2) return false
        val meaningful = words.count { it.lowercase() !in filler }
        return meaningful >= 1
    }
}
