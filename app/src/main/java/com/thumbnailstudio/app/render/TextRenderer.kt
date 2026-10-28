package com.thumbnailstudio.app.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.thumbnailstudio.app.model.TextAlignH
import com.thumbnailstudio.app.model.TextLayer
import java.io.File
import kotlin.math.abs
import kotlin.math.min

/** The fonts bundled with the platform plus any font the user imports. */
object FontCatalog {

    data class FontOption(val key: String, val label: String, val family: String, val style: Int)

    val builtIn: List<FontOption> = listOf(
        FontOption("sans_black", "Impact", "sans-serif-black", Typeface.NORMAL),
        FontOption("sans_bold", "Grotesk", "sans-serif", Typeface.BOLD),
        FontOption("sans", "Neutral", "sans-serif", Typeface.NORMAL),
        FontOption("condensed", "Condensed", "sans-serif-condensed", Typeface.BOLD),
        FontOption("serif_bold", "Editorial", "serif", Typeface.BOLD),
        FontOption("serif", "Serif", "serif", Typeface.NORMAL),
        FontOption("mono", "Mono", "monospace", Typeface.BOLD),
        FontOption("casual", "Casual", "casual", Typeface.NORMAL)
    )

    private val cache = HashMap<String, Typeface>()

    fun typeface(layer: TextLayer): Typeface {
        val path = layer.customFontPath
        if (!path.isNullOrBlank() && File(path).exists()) {
            val key = "file:$path:${layer.bold}:${layer.italic}"
            cache[key]?.let { return it }
            val base = try {
                Typeface.createFromFile(path)
            } catch (t: Throwable) {
                Typeface.SANS_SERIF
            }
            val styled = Typeface.create(base, styleBits(layer))
            cache[key] = styled
            return styled
        }
        val option = builtIn.firstOrNull { it.key == layer.fontKey } ?: builtIn[0]
        val key = "builtin:${option.key}:${layer.bold}:${layer.italic}"
        cache[key]?.let { return it }
        val base = Typeface.create(option.family, option.style)
        val styled = Typeface.create(base, styleBits(layer))
        cache[key] = styled
        return styled
    }

    private fun styleBits(layer: TextLayer): Int {
        var bits = Typeface.NORMAL
        if (layer.bold) bits = bits or Typeface.BOLD
        if (layer.italic) bits = bits or Typeface.ITALIC
        return bits
    }

    fun clear() = cache.clear()
}

/**
 * Text layout and rendering: strokes, gradients, shadows, glow, extruded 3D
 * depth, background highlight boxes and curved (arc) text.
 */
object TextRenderer {

    fun displayText(layer: TextLayer): String =
        if (layer.uppercase) layer.text.uppercase() else layer.text

    fun basePaint(layer: TextLayer, fontSize: Float = layer.fontSize): TextPaint {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = fontSize
        paint.typeface = FontCatalog.typeface(layer)
        paint.letterSpacing = (layer.letterSpacing / 100f).coerceIn(-0.1f, 0.6f)
        paint.isUnderlineText = layer.underline
        paint.color = layer.color
        paint.textAlign = Paint.Align.CENTER
        return paint
    }

    fun buildLayout(layer: TextLayer, paint: TextPaint, width: Int): StaticLayout {
        val text = displayText(layer)
        val alignment = when (layer.align) {
            TextAlignH.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignH.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignH.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val builder = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(8))
            .setAlignment(alignment)
            .setLineSpacing(0f, layer.lineSpacing.coerceIn(0.6f, 3f))
            .setIncludePad(false)
        return builder.build()
    }

    /** Shrinks the point size until the block fits the layer box (auto-fit). */
    fun fittedSize(layer: TextLayer): Float {
        var size = layer.fontSize.coerceIn(8f, 400f)
        val maxWidth = layer.width.coerceAtLeast(24f)
        val maxHeight = layer.height.coerceAtLeast(24f) * 1.05f
        var guard = 0
        while (guard++ < 14) {
            val paint = basePaint(layer, size)
            val layout = buildLayout(layer, paint, maxWidth.toInt())
            val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
            if ((layout.height <= maxHeight && widest <= maxWidth + 1f) || size <= 9f) break
            size *= 0.9f
        }
        return size
    }

    fun measure(layer: TextLayer): Pair<Float, Float> {
        val size = fittedSize(layer)
        val paint = basePaint(layer, size)
        val layout = buildLayout(layer, paint, layer.width.toInt().coerceAtLeast(8))
        val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: layer.width
        return widest to layout.height.toFloat()
    }

    fun draw(canvas: Canvas, layer: TextLayer) {
        if (layer.text.isBlank()) return
        canvas.save()
        canvas.rotate(layer.rotation, layer.x, layer.y)
        if (abs(layer.curve) > 1.5f) drawCurved(canvas, layer) else drawBlock(canvas, layer)
        canvas.restore()
    }

