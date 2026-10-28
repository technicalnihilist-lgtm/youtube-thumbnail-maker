package com.thumbnailstudio.app.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.Templates
import com.thumbnailstudio.app.export.ExportFormat
import com.thumbnailstudio.app.export.ExportResult
import com.thumbnailstudio.app.export.Exporter
import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.ImageLayer
import com.thumbnailstudio.app.model.Layer
import com.thumbnailstudio.app.model.LayerOps
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.ShapeKind
import com.thumbnailstudio.app.model.ShapeLayer
import com.thumbnailstudio.app.model.StickerLayer
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.model.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Working : ExportUiState()
    data class Done(val result: ExportResult, val format: ExportFormat) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

/**
 * Editor state holder: project document, selection, undo/redo history,
 * debounced autosave and background export. Plain class + explicit scope so
 * the logic stays unit-testable without Android framework classes.
 */
class EditorController(
    private val store: ProjectStore,
    private val scope: CoroutineScope,
    initial: Project
) {
    var project by mutableStateOf(initial)
        private set

    var selectedId by mutableStateOf<String?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var savedAt by mutableStateOf(initial.updatedAt)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    var exportState by mutableStateOf<ExportUiState>(ExportUiState.Idle)

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()
    private var pendingSnapshot: Project? = null
    private var saveJob: Job? = null
    private val historyLimit = 40

    val selectedLayer: Layer? get() = selectedId?.let { project.layer(it) }

    // ------------------------------------------------------------- history

    private fun pushHistory(previous: Project) {
        undoStack.addLast(previous)
        if (undoStack.size > historyLimit) undoStack.removeFirst()
        redoStack.clear()
        canUndo = undoStack.isNotEmpty()
        canRedo = false
    }

    /** Call when a continuous gesture starts (drag, resize, rotate). */
    fun beginEdit() {
        pendingSnapshot = project
    }

    /** Call when the gesture ends; records a single undo entry. */
    fun endEdit() {
        val snapshot = pendingSnapshot ?: return
        pendingSnapshot = null
        if (snapshot != project) {
            pushHistory(snapshot)
            scheduleSave()
        }
    }

    fun mutate(recordHistory: Boolean = true, block: (Project) -> Project) {
        val previous = project
        val next = block(previous)
        if (next == previous) return
        project = next
        if (recordHistory) {
            pushHistory(previous)
            scheduleSave()
        }
    }

    fun updateLayer(layer: Layer, recordHistory: Boolean = true) {
        mutate(recordHistory) { it.replace(layer) }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(project)
        project = previous
        canUndo = undoStack.isNotEmpty()
        canRedo = true
        if (selectedId != null && project.layer(selectedId!!) == null) selectedId = null
        scheduleSave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(project)
        project = next
        canUndo = true
        canRedo = redoStack.isNotEmpty()
        if (selectedId != null && project.layer(selectedId!!) == null) selectedId = null
        scheduleSave()
    }

    // ----------------------------------------------------------- selection

    fun select(id: String?) {
        selectedId = id
    }

    // -------------------------------------------------------------- layers

    fun addText(text: String = "YOUR TITLE") {
        val layer = TextLayer(
            id = newId(),
            name = "Text",
            text = text,
            x = project.width / 2f,
            y = project.height / 2f,
            width = 780f,
            height = 200f,
            fontSize = 96f
        )
        mutate { it.add(layer) }
        selectedId = layer.id
    }

    fun addSticker(glyph: String) {
        val layer = StickerLayer(id = newId(), glyph = glyph, x = project.width / 2f, y = project.height / 2f)
        mutate { it.add(layer) }
        selectedId = layer.id
    }

    fun addShape(kind: ShapeKind) {
        val layer = ShapeLayer(
            id = newId(),
            kind = kind,
            name = kind.name.lowercase().replace('_', ' '),
            x = project.width / 2f,
            y = project.height / 2f,
            width = 420f,
            height = 240f,
            fill = 0xFF6D5BFF.toInt()
        )
        mutate { it.add(layer) }
        selectedId = layer.id
    }

    fun addImageFromUri(context: Context, uri: Uri) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { store.importImage(uri) } ?: return@launch
            val bounds = withContext(Dispatchers.IO) { com.thumbnailstudio.app.render.ImagePipeline.decode(path, 1600) }
            val aspect = if (bounds != null && bounds.height > 0) {
                bounds.width.toFloat() / bounds.height
            } else {
                16f / 9f
            }
            val width = 800f
            val height = (width / aspect).coerceIn(120f, project.height.toFloat())
            val layer = ImageLayer(
                id = newId(),
                path = path,
                x = project.width / 2f,
                y = project.height / 2f,
                width = width,
                height = height
            )
            mutate { it.add(layer) }
            selectedId = layer.id
        }
    }

    fun addBackgroundImageFromUri(context: Context, uri: Uri) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { store.importImage(uri) } ?: return@launch
            mutate {
                it.copy(background = it.background.copy(type = com.thumbnailstudio.app.model.BgType.IMAGE, imagePath = path))
            }
        }
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        mutate { it.remove(id) }
        selectedId = null
    }

    fun duplicateSelected() {
        val id = selectedId ?: return
        val original = project.layer(id) ?: return
        mutate { it.duplicate(id) }
        selectedId = project.layers.getOrNull(project.indexOf(id) + 1)?.id ?: original.id
    }

    fun toggleLock(id: String) {
        val layer = project.layer(id) ?: return
        mutate { it.replace(LayerOps.withLock(layer, !layer.locked)) }
    }

    fun toggleVisibility(id: String) {
        val layer = project.layer(id) ?: return
        mutate { it.replace(LayerOps.withVisibility(layer, !layer.visible)) }
    }

    fun reorder(from: Int, to: Int) {
        mutate { it.move(from, to) }
    }

    fun bringToFront(id: String) = mutate { it.bringToFront(id) }
    fun sendToBack(id: String) = mutate { it.sendToBack(id) }

    fun updateBackground(background: Background) {
        mutate { it.copy(background = background) }
    }

    fun applyTemplate(def: Templates.TemplateDef) {
        val fresh = Templates.instantiate(def)
        val renamed = fresh.copy(id = project.id, name = project.name)
        project = renamed
        pushHistory(project)
        selectedId = null
        scheduleSave()
    }

    fun rename(name: String) {
        mutate(recordHistory = false) { it.rename(name) }
        scheduleSave()
    }

    fun replaceProject(next: Project, recordHistory: Boolean = true) {
        if (next == project) return
        val previous = project
        project = next
        if (recordHistory) pushHistory(previous)
        scheduleSave()
    }

    // -------------------------------------------------------------- saving

    private fun scheduleSave() {
        saveJob?.cancel()
        isSaving = true
        saveJob = scope.launch {
            delay(600)
            persist()
        }
    }

    fun persistNow() {
        saveJob?.cancel()
        scope.launch { persist() }
    }

    private suspend fun persist() {
        val snapshot = project
        val ok = withContext(Dispatchers.IO) {
            val saved = store.save(snapshot)
            if (saved) store.saveThumbnail(snapshot)
            saved
        }
        isSaving = false
        if (ok) savedAt = System.currentTimeMillis()
    }

    // -------------------------------------------------------------- export

    fun export(
        context: Context,
        format: ExportFormat,
        quality: Int,
        fileName: String
    ) {
        if (exportState is ExportUiState.Working) return
        exportState = ExportUiState.Working
        scope.launch {
            val result = Exporter.export(context, store, project, format, quality, fileName)
            exportState = result.fold(
                onSuccess = { ExportUiState.Done(it, format) },
                onFailure = { ExportUiState.Error(it.message ?: "Export failed") }
            )
        }
    }

    fun resetExportState() {
        exportState = ExportUiState.Idle
    }
}
