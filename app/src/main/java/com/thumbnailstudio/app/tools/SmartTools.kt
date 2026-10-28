package com.thumbnailstudio.app.tools

import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.ImageLayer
import com.thumbnailstudio.app.model.Layer
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.TextLayer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Rule-based design intelligence.
 *
 * These are deterministic heuristics (WCAG contrast maths, colour-harmony
 * rules and layout templates) — traditional design assistance, explicitly not
 * a generative AI model. Everything is pure Kotlin so it is unit-testable and
 * runs offline in microseconds.
 */
object SmartTools {

    enum class Harmony { COMPLEMENTARY, ANALOGOUS, TRIADIC, MONOCHROME }

    data class ContrastIssue(
        val layerId: String,
        val layerName: String,
        val ratio: Double,
        val suggestedColor: Int
    )

    data class LayoutSuggestion(
        val id: String,
        val title: String,
        val description: String,
        val apply: (Project) -> Project
    )

    // ---------------------------------------------------------------- colour

    fun alpha(color: Int): Int = (color ushr 24) and 0xFF
    fun red(color: Int): Int = (color shr 16) and 0xFF
    fun green(color: Int): Int = (color shr 8) and 0xFF
    fun blue(color: Int): Int = color and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun opaque(color: Int): Int = argb(255, red(color), green(color), blue(color))

