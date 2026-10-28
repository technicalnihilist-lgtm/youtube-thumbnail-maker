package com.thumbnailstudio.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.ImageLayer
import com.thumbnailstudio.app.model.Layer
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.ShapeLayer
import com.thumbnailstudio.app.model.StickerLayer
import com.thumbnailstudio.app.model.TextLayer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Deterministic renderer for the 1280x720 canvas.
 *
 * The same code path draws the on-screen preview, the small feed preview and
 * the exported file, so what the user sees is exactly what they get.
 */
object ThumbnailRenderer {

    fun render(canvas: Canvas, project: Project) {
        drawBackground(canvas, project)
        for (layer in project.layers) {
            if (!layer.visible || layer.opacity <= 0.004f) continue
            drawLayer(canvas, layer, project)
        }
    }

    fun drawLayer(canvas: Canvas, layer: Layer, project: Project) {
        val alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
        val restore = canvas.saveLayerAlpha(0f, 0f, project.width.toFloat(), project.height.toFloat(), alpha)
        try {
            when (layer) {
                is TextLayer -> TextRenderer.draw(canvas, layer)
                is ImageLayer -> drawImage(canvas, layer)
                is ShapeLayer -> drawShape(canvas, layer)
                is StickerLayer -> drawSticker(canvas, layer)
            }
        } finally {
            canvas.restoreToCount(restore)
        }
    }

    fun drawBackground(canvas: Canvas, project: Project) {
        val w = project.width.toFloat()
        val h = project.height.toFloat()
        val bg = project.background
        when (bg.type) {
            BgType.SOLID -> canvas.drawColor(bg.color1)
            BgType.GRADIENT -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.shader = gradientShader(bg.angle, w, h, bg.color1, bg.color2)
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            BgType.IMAGE -> {
                canvas.drawColor(bg.color1)
                val path = bg.imagePath
                val bmp = if (path != null) ImagePipeline.decode(path, 1600) else null
                if (bmp != null) {
                    val source = if (bg.blur > 0.5f) ImagePipeline.blur(bmp, bg.blur) else bmp
                    drawCenterCrop(canvas, source, RectF(0f, 0f, w, h))
                }
            }
        }
        if (bg.darken > 0.004f) {
            val paint = Paint()
            paint.color = ((bg.darken.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0x000000
            canvas.drawRect(0f, 0f, w, h, paint)
        }
        if (bg.vignette > 0.004f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = hypot(w.toDouble(), h.toDouble()).toFloat() / 2f
            paint.shader = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(0x00000000, 0x00000000, ((bg.vignette.coerceIn(0f, 1f) * 230).toInt() shl 24)),
                floatArrayOf(0.45f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    fun gradientShader(angleDeg: Float, w: Float, h: Float, c1: Int, c2: Int): Shader {
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val half = (kotlin.math.abs(dx) * w + kotlin.math.abs(dy) * h) / 2f
        return LinearGradient(
            cx - dx * half, cy - dy * half,
            cx + dx * half, cy + dy * half,
            c1, c2, Shader.TileMode.CLAMP
        )
    }

    private fun drawImage(canvas: Canvas, layer: ImageLayer) {
        val bmp = ImagePipeline.graded(layer) ?: return
        val rect = RectF(
            layer.x - layer.width / 2f, layer.y - layer.height / 2f,
            layer.x + layer.width / 2f, layer.y + layer.height / 2f
        )
        canvas.save()
        canvas.rotate(layer.rotation, layer.x, layer.y)
        val clip = Path().apply {
            addRoundRect(rect, layer.cornerRadius.coerceAtLeast(0f), layer.cornerRadius.coerceAtLeast(0f), Path.Direction.CW)
        }
        canvas.clipPath(clip)
        drawCenterCrop(canvas, bmp, rect)
        canvas.restore()

        if (layer.strokeWidth > 0.2f) {
            canvas.save()
            canvas.rotate(layer.rotation, layer.x, layer.y)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = layer.strokeWidth
                color = layer.strokeColor
            }
            canvas.drawRoundRect(rect, layer.cornerRadius, layer.cornerRadius, paint)
            canvas.restore()
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bmp: Bitmap, rect: RectF) {
        val scale = maxOf(rect.width() / bmp.width, rect.height() / bmp.height)
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val left = rect.centerX() - dw / 2f
        val top = rect.centerY() - dh / 2f
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), paint)
    }

    private fun drawShape(canvas: Canvas, layer: ShapeLayer) {
        val w = layer.width.coerceAtLeast(1f)
        val h = layer.height.coerceAtLeast(1f)
        val path = ShapePaths.build(layer.kind, w, h, layer.cornerRadius)
        canvas.save()
        canvas.rotate(layer.rotation, layer.x, layer.y)
        canvas.translate(layer.x - w / 2f, layer.y - h / 2f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (layer.gradient) {
                shader = LinearGradient(0f, 0f, w, h, layer.fill, layer.fill2, Shader.TileMode.CLAMP)
            } else {
                color = layer.fill
            }
        }
        canvas.drawPath(path, fill)
        if (layer.strokeWidth > 0.2f) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = layer.strokeWidth
                color = layer.strokeColor
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, stroke)
        }
        canvas.restore()
    }

    private fun drawSticker(canvas: Canvas, layer: StickerLayer) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = layer.height.coerceAtLeast(12f) * 0.86f
            textAlign = Paint.Align.CENTER
            color = 0xFFFFFFFF.toInt()
        }
        canvas.save()
        canvas.rotate(layer.rotation, layer.x, layer.y)
        val metrics = paint.fontMetrics
        val baseline = layer.y - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(layer.glyph, layer.x, baseline, paint)
        canvas.restore()
    }

    /** Renders the project into a fresh bitmap at the requested pixel size. */
    fun renderToBitmap(project: Project, outWidth: Int, outHeight: Int, fillColor: Int = 0xFF000000.toInt()): Bitmap {
        val safeW = outWidth.coerceAtLeast(1)
        val safeH = outHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(fillColor)
        canvas.scale(safeW / project.width.toFloat(), safeH / project.height.toFloat())
        render(canvas, project)
        return bitmap
    }

    /** Small YouTube feed preview (default 320px wide, 16:9). */
    fun renderFeedPreview(project: Project, widthPx: Int = 320): Bitmap =
        renderToBitmap(project, widthPx, (widthPx * project.height / project.width.toFloat()).toInt())
}
