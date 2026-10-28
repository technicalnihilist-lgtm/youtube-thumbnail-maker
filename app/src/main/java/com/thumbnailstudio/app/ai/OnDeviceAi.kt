package com.thumbnailstudio.app.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Genuine on-device machine learning.
 *
 * Both models run entirely on the phone: subject segmentation produces a
 * cutout of the main subject in a photo, and Latin text recognition reads text
 * that already exists in a photo so it can be edited. No image, no pixel and no
 * text ever leaves the device, and there is no API key or account anywhere.
 */
object OnDeviceAi {

    /** Play Services ships the segmentation model; OCR is bundled in the APK. */
    fun segmentationAvailable(context: Context): Boolean = try {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
    } catch (t: Throwable) {
        false
    }

    /**
     * Returns a new bitmap of the dominant subject with a transparent
     * background, or null when nothing could be segmented.
     */
    suspend fun cutoutSubject(source: Bitmap): Bitmap? {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .enableForegroundConfidenceMask()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        val image = InputImage.fromBitmap(source, 0)
        return try {
            suspendCancellableCoroutine { continuation ->
                segmenter.process(image)
                    .addOnSuccessListener { result ->
                        val bitmap = runCatching {
                            result.foregroundBitmap ?: maskToCutout(source, result.foregroundConfidenceMask)
                        }.getOrNull()
                        if (continuation.isActive) continuation.resume(bitmap)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            runCatching { segmenter.close() }
        }
    }

    /** Fallback path: build an alpha-masked copy from the confidence mask. */
    private fun maskToCutout(source: Bitmap, mask: java.nio.FloatBuffer?): Bitmap? {
        if (mask == null) return null
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val confidence = FloatArray(mask.remaining())
        mask.get(confidence)
        if (confidence.size < width * height) return null
        for (i in pixels.indices) {
            val alpha = (confidence[i].coerceIn(0f, 1f) * 255f).toInt()
            pixels[i] = (alpha shl 24) or (pixels[i] and 0x00FFFFFF)
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /** On-device OCR: the text already visible in a photo. */
    suspend fun readText(source: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(source, 0)
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) continuation.resume(text.text)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            runCatching { recognizer.close() }
        }
    }

    fun savePng(bitmap: Bitmap, directory: File, prefix: String): String? = runCatching {
        directory.mkdirs()
        val file = File(directory, "${prefix}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        file.absolutePath
    }.getOrNull()
}
