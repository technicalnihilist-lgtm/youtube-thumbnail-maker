package com.thumbnailstudio.app.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.thumbnailstudio.app.model.ImageLayer
import java.io.File

/**
 * Decodes, caches and colour-grades image layers.
 *
 * Everything here is plain `android.graphics` work: no native libraries, no
 * models, no network. Bitmaps are decoded at a bounded resolution so that a
 * handful of 4K photos on a low-RAM device still fit comfortably in memory.
 */
object ImagePipeline {

    private val cache: LruCache<String, Bitmap> by lazy {
        val budgetKb = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()
            .coerceIn(16 * 1024, 96 * 1024)
        object : LruCache<String, Bitmap>(budgetKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    fun clearCache() = cache.evictAll()

    fun cacheSizeKb(): Int = cache.size()

    /** Decodes a file, honouring EXIF rotation, downsampled to [maxDim]. */
    fun decode(path: String, maxDim: Int = 1600): Bitmap? {
        val key = "raw:$path:$maxDim"
        cache.get(key)?.let { return it }
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / (sample * 2) >= maxDim || h / (sample * 2) >= maxDim) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = try {
            BitmapFactory.decodeFile(path, opts)
        } catch (oom: OutOfMemoryError) {
            null
        } ?: return null

        val upright = applyExif(decoded, path)
        cache.put(key, upright)
        return upright
    }

    private fun applyExif(src: Bitmap, path: String): Bitmap {
        val orientation = try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (t: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return src
        }
        return try {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (t: Throwable) {
            src
        }
    }

    /** Colour-graded bitmap for an image layer, cached by its effect signature. */
    fun graded(layer: ImageLayer, maxDim: Int = 1600): Bitmap? {
        val key = "grade:${layer.path}:$maxDim:${layer.brightness}:${layer.contrast}:" +
            "${layer.saturation}:${layer.hue}:${layer.sharpen}:${layer.flipH}"
        cache.get(key)?.let { return it }

        val src = decode(layer.path, maxDim) ?: return null
        var out = src
        if (layer.flipH) {
            val m = Matrix().apply { postScale(-1f, 1f) }
            out = Bitmap.createBitmap(out, 0, 0, out.width, out.height, m, true)
        }
        if (layer.brightness != 0f || layer.contrast != 0f || layer.saturation != 0f || layer.hue != 0f) {
            out = applyMatrix(out, colorMatrix(layer))
        }
        if (layer.sharpen > 0.01f) {
            out = sharpen(out, layer.sharpen.coerceIn(0f, 1f))
        }
        cache.put(key, out)
        return out
    }

    fun colorMatrix(layer: ImageLayer): ColorMatrix {
        val cm = ColorMatrix()
        if (layer.saturation != 0f) {
            val s = 1f + layer.saturation.coerceIn(-1f, 2f)
            val sat = ColorMatrix()
            sat.setSaturation(s)
            cm.postConcat(sat)
        }
        if (layer.hue != 0f) {
            val hue = ColorMatrix()
            hue.setRotate(0, layer.hue.coerceIn(-180f, 180f))
            cm.postConcat(hue)
        }
        if (layer.brightness != 0f || layer.contrast != 0f) {
            val b = layer.brightness.coerceIn(-1f, 1f) * 255f
            val c = 1f + layer.contrast.coerceIn(-1f, 1f)
            val t = (1f - c) * 128f
            val bc = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, b + t,
                    0f, c, 0f, 0f, b + t,
                    0f, 0f, c, 0f, b + t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(bc)
        }
        return cm
    }

    private fun applyMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Classic 3x3 unsharp kernel. Traditional image processing, not AI. */
    fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src
        val a = amount * 0.9f
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val result = IntArray(w * h)
        val c = 1f + 4f * a
        val n = -a
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val center = pixels[i]
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    result[i] = center
                    continue
                }
                val up = pixels[i - w]
                val down = pixels[i + w]
                val left = pixels[i - 1]
                val right = pixels[i + 1]
                var r = c * ((center shr 16) and 0xFF) + n * (((up shr 16) and 0xFF) + ((down shr 16) and 0xFF) +
                    ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF))
                var g = c * ((center shr 8) and 0xFF) + n * (((up shr 8) and 0xFF) + ((down shr 8) and 0xFF) +
                    ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF))
                var b = c * (center and 0xFF) + n * ((up and 0xFF) + (down and 0xFF) + (left and 0xFF) + (right and 0xFF))
                r = r.coerceIn(0f, 255f); g = g.coerceIn(0f, 255f); b = b.coerceIn(0f, 255f)
                result[i] = (center and 0xFF000000.toInt()) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
            }
        }
        out.setPixels(result, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Fast blur used for background softening: progressive downscale then
     * bilinear upscale. Cheap, allocation-light and visually smooth.
     */
    fun blur(src: Bitmap, radius: Float): Bitmap {
        if (radius <= 0.5f) return src
        val factor = (1f + radius / 6f).coerceIn(1f, 24f)
        val smallW = (src.width / factor).toInt().coerceAtLeast(2)
        val smallH = (src.height / factor).toInt().coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val out = Bitmap.createScaledBitmap(small, src.width, src.height, true)
        if (small != out) small.recycle()
        return out
    }
}
