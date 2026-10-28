package com.thumbnailstudio.app.render

import android.graphics.Bitmap
import java.io.File
import kotlin.math.abs

/**
 * Traditional (non-AI) background removal helpers.
 *
 * Two honest tools live here:
 *  - [removeColorBackground]: flood-fill style chroma removal from the edges,
 *    the same idea as a magic wand in a paint program.
 *  - [eraseStroke]: paints transparent pixels along a brush path, used by the
 *    manual eraser.
 * Neither pretends to be a segmentation model.
 */
object Cutout {

    fun removeColorBackground(
        sourcePath: String,
        tolerance: Float,
        outputFile: File
    ): Boolean = runCatching {
        val src = ImagePipeline.decode(sourcePath, 1400) ?: return false
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample the four corners to guess the backdrop colour.
        val samples = intArrayOf(pixels[0], pixels[w - 1], pixels[(h - 1) * w], pixels[h * w - 1])
        val refR = samples.sumOf { (it shr 16) and 0xFF } / 4
        val refG = samples.sumOf { (it shr 8) and 0xFF } / 4
        val refB = samples.sumOf { it and 0xFF } / 4

        val threshold = (tolerance.coerceIn(0.02f, 1f) * 160f)
        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0

        fun matches(index: Int): Boolean {
            val c = pixels[index]
            val dr = abs(((c shr 16) and 0xFF) - refR)
            val dg = abs(((c shr 8) and 0xFF) - refG)
            val db = abs((c and 0xFF) - refB)
            return dr + dg + db <= threshold
        }

        fun seed(index: Int) {
            if (!visited[index] && matches(index)) {
                visited[index] = true
                queue[tail++] = index
            }
        }
        for (x in 0 until w) { seed(x); seed((h - 1) * w + x) }
        for (y in 0 until h) { seed(y * w); seed(y * w + w - 1) }

        while (head < tail) {
            val index = queue[head++]
            pixels[index] = 0x00000000
            val x = index % w
            val y = index / w
            if (x > 0) seed(index - 1)
            if (x < w - 1) seed(index + 1)
            if (y > 0) seed(index - w)
            if (y < h - 1) seed(index + w)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        outputFile.outputStream().use { stream ->
            out.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        out.recycle()
        true
    }.getOrDefault(false)

    /** Erases a soft circle of pixels; returns true when something changed. */
    fun eraseStroke(bitmap: Bitmap, x: Float, y: Float, radius: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val left = (x - radius).toInt().coerceAtLeast(0)
        val top = (y - radius).toInt().coerceAtLeast(0)
        val right = (x + radius).toInt().coerceAtMost(w - 1)
        val bottom = (y + radius).toInt().coerceAtMost(h - 1)
        if (right <= left || bottom <= top) return
        val rw = right - left + 1
        val rh = bottom - top + 1
        val buffer = IntArray(rw * rh)
        bitmap.getPixels(buffer, 0, rw, left, top, rw, rh)
        val r2 = radius * radius
        for (py in 0 until rh) {
            for (px in 0 until rw) {
                val dx = (left + px) - x
                val dy = (top + py) - y
                if (dx * dx + dy * dy <= r2) {
                    buffer[py * rw + px] = 0x00000000
                }
            }
        }
        bitmap.setPixels(buffer, 0, rw, left, top, rw, rh)
    }

    fun saveBitmap(bitmap: Bitmap, file: File): Boolean = runCatching {
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        true
    }.getOrDefault(false)
}
