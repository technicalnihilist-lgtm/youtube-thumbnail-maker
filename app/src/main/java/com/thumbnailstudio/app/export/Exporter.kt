package com.thumbnailstudio.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.render.ThumbnailRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ExportFormat(val label: String, val mime: String, val ext: String) {
    PNG("PNG · lossless", "image/png", "png"),
    JPEG("JPEG · smaller", "image/jpeg", "jpg")
}

data class ExportResult(
    val file: File,
    val uri: Uri,
    val width: Int,
    val height: Int,
    val byteCount: Long,
    val savedToGallery: Boolean,
    val displayPath: String
)

/**
 * Renders and writes the final 1280x720 image.
 *
 * On Android 10+ the file lands in Pictures/Thumbnail Studio through MediaStore
 * (no storage permission needed). Older devices get an app-private copy that is
 * shared through the sharesheet.
 */
object Exporter {

    private const val ALBUM = "Thumbnail Studio"

    suspend fun export(
        context: Context,
        store: ProjectStore,
        project: Project,
        format: ExportFormat,
        quality: Int,
        baseName: String
    ): Result<ExportResult> = withContext(Dispatchers.Default) {
        runCatching {
            val width = project.width
            val height = project.height
            val bitmap = ThumbnailRenderer.renderToBitmap(project, width, height)
            try {
                val safeName = sanitize(baseName).ifBlank { "thumbnail" }
                val fileName = "$safeName.${format.ext}"
                val compressFormat = if (format == ExportFormat.PNG) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                // Cache copy is always written so the sharesheet has a URI.
                val cacheFile = store.saveExportCopy(bitmap, fileName, compressFormat, quality)
                    ?: error("Could not write export file")

                val gallery = saveToGallery(context, bitmap, fileName, format, quality)
                ExportResult(
                    file = cacheFile,
                    uri = gallery.second ?: fileUri(context, cacheFile),
                    width = width,
                    height = height,
                    byteCount = cacheFile.length(),
                    savedToGallery = gallery.first,
                    displayPath = if (gallery.first) "Pictures/$ALBUM/$fileName" else cacheFile.name
                )
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: ExportFormat,
        quality: Int
    ): Pair<Boolean, Uri?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-scoped-storage: keep an app-private copy, no permissions asked.
            return false to null
        }
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false to null
            resolver.openOutputStream(uri)?.use { out ->
                val compressFormat = if (format == ExportFormat.PNG) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, quality, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true to uri
        } catch (t: Throwable) {
            false to null
        }
    }

    fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareIntent(context: Context, result: ExportResult, format: ExportFormat): Intent {
        val uri = fileUri(context, result.file)
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Made with Thumbnail Studio")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(" ", "_").take(48)
}
