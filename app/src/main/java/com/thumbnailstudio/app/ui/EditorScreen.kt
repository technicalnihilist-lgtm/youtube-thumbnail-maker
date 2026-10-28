package com.thumbnailstudio.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.SettingsStore
import com.thumbnailstudio.app.export.ExportFormat
import com.thumbnailstudio.app.export.Exporter
import com.thumbnailstudio.app.model.Layer
import com.thumbnailstudio.app.model.LayerOps
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.render.ThumbnailRenderer
import com.thumbnailstudio.app.ui.theme.Violet
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class EditorTab(val label: String) {
    ADD("Add"), TEXT("Text"), IMAGE("Photo"), SHAPE("Shape"),
    STICKER("Sticker"), BG("Backdrop"), LAYERS("Layers"), TOOLS("Smart")
}

@Composable
fun EditorScreen(
    store: ProjectStore,
    settings: SettingsStore,
    initial: Project,
    onClose: () -> Unit,
    onExported: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(initial.id) { EditorController(store, scope, initial) }
    val project = controller.project
    var tab by remember { mutableStateOf(EditorTab.ADD) }
    var showExport by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTextEditor by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val selected = controller.selectedLayer

    LaunchedEffect(controller.selectedId) {
        tab = when (selected) {
            is TextLayer -> EditorTab.TEXT
            is com.thumbnailstudio.app.model.ImageLayer -> EditorTab.IMAGE
            is com.thumbnailstudio.app.model.ShapeLayer -> EditorTab.SHAPE
            is com.thumbnailstudio.app.model.StickerLayer -> EditorTab.STICKER
            null -> tab
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.persistNow() }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) controller.addImageFromUri(context, uri)
    }
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) controller.addBackgroundImageFromUri(context, uri)
    }
    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val current = controller.selectedLayer
            if (current is com.thumbnailstudio.app.model.ImageLayer) {
                scope.launch {
                    val path = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.importImage(uri) }
                    if (path != null) controller.updateLayer(current.copy(path = path))
                }
            }
        }
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val current = controller.selectedLayer
            scope.launch {
                val path = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.importFont(uri) }
                if (path != null && current is TextLayer) {
                    controller.updateLayer(current.copy(customFontPath = path))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ---------------------------------------------------------- top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                controller.persistNow()
                onClose()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close editor")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    if (controller.isSaving) "Saving…" else "Autosaved · 1280×720",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { controller.undo() }, enabled = controller.canUndo) {
                Text("↶", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = { controller.redo() }, enabled = controller.canRedo) {
                Text("↷", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Rename project") }, onClick = {
                    showMenu = false; showRename = true
                })
                DropdownMenuItem(text = { Text("Duplicate design") }, onClick = {
                    showMenu = false
                    val copy = project.copy(
                        id = com.thumbnailstudio.app.model.newId(),
                        name = project.name + " copy"
                    )
                    store.save(copy)
                    store.saveThumbnail(copy)
                })
                DropdownMenuItem(text = { Text("Save as template") }, onClick = {
                    showMenu = false
                    store.saveAsTemplate(project, project.name)
                })
                DropdownMenuItem(text = { Text("Export thumbnail") }, onClick = {
                    showMenu = false; showExport = true
                })
            }
        }

        // ------------------------------------------------------------ canvas
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            EditorCanvas(
                project = project,
                selectedId = controller.selectedId,
                showGrid = settings.showGrid,
                showSafeArea = settings.showSafeArea,
                snap = settings.snapToCenter,
                onSelect = { controller.select(it) },
                onLayerChange = { controller.updateLayer(it, recordHistory = false) },
                onGestureStart = { controller.beginEdit() },
                onGestureEnd = { controller.endEdit() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ------------------------------------------------------------ panel
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                EditorTabRow(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 268.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    when (tab) {
                        EditorTab.ADD -> AddPanel(
                            onAddText = { controller.addText() },
                            onAddShape = { controller.addShape(it) },
                            onAddSticker = { controller.addSticker(it) },
                            onPickImage = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        EditorTab.TEXT -> {
                            val layer = selected as? TextLayer
                            if (layer == null) {
                                AddPanel(
                                    onAddText = { controller.addText() },
                                    onAddShape = { controller.addShape(it) },
                                    onAddSticker = { controller.addSticker(it) },
                                    onPickImage = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            } else {
                                TextProperties(
                                    layer = layer,
                                    onUpdate = { controller.updateLayer(it) },
                                    onEditText = { showTextEditor = true },
                                    onPickFont = { fontPicker.launch("*/*") }
                                )
                            }
                        }

                        EditorTab.IMAGE -> {
                            val layer = selected as? com.thumbnailstudio.app.model.ImageLayer
                            if (layer == null) {
                                ImagePlaceholderPanel(
                                    onPick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onPickBackground = {
                                        bgPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            } else {
                                ImageProperties(
                                    layer = layer,
                                    onUpdate = { controller.updateLayer(it) },
                                    onReplace = {
                                        replacePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }

                        EditorTab.SHAPE -> {
                            val layer = selected as? com.thumbnailstudio.app.model.ShapeLayer
                            if (layer == null) {
                                ShapePicker(onPick = { controller.addShape(it) })
                            } else {
                                ShapeProperties(layer = layer, onUpdate = { controller.updateLayer(it) })
                            }
                        }

                        EditorTab.STICKER -> {
                            val layer = selected as? com.thumbnailstudio.app.model.StickerLayer
                            if (layer == null) {
                                StickerPicker(onPick = { controller.addSticker(it) })
                            } else {
                                StickerProperties(layer = layer, onUpdate = { controller.updateLayer(it) })
                            }
                        }

                        EditorTab.BG -> BackgroundPanel(
                            background = project.background,
                            onChange = { controller.updateBackground(it) },
                            onPickImage = {
                                bgPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        EditorTab.LAYERS -> LayersPanel(
                            project = project,
                            selectedId = controller.selectedId,
                            onSelect = { controller.select(it) },
                            onToggleVisibility = { controller.toggleVisibility(it) },
                            onToggleLock = { controller.toggleLock(it) },
                            onMoveUp = { id ->
                                val idx = project.indexOf(id)
                                controller.reorder(idx, idx + 1)
                            },
                            onMoveDown = { id ->
                                val idx = project.indexOf(id)
                                controller.reorder(idx, idx - 1)
                            },
                            onDelete = { controller.deleteSelected() },
                            onDuplicate = { controller.duplicateSelected() }
                        )

                        EditorTab.TOOLS -> ToolsPanel(
                            project = project,
                            onApplyProject = { controller.replaceProject(it) },
                            onOpenExport = { showExport = true }
                        )
                    }
                }
            }
        }
    }

    if (showTextEditor) {
        val layer = selected as? TextLayer
        if (layer != null) {
            TextEditDialog(
                initial = layer.text,
                onDismiss = { showTextEditor = false },
                onConfirm = {
                    controller.updateLayer(layer.copy(text = it))
                    showTextEditor = false
                }
            )
        } else {
            showTextEditor = false
        }
    }

    if (showRename) {
        RenameDialog(
            initial = project.name,
            onDismiss = { showRename = false },
            onConfirm = {
                controller.rename(it)
                showRename = false
            }
        )
    }

    if (showExport) {
        ExportDialog(
            project = project,
            defaultFormat = if (settings.exportFormat == "JPEG") ExportFormat.JPEG else ExportFormat.PNG,
            defaultQuality = settings.jpegQuality,
            state = controller.exportState,
            onExport = { format, quality, name ->
                controller.export(context, format, quality, name)
            },
            onShare = { result, format ->
                runCatching {
                    context.startActivity(
                        Intent.createChooser(Exporter.shareIntent(context, result, format), "Share thumbnail")
                    )
                }
                onExported()
            },
            onDismiss = {
                showExport = false
                controller.resetExportState()
            }
        )
    }
}

@Composable
private fun EditorTabRow(selected: EditorTab, onSelect: (EditorTab) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(EditorTab.entries.toList()) { entry ->
            val isSelected = entry == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) Violet else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSelect(entry) }
            ) {
                Text(
                    entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Interactive canvas: pinch-zoom, pan, drag, resize and rotate with hit-testing
 * against the topmost layer under the finger.
 */
@Composable
fun EditorCanvas(
    project: Project,
    selectedId: String?,
    showGrid: Boolean,
    showSafeArea: Boolean,
    snap: Boolean,
    onSelect: (String?) -> Unit,
    onLayerChange: (Layer) -> Unit,
    onGestureStart: () -> Unit,
    onGestureEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val currentProject by rememberUpdatedState(project)
    val currentSelected by rememberUpdatedState(selectedId)
    val currentSnap by rememberUpdatedState(snap)

    BoxWithConstraints(modifier) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val fitScale = minOf(viewW / project.width, viewH / project.height) * 0.98f

        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(project.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val scale = fitScale * zoom
                        val originX = (size.width - currentProject.width * scale) / 2f + pan.x
                        val originY = (size.height - currentProject.height * scale) / 2f + pan.y
                        fun toProject(p: Offset) = Offset((p.x - originX) / scale, (p.y - originY) / scale)
                        fun toScreen(p: Offset) = Offset(originX + p.x * scale, originY + p.y * scale)

                        val proj = currentProject
                        val selLayer = currentSelected?.let { proj.layer(it) }

                        fun handlePoints(layer: Layer): Triple<Offset, Offset, Offset> {
                            val rad = Math.toRadians(layer.rotation.toDouble())
                            val c = cos(rad).toFloat()
                            val s = sin(rad).toFloat()
                            fun corner(dx: Float, dy: Float): Offset {
                                val lx = dx * layer.width / 2f
                                val ly = dy * layer.height / 2f
                                return toScreen(Offset(layer.x + lx * c - ly * s, layer.y + lx * s + ly * c))
                            }
                            val bottomRight = corner(1f, 1f)
                            val topRight = corner(1f, -1f)
                            val rotateDir = Offset(topRight.x - toScreen(Offset(layer.x, layer.y)).x,
                                topRight.y - toScreen(Offset(layer.x, layer.y)).y)
                            val len = hypot(rotateDir.x.toDouble(), rotateDir.y.toDouble()).toFloat().coerceAtLeast(1f)
                            val rotate = Offset(
                                topRight.x + rotateDir.x / len * 46f,
                                topRight.y + rotateDir.y / len * 46f
                            )
                            return Triple(bottomRight, topRight, rotate)
                        }

                        var mode = "canvas"
                        var startLayer: Layer? = null
                        val startPoint = toProject(down.position)
                        var lastPanPoint = down.position

                        if (selLayer != null && !selLayer.locked) {
                            val (br, _, rot) = handlePoints(selLayer)
                            if ((down.position - rot).getDistance() < 40f) {
                                mode = "rotate"; startLayer = selLayer
                            } else if ((down.position - br).getDistance() < 40f) {
                                mode = "scale"; startLayer = selLayer
                            }
                        }

                        if (mode == "canvas") {
                            val hit = proj.layers.lastOrNull { layer ->
                                if (!layer.visible || layer.locked) return@lastOrNull false
                                val rad = Math.toRadians(-layer.rotation.toDouble())
                                val c = cos(rad).toFloat()
                                val s = sin(rad).toFloat()
                                val dx = startPoint.x - layer.x
                                val dy = startPoint.y - layer.y
                                val lx = dx * c - dy * s
                                val ly = dx * s + dy * c
                                abs(lx) <= layer.width / 2f && abs(ly) <= layer.height / 2f
                            }
                            if (hit != null) {
                                onSelect(hit.id)
                                mode = "move"
                                startLayer = hit
                            } else {
                                onSelect(null)
                            }
                        }

                        if (mode != "canvas") onGestureStart()

                        var active = true
                        while (active) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed == 0) break
                            if (event.changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                zoom = (zoom * zoomChange).coerceIn(0.6f, 8f)
                                pan += panChange
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            val p = toProject(change.position)
                            when (mode) {
                                "move" -> {
                                    val base = startLayer ?: break
                                    var nx = base.x + (p.x - startPoint.x)
                                    var ny = base.y + (p.y - startPoint.y)
                                    if (currentSnap) {
                                        val centreX = currentProject.width / 2f
                                        val centreY = currentProject.height / 2f
                                        if (abs(nx - centreX) < 14f) nx = centreX
                                        if (abs(ny - centreY) < 14f) ny = centreY
                                    }
                                    onLayerChange(LayerOps.translate(base, nx - base.x, ny - base.y))
                                }
                                "scale" -> {
                                    val base = startLayer ?: break
                                    val centre = Offset(base.x, base.y)
                                    val d0 = (startPoint - centre).getDistance().coerceAtLeast(1f)
                                    val d1 = (p - centre).getDistance()
                                    val ratio = (d1 / d0).coerceIn(0.15f, 6f)
                                    onLayerChange(LayerOps.scaleTo(base, base.width * ratio, base.height * ratio))
                                }
                                "rotate" -> {
                                    val base = startLayer ?: break
                                    val centre = Offset(base.x, base.y)
                                    val a0 = atan2(startPoint.y - centre.y, startPoint.x - centre.x)
                                    val a1 = atan2(p.y - centre.y, p.x - centre.x)
                                    var deg = Math.toDegrees((a1 - a0).toDouble()).toFloat()
                                    if (currentSnap) {
                                        val snapped = Math.round((base.rotation + deg) / 15f) * 15f
                                        if (abs(snapped - (base.rotation + deg)) < 4f) deg = snapped - base.rotation
                                    }
                                    onLayerChange(LayerOps.rotate(base, base.rotation + deg))
                                }
                                else -> {
                                    pan += change.position - lastPanPoint
                                    lastPanPoint = change.position
                                }
                            }
                            change.consume()
                        }
                        if (mode != "canvas") onGestureEnd()
                    }
                }
        ) {
            val scale = fitScale * zoom
            val originX = (size.width - project.width * scale) / 2f + pan.x
            val originY = (size.height - project.height * scale) / 2f + pan.y

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.translate(originX, originY)
                native.scale(scale, scale)
                ThumbnailRenderer.render(native, project)
                native.restore()
            }

            // canvas frame
            drawRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(originX, originY),
                size = Size(project.width * scale, project.height * scale),
                style = Stroke(width = 1.5f)
            )

            if (showGrid) {
                val step = project.width / 8f
                for (i in 1 until 8) {
                    val x = originX + i * step * scale
                    drawLine(Color.White.copy(alpha = 0.10f), Offset(x, originY), Offset(x, originY + project.height * scale), 1f)
                }
                for (i in 1 until 4) {
                    val y = originY + i * (project.height / 4f) * scale
                    drawLine(Color.White.copy(alpha = 0.10f), Offset(originX, y), Offset(originX + project.width * scale, y), 1f)
                }
            }

            if (showSafeArea) {
                val inset = 0.06f
                drawRect(
                    color = Color(0x6622D3EE),
                    topLeft = Offset(originX + project.width * inset * scale, originY + project.height * inset * scale),
                    size = Size(project.width * (1 - 2 * inset) * scale, project.height * (1 - 2 * inset) * scale),
                    style = Stroke(width = 1.2f)
                )
            }

            val sel = selectedId?.let { project.layer(it) }
            if (sel != null) {
                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    val save = nc.save()
                    nc.translate(originX, originY)
                    nc.scale(scale, scale)
                    nc.rotate(sel.rotation, sel.x, sel.y)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 2.5f / scale * 1.8f
                    paint.color = 0xFF6D5BFF.toInt()
                    nc.drawRect(
                        sel.x - sel.width / 2f, sel.y - sel.height / 2f,
                        sel.x + sel.width / 2f, sel.y + sel.height / 2f, paint
                    )
                    paint.style = android.graphics.Paint.Style.FILL
                    val hr = 9f / scale
                    val cornerX = sel.x + sel.width / 2f
                    val cornerY = sel.y + sel.height / 2f
                    paint.color = 0xFFFFFFFF.toInt()
                    nc.drawCircle(cornerX, cornerY, hr, paint)
                    nc.drawCircle(cornerX, sel.y - sel.height / 2f, hr, paint)
                    paint.color = 0xFF6D5BFF.toInt()
                    nc.drawCircle(cornerX, cornerY, hr * 0.55f, paint)
                    paint.color = 0xFF22D3EE.toInt()
                    nc.drawCircle(cornerX, sel.y - sel.height / 2f, hr * 0.55f, paint)
                    val rotY = sel.y - sel.height / 2f
                    paint.color = 0xFFFF4D6D.toInt()
                    nc.drawCircle(cornerX, rotY - 46f / scale, hr, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 2f / scale
                    paint.color = 0x99FFFFFF.toInt()
                    nc.drawLine(cornerX, rotY, cornerX, rotY - 46f / scale, paint)
                    nc.restoreToCount(save)
                }
            }
        }
    }
}

@Composable
fun TextEditDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit text") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Thumbnail text") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project name") },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.ifBlank { "Untitled thumbnail" }) }) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Small helper used by the tools panel to draw a feed-size preview. */
@Composable
fun FeedPreview(project: Project, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), modifier = modifier.size(width = 168.dp, height = 94.dp)) {
        ProjectThumbnail(project, Modifier.fillMaxSize(), widthPx = 340)
    }
}
