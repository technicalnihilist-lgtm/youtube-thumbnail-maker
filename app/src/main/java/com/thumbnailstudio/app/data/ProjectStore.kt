package com.thumbnailstudio.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.render.ThumbnailRenderer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class ProjectSummary(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val category: String,
    val thumbFile: File?,
    val isTemplate: Boolean,
    val layerCount: Int
)

/**
 * File-based project storage. Each project is a single small JSON document,
 * which keeps the APK dependency-free (no Room/KSP codegen) while remaining
 * crash-safe through atomic temp-file writes.
 */
class ProjectStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    val projectsDir: File = File(context.filesDir, "projects").apply { mkdirs() }
    val imagesDir: File = File(context.filesDir, "images").apply { mkdirs() }
    val fontsDir: File = File(context.filesDir, "fonts").apply { mkdirs() }
    val thumbsDir: File = File(context.cacheDir, "thumbs").apply { mkdirs() }
    val exportsDir: File = File(context.cacheDir, "exports").apply { mkdirs() }

    fun projectFile(id: String) = File(projectsDir, "$id.json")

    fun thumbFile(id: String) = File(thumbsDir, "$id.png")

    fun list(includeTemplates: Boolean = false): List<ProjectSummary> {
        val files = projectsDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        return files.mapNotNull { file ->
            val project = runCatching { json.decodeFromString(Project.serializer(), file.readText()) }.getOrNull()
                ?: return@mapNotNull null
            if (!includeTemplates && project.isTemplate) return@mapNotNull null
            if (includeTemplates && !project.isTemplate) return@mapNotNull null
            ProjectSummary(
                id = project.id,
                name = project.name,
                updatedAt = project.updatedAt,
                category = project.category,
                thumbFile = thumbFile(project.id).takeIf { it.exists() },
                isTemplate = project.isTemplate,
                layerCount = project.layers.size
            )
        }.sortedByDescending { it.updatedAt }
    }

    fun load(id: String): Project? {
        val file = projectFile(id)
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(Project.serializer(), file.readText()) }.getOrNull()
    }

    fun save(project: Project): Boolean = runCatching {
        val payload = json.encodeToString(Project.serializer(), project)
        val target = projectFile(project.id)
        val tmp = File(projectsDir, "${project.id}.tmp")
        tmp.writeText(payload)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }.isSuccess

    fun delete(id: String): Boolean {
        val ok = projectFile(id).delete()
        thumbFile(id).delete()
        return ok
    }

    fun duplicate(project: Project, newName: String): Project {
        val copy = project.copy(
            id = UUID.randomUUID().toString().replace("-", "").take(10),
            name = newName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isTemplate = false
        )
        save(copy)
        return copy
    }

    /** Persists a rendered preview used by the project list. */
    fun saveThumbnail(project: Project): File? = runCatching {
        val bitmap: Bitmap = ThumbnailRenderer.renderFeedPreview(project, 480)
        val file = thumbFile(project.id)
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
        bitmap.recycle()
        file
    }.getOrNull()

    /** Copies a picked gallery image into app storage; returns the local path. */
    fun importImage(uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val ext = resolver.getType(uri)?.substringAfterLast('/')?.let { if (it == "jpeg") "jpg" else it } ?: "png"
        val target = File(imagesDir, "img_${UUID.randomUUID().toString().replace("-", "").take(12)}.$ext")
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        target.absolutePath
    }.getOrNull()

    fun importFont(uri: Uri): String? = runCatching {
        val name = "font_${UUID.randomUUID().toString().replace("-", "").take(8)}.ttf"
        val target = File(fontsDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    fun saveAsTemplate(project: Project, name: String): Project {
        val template = project.copy(
            id = UUID.randomUUID().toString().replace("-", "").take(10),
            name = name,
            isTemplate = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        save(template)
        saveThumbnail(template)
        return template
    }

    fun saveExportCopy(bitmap: Bitmap, fileName: String, format: Bitmap.CompressFormat, quality: Int): File? = runCatching {
        val file = File(exportsDir, fileName)
        file.outputStream().use { out -> bitmap.compress(format, quality, out) }
        file
    }.getOrNull()

    fun clearExportCache() {
        exportsDir.listFiles()?.forEach { it.delete() }
    }
}