    /** WCAG 2.1 relative luminance. */
    fun relativeLuminance(color: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(color)) + 0.7152 * channel(green(color)) + 0.0722 * channel(blue(color))
    }

    /** WCAG contrast ratio, always >= 1.0. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = max(la, lb)
        val lo = min(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    fun readabilityLabel(ratio: Double): String = when {
        ratio >= 7.0 -> "Excellent"
        ratio >= 4.5 -> "Good"
        ratio >= 3.0 -> "Low"
        else -> "Poor"
    }

    /** Black or white, whichever reads better on [background]. */
    fun bestForeground(background: Int): Int =
        if (contrastRatio(0xFFFFFFFF.toInt(), background) >= contrastRatio(0xFF000000.toInt(), background)) {
            0xFFFFFFFF.toInt()
        } else {
            0xFF000000.toInt()
        }

    fun toHex(color: Int): String = String.format("#%06X", color and 0xFFFFFF)

    fun parseHex(value: String, fallback: Int = 0xFF000000.toInt()): Int {
        val cleaned = value.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                6 -> (0xFF000000.toInt()) or cleaned.toLong(16).toInt()
                8 -> cleaned.toLong(16).toInt()
                else -> fallback
            }
        } catch (t: Throwable) {
            fallback
        }
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        return argb(
            255,
            (red(a) + (red(b) - red(a)) * f).toInt(),
            (green(a) + (green(b) - green(a)) * f).toInt(),
            (blue(a) + (blue(b) - blue(a)) * f).toInt()
        )
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        argb((alpha.coerceIn(0f, 1f) * 255).toInt(), red(color), green(color), blue(color))

    fun shift(color: Int, degrees: Float): Int {
        val hsv = FloatArray(3)
        hsv[0] = hueOf(color)
        hsv[1] = saturationOf(color)
        hsv[2] = valueOf(color)
        hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
        return fromHsv(hsv[0], hsv[1], hsv[2])
    }

    fun hueOf(color: Int): Float {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        if (d < 0.0001f) return 0f
        val h = when (mx) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        return (h * 60f + 360f) % 360f
    }

    fun saturationOf(color: Int): Float {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val mx = max(r, max(g, b))
        return if (mx <= 0f) 0f else (mx - min(r, min(g, b))) / mx
    }

    fun valueOf(color: Int): Float =
        max(red(color) / 255f, max(green(color) / 255f, blue(color) / 255f))

    fun fromHsv(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val hh = (h % 360f + 360f) % 360f / 60f
        val x = c * (1 - abs(hh % 2 - 1))
        val (r1, g1, b1) = when (hh.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = v - c
        return argb(255, ((r1 + m) * 255).toInt(), ((g1 + m) * 255).toInt(), ((b1 + m) * 255).toInt())
    }

    /** Colour-harmony palette derived from one base colour. */
    fun harmonize(base: Int, harmony: Harmony, count: Int = 5): List<Int> {
        val h = hueOf(base)
        val s = saturationOf(base).coerceIn(0.35f, 0.9f)
        val v = valueOf(base).coerceIn(0.35f, 0.95f)
        val hues = when (harmony) {
            Harmony.COMPLEMENTARY -> listOf(0f, 180f, 12f, 192f, 24f)
            Harmony.ANALOGOUS -> listOf(0f, 30f, 330f, 60f, 300f)
            Harmony.TRIADIC -> listOf(0f, 120f, 240f, 20f, 140f)
            Harmony.MONOCHROME -> listOf(0f, 0f, 0f, 0f, 0f)
        }
        val values = when (harmony) {
            Harmony.MONOCHROME -> listOf(0.95f, 0.78f, 0.6f, 0.45f, 0.3f)
            else -> listOf(0.95f, 0.85f, 0.75f, 0.65f, 0.55f)
        }
        return (0 until min(count, hues.size)).map { i ->
            fromHsv(h + hues[i], if (harmony == Harmony.MONOCHROME) s * 0.6f else s, values[i] * v + 0.05f)
        }
    }

    /** Collapses a list of sampled pixels into up to [max] distinct swatches. */
    fun quantizePalette(samples: List<Int>, max: Int = 5): List<Int> {
        if (samples.isEmpty()) return emptyList()
        val buckets = HashMap<Int, IntArray>() // key -> [count, r, g, b]
        for (c in samples) {
            if (alpha(c) < 40) continue
            val key = (red(c) / 32) shl 6 or (green(c) / 32) shl 3 or (blue(c) / 32)
            val entry = buckets.getOrPut(key) { intArrayOf(0, 0, 0, 0) }
            entry[0]++
            entry[1] += red(c)
            entry[2] += green(c)
            entry[3] += blue(c)
        }
        return buckets.values
            .filter { it[0] > 0 }
            .sortedByDescending { it[0] }
            .take(max)
            .map { argb(255, it[1] / it[0], it[2] / it[0], it[3] / it[0]) }
    }

    // ------------------------------------------------------------ background

    /** Representative background colours behind the whole canvas. */
    fun backgroundColors(project: Project): List<Int> {
        val bg = project.background
        return when (bg.type) {
            BgType.SOLID -> listOf(opaque(bg.color1))
            BgType.GRADIENT -> listOf(opaque(bg.color1), opaque(bg.color2))
            BgType.IMAGE -> {
                val path = bg.imagePath
                val bmp = if (path != null) com.thumbnailstudio.app.render.ImagePipeline.decode(path, 96) else null
                if (bmp != null) {
                    val samples = ArrayList<Int>(bmp.width * bmp.height)
                    val row = IntArray(bmp.width)
                    for (y in 0 until bmp.height) {
                        bmp.getPixels(row, 0, bmp.width, 0, y, bmp.width, 1)
                        samples.addAll(row.toList())
                    }
                    quantizePalette(samples, 3)
                } else {
                    listOf(opaque(bg.color1))
                }
            }
        }
    }

    /** Worst-case contrast of a text colour against the background. */
    fun worstContrast(textColor: Int, backgrounds: List<Int>): Double =
        backgrounds.minOfOrNull { contrastRatio(opaque(textColor), it) } ?: 21.0

    fun contrastIssues(project: Project): List<ContrastIssue> {
        val backgrounds = backgroundColors(project)
        if (backgrounds.isEmpty()) return emptyList()
        return project.layers.filterIsInstance<TextLayer>().filter { it.visible }.mapNotNull { layer ->
            val ratio = worstContrast(layer.color, backgrounds)
            if (ratio < 4.5) {
                ContrastIssue(layer.id, layer.name, ratio, bestForeground(backgrounds.first()))
            } else {
                null
            }
        }
    }

    /** Applies the smallest change that lifts every text layer to WCAG AA. */
    fun autoFixContrast(project: Project): Project {
        val backgrounds = backgroundColors(project)
        if (backgrounds.isEmpty()) return project
        var result = project
        for (layer in project.layers.filterIsInstance<TextLayer>()) {
            val ratio = worstContrast(layer.color, backgrounds)
            if (ratio < 4.5) {
                val fixed = bestForeground(backgrounds.first())
                result = result.replace(layer.copy(color = fixed, shadowEnabled = true, shadowColor = 0xCC000000.toInt()))
            }
        }
        return result
    }

    /** Adds a translucent scrim behind low-contrast text instead of recolouring it. */
    fun addReadabilityScrim(project: Project): Project {
        var result = project
        for (layer in project.layers.filterIsInstance<TextLayer>()) {
            if (worstContrast(layer.color, backgroundColors(project)) < 4.5) {
                result = result.replace(layer.copy(boxEnabled = true, boxColor = 0xB3000000.toInt(), boxPadding = 30f))
            }
        }
        return result
    }

    fun suggestedBackgroundFor(textColor: Int): Int {
        val fg = opaque(textColor)
        return if (relativeLuminance(fg) > 0.5) {
            fromHsv(hueOf(fg), 0.55f, 0.18f)
        } else {
            fromHsv(hueOf(fg), 0.12f, 0.96f)
        }
    }

    // ---------------------------------------------------------------- layout

    fun layoutSuggestions(project: Project): List<LayoutSuggestion> {
        val texts = project.layers.filterIsInstance<TextLayer>()
        val images = project.layers.filterIsInstance<ImageLayer>()
        val suggestions = mutableListOf<LayoutSuggestion>()

        suggestions += LayoutSuggestion(
            "title_top",
            "Title top, focus below",
            "Headline in the upper third, visual weight underneath"
        ) { p ->
            var out = p
            texts.sortedBy { it.y }.forEachIndexed { i, t ->
                val targetY = if (i == 0) 168f else 300f + i * 90f
                out = out.replace(t.copy(y = targetY, x = 640f))
            }
            images.forEach { img ->
                out = out.replace(img.copy(x = 640f, y = 470f, width = 700f, height = 394f))
            }
            out
        }

        suggestions += LayoutSuggestion(
            "split",
            "Left copy, right visual",
            "Text anchored left, image occupying the right half"
        ) { p ->
            var out = p
            texts.sortedBy { it.y }.forEachIndexed { i, t ->
                out = out.replace(
                    t.copy(
                        x = 360f,
                        y = 250f + i * (t.height * 0.85f + 24f),
                        width = min(t.width, 620f),
                        align = com.thumbnailstudio.app.model.TextAlignH.LEFT
                    )
                )
            }
            images.forEach { img ->
                out = out.replace(img.copy(x = 940f, y = 360f, width = 560f, height = 620f))
            }
            out
        }

        suggestions += LayoutSuggestion(
            "center_stack",
            "Centred stack",
            "Balanced, symmetrical composition for clean titles"
        ) { p ->
            var out = p
            val ordered = texts.sortedBy { it.y }
            val total = ordered.sumOf { (it.height * 0.85).toDouble() }.toFloat()
            var cursor = 360f - total / 2f
            ordered.forEach { t ->
                val h = t.height * 0.85f
                out = out.replace(
                    t.copy(
                        x = 640f,
                        y = cursor + h / 2f,
                        align = com.thumbnailstudio.app.model.TextAlignH.CENTER
                    )
                )
                cursor += h + 20f
            }
            out
        }

        suggestions += LayoutSuggestion(
            "big_impact",
            "Big impact headline",
            "One oversized headline that survives the mobile feed"
        ) { p ->
            var out = p
            val first = texts.minByOrNull { it.y }
            if (first != null) {
                out = out.replace(
                    first.copy(
                        x = 640f, y = 400f, width = 1120f,
                        fontSize = 150f, height = 300f,
                        strokeEnabled = true, strokeWidth = 10f
                    )
                )
            }
            out
        }
        return suggestions
    }

    /** 0..100 heuristic quality score used by the editor's checklist. */
    fun scoreThumbnail(project: Project): Int {
        var score = 100
        val visible = project.layers.filter { it.visible }
        if (visible.size > 9) score -= (visible.size - 9) * 3
        val texts = visible.filterIsInstance<TextLayer>()
        if (texts.isEmpty()) score -= 30
        val backgrounds = backgroundColors(project)
        for (t in texts) {
            val ratio = worstContrast(t.color, backgrounds)
            if (ratio < 3.0) score -= 14 else if (ratio < 4.5) score -= 7
            if (t.fontSize < 40f) score -= 5
            if (t.x - t.width / 2f < 24f || t.x + t.width / 2f > project.width - 24f) score -= 6
            if (t.y - t.height / 2f < 24f || t.y + t.height / 2f > project.height - 24f) score -= 6
        }
        return score.coerceIn(0, 100)
    }

    fun checklist(project: Project): List<Pair<String, Boolean>> {
        val texts = project.layers.filterIsInstance<TextLayer>()
        val backgrounds = backgroundColors(project)
        val readable = texts.isNotEmpty() && texts.all { worstContrast(it.color, backgrounds) >= 4.5 }
        val safe = project.layers.filter { it.visible }.all {
            it.x - it.width / 2f >= 0f && it.x + it.width / 2f <= project.width &&
                it.y - it.height / 2f >= 0f && it.y + it.height / 2f <= project.height
        }
        return listOf(
            "Has a headline" to texts.isNotEmpty(),
            "Text passes WCAG AA contrast" to readable,
            "Everything inside the frame" to safe,
            "Layers under control" to (project.layers.count { it.visible } in 1..9)
        )
    }

    /** Rotates a design by 180 degrees — quick alternate version. */
    fun mirrorAlternate(project: Project): Project {
        var out = project
        for (layer in project.layers) {
            out = out.replace(
                when (layer) {
                    is TextLayer -> layer.copy(x = project.width - layer.x)
                    is ImageLayer -> layer.copy(x = project.width - layer.x)
                    is com.thumbnailstudio.app.model.ShapeLayer -> layer.copy(x = project.width - layer.x)
                    is com.thumbnailstudio.app.model.StickerLayer -> layer.copy(x = project.width - layer.x)
                }
            )
        }
        return out
    }

    fun layerLabel(layer: Layer): String = when (layer) {
        is TextLayer -> layer.text.lineSequence().firstOrNull()?.take(18)?.ifBlank { "Text" } ?: "Text"
        is ImageLayer -> "Photo"
        is com.thumbnailstudio.app.model.ShapeLayer -> layer.kind.name.lowercase().replace('_', ' ')
        is com.thumbnailstudio.app.model.StickerLayer -> "Sticker " + layer.glyph
    }
}