    private fun drawBlock(canvas: Canvas, layer: TextLayer) {
        val size = fittedSize(layer)
        val paint = basePaint(layer, size)
        val width = layer.width.coerceAtLeast(24f).toInt()
        val layout = buildLayout(layer, paint, width)
        val blockW = min(width.toFloat(), (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: width.toFloat())
        val blockH = layout.height.toFloat()

        canvas.save()
        canvas.translate(layer.x - width / 2f, layer.y - blockH / 2f)

        if (layer.boxEnabled) {
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.boxColor }
            canvas.drawRoundRect(
                -layer.boxPadding, -layer.boxPadding,
                width + layer.boxPadding, blockH + layer.boxPadding,
                layer.boxCorner, layer.boxCorner, boxPaint
            )
        }

        // Extruded 3D depth: repeated offset copies in a darkened tone.
        if (layer.depth3d > 0.5f) {
            val depthPaint = TextPaint(paint)
            depthPaint.color = shade(layer.color, 0.45f)
            depthPaint.shader = null
            depthPaint.clearShadowLayer()
            depthPaint.maskFilter = null
            val depthLayout = buildLayout(layer, depthPaint, width)
            var i = layer.depth3d.toInt().coerceIn(0, 40)
            while (i > 0) {
                canvas.save()
                canvas.translate(i * 0.85f, i * 0.85f)
                depthLayout.draw(canvas)
                canvas.restore()
                i -= 1
            }
        }

        if (layer.glowEnabled) {
            val glowPaint = TextPaint(paint)
            glowPaint.color = layer.glowColor
            glowPaint.shader = null
            glowPaint.maskFilter = BlurMaskFilter(layer.glowRadius.coerceIn(2f, 80f), BlurMaskFilter.Blur.NORMAL)
            buildLayout(layer, glowPaint, width).draw(canvas)
        }

        if (layer.shadowEnabled) {
            paint.setShadowLayer(
                layer.shadowBlur.coerceIn(0.1f, 80f),
                layer.shadowDx, layer.shadowDy, layer.shadowColor
            )
        }

        if (layer.strokeEnabled && layer.strokeWidth > 0.2f) {
            val strokePaint = TextPaint(paint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = layer.strokeWidth * 2f
            strokePaint.strokeJoin = Paint.Join.ROUND
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.color = layer.strokeColor
            strokePaint.shader = null
            strokePaint.clearShadowLayer()
            buildLayout(layer, strokePaint, width).draw(canvas)
        }

        if (layer.gradient) {
            paint.shader = LinearGradient(
                0f, 0f, blockW, blockH,
                layer.color, layer.gradientColor,
                Shader.TileMode.CLAMP
            )
        }
        layout.draw(canvas)
        canvas.restore()
    }

    /** Characters placed along a circular arc; [TextLayer.curve] is -100..100. */
    private fun drawCurved(canvas: Canvas, layer: TextLayer) {
        val text = displayText(layer)
        if (text.isEmpty()) return
        val paint = basePaint(layer, fittedSize(layer))
        paint.textAlign = Paint.Align.CENTER
        val total = paint.measureText(text).coerceAtLeast(1f)
        val sweepDeg = (layer.curve.coerceIn(-100f, 100f) / 100f) * 150f
        val sweepRad = Math.toRadians(sweepDeg.toDouble()).toFloat()
        val radius = if (abs(sweepRad) > 0.02f) total / abs(sweepRad) else total
        val direction = if (layer.curve >= 0f) 1f else -1f
        val circleCenterY = layer.y + radius * direction

        canvas.save()
        canvas.translate(layer.x, 0f)
        var travelled = 0f
        for (ch in text) {
            val cw = paint.measureText(ch.toString())
            val mid = travelled + cw / 2f
            val angle = -sweepDeg / 2f + (mid / total) * sweepDeg
            canvas.save()
            canvas.rotate(angle, 0f, circleCenterY)
            val drawY = circleCenterY - radius * direction
            if (layer.strokeEnabled && layer.strokeWidth > 0.2f) {
                val sp = TextPaint(paint)
                sp.style = Paint.Style.STROKE
                sp.strokeWidth = layer.strokeWidth * 2f
                sp.color = layer.strokeColor
                canvas.drawText(ch.toString(), 0f, drawY, sp)
            }
            if (layer.shadowEnabled) {
                paint.setShadowLayer(layer.shadowBlur, layer.shadowDx, layer.shadowDy, layer.shadowColor)
            }
            canvas.drawText(ch.toString(), 0f, drawY, paint)
            canvas.restore()
            travelled += cw
        }
        canvas.restore()
    }

    fun shade(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return (a shl 24) or
            (r.toInt().coerceIn(0, 255) shl 16) or
            (g.toInt().coerceIn(0, 255) shl 8) or
            b.toInt().coerceIn(0, 255)
    }
}
