package com.thumbnailstudio.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thumbnailstudio.app.export.ExportFormat
import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.ImageLayer
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.ShapeKind
import com.thumbnailstudio.app.model.ShapeLayer
import com.thumbnailstudio.app.model.StickerLayer
import com.thumbnailstudio.app.model.TextAlignH
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.render.Cutout
import com.thumbnailstudio.app.render.ImagePipeline
import com.thumbnailstudio.app.tools.SmartTools
import com.thumbnailstudio.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------------------------------------------------------------------
// Add panel
// ---------------------------------------------------------------------------

@Composable
fun AddPanel(
    onAddText: () -> Unit,
    onAddShape: (ShapeKind) -> Unit,
    onAddSticker: (String) -> Unit,
    onPickImage: () -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigAction("\uD83D\uDD24", "Text", Modifier.weight(1f), onAddText)
            BigAction("\uD83D\uDDBC\uFE0F", "Photo", Modifier.weight(1f), onPickImage)
            BigAction("\u25B2", "Shape", Modifier.weight(1f)) { onAddShape(ShapeKind.ROUND_RECT) }
            BigAction("\uD83D\uDE00", "Sticker", Modifier.weight(1f)) { onAddSticker("\uD83D\uDD25") }
        }
        Spacer(Modifier.height(14.dp))
        SectionLabel("Shapes")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ShapeKind.entries.toList()) { kind ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp).clickable { onAddShape(kind) }
                ) {
                    Box(Modifier.padding(10.dp)) {
                        ShapeGlyph(kind)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionLabel("Quick stickers")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(stickerSet) { glyph ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp).clickable { onAddSticker(glyph) }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(glyph, fontSize = 24.sp) }
                }
            }
        }
    }
}

@Composable
fun BigAction(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ShapeGlyph(kind: ShapeKind) {
    Canvas(Modifier.fillMaxSize()) {
        val path = com.thumbnailstudio.app.render.ShapePaths.build(kind, size.width, size.height, 8f)
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = android.graphics.Color.WHITE
            paint.alpha = 220
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}

val stickerSet = listOf(
    "\uD83D\uDD25", "\u26A1", "\uD83D\uDE31", "\uD83D\uDC40", "\u2B50", "\uD83D\uDCB0",
    "\uD83C\uDFAE", "\uD83C\uDFB5", "\uD83C\uDFA7", "\uD83D\uDCF1", "\uD83D\uDCE6", "\uD83C\uDFAF",
    "\u2705", "\u274C", "\u2757", "\uD83D\uDCA5", "\uD83C\uDF89", "\uD83E\uDD16",
    "\uD83D\uDCF8", "\uD83C\uDF10", "\uD83D\uDD14", "\uD83D\uDCAC", "\uD83D\uDCC8", "\uD83C\uDF1F"
)

@Composable
fun ShapePicker(onPick: (ShapeKind) -> Unit) {
    Column {
        SectionLabel("Add a shape")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ShapeKind.entries.toList()) { kind ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(58.dp).clickable { onPick(kind) }
                ) {
                    Box(Modifier.padding(10.dp)) { ShapeGlyph(kind) }
                }
            }
        }
    }
}

@Composable
fun StickerPicker(onPick: (String) -> Unit) {
    Column {
        SectionLabel("Add a sticker")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(stickerSet) { glyph ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(54.dp).clickable { onPick(glyph) }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(glyph, fontSize = 24.sp) }
                }
            }
        }
    }
}

@Composable
fun ImagePlaceholderPanel(onPick: () -> Unit, onPickBackground: () -> Unit) {
    Column {
        Text("No photo selected", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add a photo from your gallery. Photos stay on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPick) { Text("Add photo") }
            OutlinedButton(onClick = onPickBackground) { Text("Use as backdrop") }
        }
    }
}

// ---------------------------------------------------------------------------
// Text properties
// ---------------------------------------------------------------------------

data class TextPreset(val name: String, val apply: (TextLayer) -> TextLayer)

val textPresets = listOf(
    TextPreset("Gaming") {
        it.copy(
            fontKey = "sans_black", bold = true, italic = true, uppercase = true,
            color = 0xFFFFFFFF.toInt(), strokeEnabled = true, strokeWidth = 12f,
            strokeColor = 0xFF000000.toInt(), shadowEnabled = true, shadowBlur = 22f,
            glowEnabled = false, gradient = false, depth3d = 0f
        )
    },
    TextPreset("Music") {
        it.copy(
            fontKey = "sans_black", bold = true, italic = false, uppercase = true,
            color = 0xFFFFFFFF.toInt(), gradient = true, gradientColor = 0xFFF9A8D4.toInt(),
            strokeEnabled = false, glowEnabled = true, glowColor = 0xFFC084FC.toInt(),
            glowRadius = 26f, curve = 26f, shadowEnabled = true
        )
    },
    TextPreset("Tech") {
        it.copy(
            fontKey = "mono", bold = true, uppercase = true, letterSpacing = 6f,
            color = 0xFF22D3EE.toInt(), gradient = false, strokeEnabled = false,
            glowEnabled = true, glowColor = 0xFF22D3EE.toInt(), glowRadius = 18f,
            shadowEnabled = false, boxEnabled = false
        )
    },
    TextPreset("Education") {
        it.copy(
            fontKey = "sans_bold", bold = true, uppercase = false, color = 0xFF0F172A.toInt(),
            strokeEnabled = false, shadowEnabled = false, boxEnabled = true,
            boxColor = 0xFFFACC15.toInt(), boxPadding = 28f, boxCorner = 16f
        )
    },
    TextPreset("Vlog") {
        it.copy(
            fontKey = "sans_bold", bold = true, uppercase = false, color = 0xFFFFFFFF.toInt(),
            strokeEnabled = true, strokeWidth = 8f, strokeColor = 0xFF7C2D12.toInt(),
            shadowEnabled = true, boxEnabled = false, rotation = -3f
        )
    },
    TextPreset("News") {
        it.copy(
            fontKey = "condensed", bold = true, uppercase = true, color = 0xFFFFFFFF.toInt(),
            strokeEnabled = false, shadowEnabled = true, shadowBlur = 8f,
            boxEnabled = true, boxColor = 0xFFDC2626.toInt(), boxPadding = 22f, boxCorner = 6f
        )
    },
    TextPreset("Cinematic") {
        it.copy(
            fontKey = "serif_bold", bold = true, uppercase = true, letterSpacing = 10f,
            color = 0xFFF5F5F4.toInt(), strokeEnabled = false, shadowEnabled = true,
            shadowBlur = 26f, gradient = false, glowEnabled = false
        )
    },
    TextPreset("3D Pop") {
        it.copy(
            fontKey = "sans_black", bold = true, uppercase = true, color = 0xFFFFD166.toInt(),
            depth3d = 16f, strokeEnabled = true, strokeWidth = 10f,
            strokeColor = 0xFF1F2937.toInt(), shadowEnabled = true, shadowBlur = 18f
        )
    }
)

@Composable
fun TextProperties(
    layer: TextLayer,
    onUpdate: (TextLayer) -> Unit,
    onEditText: () -> Unit,
    onPickFont: () -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onEditText, modifier = Modifier.weight(1f)) {
                Text(layer.text.lineSequence().firstOrNull()?.take(22) ?: "Edit text", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onPickFont) { Text("Font file") }
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("Style presets")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(textPresets) { preset ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onUpdate(preset.apply(layer)) }
                ) {
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel("Font")
        ChipRow(
            options = com.thumbnailstudio.app.render.FontCatalog.builtIn.map { it.label },
            selected = com.thumbnailstudio.app.render.FontCatalog.builtIn
                .firstOrNull { it.key == layer.fontKey }?.label ?: "Impact",
            onSelect = { label ->
                val option = com.thumbnailstudio.app.render.FontCatalog.builtIn.firstOrNull { it.label == label }
                if (option != null) onUpdate(layer.copy(fontKey = option.key, customFontPath = null))
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StyleToggle("B", layer.bold) { onUpdate(layer.copy(bold = it)) }
            StyleToggle("I", layer.italic) { onUpdate(layer.copy(italic = it)) }
            StyleToggle("U", layer.underline) { onUpdate(layer.copy(underline = it)) }
            StyleToggle("AA", layer.uppercase) { onUpdate(layer.copy(uppercase = it)) }
            Spacer(Modifier.weight(1f))
            ChipRow(
                options = listOf("Left", "Center", "Right"),
                selected = when (layer.align) {
                    TextAlignH.LEFT -> "Left"
                    TextAlignH.CENTER -> "Center"
                    TextAlignH.RIGHT -> "Right"
                },
                onSelect = {
                    onUpdate(
                        layer.copy(
                            align = when (it) {
                                "Left" -> TextAlignH.LEFT
                                "Right" -> TextAlignH.RIGHT
                                else -> TextAlignH.CENTER
                            }
                        )
                    )
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        SliderRow("Size", layer.fontSize, 12f..300f, { onUpdate(layer.copy(fontSize = it)) })
        SliderRow("Letter spacing", layer.letterSpacing, -5f..30f, { onUpdate(layer.copy(letterSpacing = it)) })
        SliderRow("Line spacing", layer.lineSpacing, 0.7f..2f, { onUpdate(layer.copy(lineSpacing = it)) }, display = { String.format("%.2f", it) })
        SliderRow("Rotation", layer.rotation, -180f..180f, { onUpdate(layer.copy(rotation = it)) })
        SliderRow("Curve", layer.curve, -100f..100f, { onUpdate(layer.copy(curve = it)) })
        SliderRow("3D depth", layer.depth3d, 0f..30f, { onUpdate(layer.copy(depth3d = it)) })

        Spacer(Modifier.height(6.dp))
        SectionLabel("Colour")
        ColorField("Text colour", layer.color) { onUpdate(layer.copy(color = it)) }
        ToggleRow("Gradient fill", layer.gradient) { onUpdate(layer.copy(gradient = it)) }
        if (layer.gradient) {
            ColorField("Gradient end", layer.gradientColor) { onUpdate(layer.copy(gradientColor = it)) }
        }

        Spacer(Modifier.height(6.dp))
        SectionLabel("Outline & effects")
        ToggleRow("Outline", layer.strokeEnabled) { onUpdate(layer.copy(strokeEnabled = it)) }
        if (layer.strokeEnabled) {
            ColorField("Outline colour", layer.strokeColor) { onUpdate(layer.copy(strokeColor = it)) }
            SliderRow("Outline width", layer.strokeWidth, 0f..30f, { onUpdate(layer.copy(strokeWidth = it)) })
        }
        ToggleRow("Drop shadow", layer.shadowEnabled) { onUpdate(layer.copy(shadowEnabled = it)) }
        if (layer.shadowEnabled) {
            ColorField("Shadow colour", layer.shadowColor) { onUpdate(layer.copy(shadowColor = it)) }
            SliderRow("Shadow blur", layer.shadowBlur, 0f..60f, { onUpdate(layer.copy(shadowBlur = it)) })
            SliderRow("Shadow offset", layer.shadowDy, -30f..30f, { onUpdate(layer.copy(shadowDy = it, shadowDx = it / 2f)) })
        }
        ToggleRow("Glow", layer.glowEnabled) { onUpdate(layer.copy(glowEnabled = it)) }
        if (layer.glowEnabled) {
            ColorField("Glow colour", layer.glowColor) { onUpdate(layer.copy(glowColor = it)) }
            SliderRow("Glow radius", layer.glowRadius, 2f..60f, { onUpdate(layer.copy(glowRadius = it)) })
        }
        ToggleRow("Background box", layer.boxEnabled) { onUpdate(layer.copy(boxEnabled = it)) }
        if (layer.boxEnabled) {
            ColorField("Box colour", layer.boxColor) { onUpdate(layer.copy(boxColor = it)) }
            SliderRow("Box padding", layer.boxPadding, 0f..60f, { onUpdate(layer.copy(boxPadding = it)) })
            SliderRow("Box corner", layer.boxCorner, 0f..60f, { onUpdate(layer.copy(boxCorner = it)) })
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Tip: open the Smart tab for automatic contrast checks against your actual backdrop.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StyleToggle(label: String, active: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) Violet else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp).clickable { onChange(!active) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Image properties
// ---------------------------------------------------------------------------

@Composable
fun ImageProperties(
    layer: ImageLayer,
    onUpdate: (ImageLayer) -> Unit,
    onReplace: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tolerance by remember { mutableFloatStateOf(0.25f) }
    var showErase by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplace, modifier = Modifier.weight(1f)) { Text("Replace photo") }
            OutlinedButton(onClick = { showErase = true }) { Text("Erase") }
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("Remove background colour (traditional cutout)")
        Text(
            "Flood-fills similar pixels from the edges, like a magic wand. This is not AI segmentation.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SliderRow("Tolerance", tolerance, 0.05f..0.9f, { tolerance = it }, display = { "${(it * 100).toInt()}%" })
        OutlinedButton(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    val outDir = File(context.filesDir, "images").apply { mkdirs() }
                    val out = File(outDir, "cut_${System.currentTimeMillis()}.png")
                    val ok = withContext(Dispatchers.IO) {
                        Cutout.removeColorBackground(layer.path, tolerance, out)
                    }
                    busy = false
                    if (ok) onUpdate(layer.copy(path = out.absolutePath))
                }
            }
        ) { Text(if (busy) "Working…" else "Cut out background colour") }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Adjustments")
        SliderRow("Brightness", layer.brightness, -0.6f..0.6f, { onUpdate(layer.copy(brightness = it)) }, display = { "${(it * 100).toInt()}" })
        SliderRow("Contrast", layer.contrast, -0.6f..0.8f, { onUpdate(layer.copy(contrast = it)) }, display = { "${(it * 100).toInt()}" })
        SliderRow("Saturation", layer.saturation, -1f..1f, { onUpdate(layer.copy(saturation = it)) }, display = { "${(it * 100).toInt()}" })
        SliderRow("Hue", layer.hue, -180f..180f, { onUpdate(layer.copy(hue = it)) })
        SliderRow("Sharpness", layer.sharpen, 0f..1f, { onUpdate(layer.copy(sharpen = it)) }, display = { "${(it * 100).toInt()}%" })

        Spacer(Modifier.height(8.dp))
        SectionLabel("Shape")
        SliderRow("Corner radius", layer.cornerRadius, 0f..160f, { onUpdate(layer.copy(cornerRadius = it)) })
        ToggleRow("Flip horizontally", layer.flipH) { onUpdate(layer.copy(flipH = it)) }
        ToggleRow("Border", layer.strokeWidth > 0.2f) {
            onUpdate(layer.copy(strokeWidth = if (it) 6f else 0f, strokeColor = 0xFFFFFFFF.toInt()))
        }
        if (layer.strokeWidth > 0.2f) {
            ColorField("Border colour", layer.strokeColor) { onUpdate(layer.copy(strokeColor = it)) }
            SliderRow("Border width", layer.strokeWidth, 1f..24f, { onUpdate(layer.copy(strokeWidth = it)) })
        }
    }

    if (showErase) {
        EraseDialog(
            path = layer.path,
            onDismiss = { showErase = false },
            onDone = { newPath ->
                onUpdate(layer.copy(path = newPath))
                showErase = false
            }
        )
    }
}

@Composable
fun EraseDialog(path: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brush by remember { mutableFloatStateOf(28f) }
    var dirty by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) { ImagePipeline.decode(path, 1000)?.copy(Bitmap.Config.ARGB_8888, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erase brush") },
        text = {
            Column {
                Text(
                    "Drag over the photo to erase. Manual masking — nothing is uploaded.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val bmp = bitmap
                if (bmp == null) {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(bmp.width.toFloat() / bmp.height)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2A2F3A), Color(0xFF1A1F29)))
                            )
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Canvas(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(bmp) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val px = offset.x / size.width * bmp.width
                                            val py = offset.y / size.height * bmp.height
                                            Cutout.eraseStroke(bmp, px, py, brush)
                                            dirty = true
                                        },
                                        onDrag = { change, _ ->
                                            val px = change.position.x / size.width * bmp.width
                                            val py = change.position.y / size.height * bmp.height
                                            Cutout.eraseStroke(bmp, px, py, brush)
                                            dirty = true
                                            change.consume()
                                        }
                                    )
                                }
                        ) { }
                    }
                    Spacer(Modifier.height(8.dp))
                    SliderRow("Brush size", brush, 8f..120f, { brush = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = bitmap != null && dirty && !saved,
                onClick = {
                    val bmp = bitmap ?: return@TextButton
                    saved = true
                    scope.launch {
                        val outDir = File(context.filesDir, "images").apply { mkdirs() }
                        val out = File(outDir, "erased_${System.currentTimeMillis()}.png")
                        val ok = withContext(Dispatchers.IO) { Cutout.saveBitmap(bmp, out) }
                        if (ok) onDone(out.absolutePath)
                    }
                }
            ) { Text("Save cutout") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// Shape / sticker properties
// ---------------------------------------------------------------------------

@Composable
fun ShapeProperties(layer: ShapeLayer, onUpdate: (ShapeLayer) -> Unit) {
    Column {
        SectionLabel("Shape")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShapeKind.entries.toList()) { kind ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (kind == layer.kind) Violet else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(50.dp).clickable { onUpdate(layer.copy(kind = kind)) }
                ) {
                    Box(Modifier.padding(9.dp)) { ShapeGlyph(kind) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ColorField("Fill", layer.fill) { onUpdate(layer.copy(fill = it)) }
        ToggleRow("Gradient fill", layer.gradient) { onUpdate(layer.copy(gradient = it)) }
        if (layer.gradient) {
            ColorField("Gradient end", layer.fill2) { onUpdate(layer.copy(fill2 = it)) }
        }
        SliderRow("Corner radius", layer.cornerRadius, 0f..120f, { onUpdate(layer.copy(cornerRadius = it)) })
        ToggleRow("Border", layer.strokeWidth > 0.2f) {
            onUpdate(layer.copy(strokeWidth = if (it) 6f else 0f, strokeColor = 0xFFFFFFFF.toInt()))
        }
        if (layer.strokeWidth > 0.2f) {
            ColorField("Border colour", layer.strokeColor) { onUpdate(layer.copy(strokeColor = it)) }
            SliderRow("Border width", layer.strokeWidth, 1f..24f, { onUpdate(layer.copy(strokeWidth = it)) })
        }
    }
}

@Composable
fun StickerProperties(layer: StickerLayer, onUpdate: (StickerLayer) -> Unit) {
    Column {
        SectionLabel("Sticker")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stickerSet) { glyph ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (glyph == layer.glyph) Violet else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(50.dp).clickable { onUpdate(layer.copy(glyph = glyph)) }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(glyph, fontSize = 22.sp) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SliderRow("Size", layer.width, 40f..600f, { onUpdate(layer.copy(width = it, height = it)) })
        SliderRow("Rotation", layer.rotation, -180f..180f, { onUpdate(layer.copy(rotation = it)) })
    }
}

// ---------------------------------------------------------------------------
// Background panel
// ---------------------------------------------------------------------------

val gradientPresets = listOf(
    Triple(0xFF0F172A.toInt(), 0xFF312E81.toInt(), "Midnight"),
    Triple(0xFF7F1D1D.toInt(), 0xFF1F2937.toInt(), "Ember"),
    Triple(0xFF3B0764.toInt(), 0xFFBE185D.toInt(), "Neon"),
    Triple(0xFF052E2B.toInt(), 0xFF0F766E.toInt(), "Teal"),
    Triple(0xFFF97316.toInt(), 0xFF7C2D12.toInt(), "Sunset"),
    Triple(0xFF111827.toInt(), 0xFF4B5563.toInt(), "Graphite"),
    Triple(0xFF1E3A8A.toInt(), 0xFF0EA5E9.toInt(), "Ocean"),
    Triple(0xFF4C1D95.toInt(), 0xFF8B5CF6.toInt(), "Violet")
)

@Composable
fun BackgroundPanel(background: Background, onChange: (Background) -> Unit, onPickImage: () -> Unit) {
    Column {
        SectionLabel("Backdrop type")
        ChipRow(
            options = listOf("Solid", "Gradient", "Photo"),
            selected = when (background.type) {
                BgType.SOLID -> "Solid"
                BgType.GRADIENT -> "Gradient"
                BgType.IMAGE -> "Photo"
            },
            onSelect = {
                onChange(
                    background.copy(
                        type = when (it) {
                            "Solid" -> BgType.SOLID
                            "Gradient" -> BgType.GRADIENT
                            else -> BgType.IMAGE
                        }
                    )
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        when (background.type) {
            BgType.SOLID -> ColorField("Colour", background.color1) { onChange(background.copy(color1 = it)) }
            BgType.GRADIENT -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(gradientPresets) { preset ->
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(preset.first), Color(preset.second))))
                                .clickable {
                                    onChange(background.copy(color1 = preset.first, color2 = preset.second, type = BgType.GRADIENT))
                                }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                ColorField("Colour A", background.color1) { onChange(background.copy(color1 = it)) }
                ColorField("Colour B", background.color2) { onChange(background.copy(color2 = it)) }
                SliderRow("Angle", background.angle, 0f..360f, { onChange(background.copy(angle = it)) })
            }
            BgType.IMAGE -> {
                OutlinedButton(onClick = onPickImage) { Text("Choose photo") }
                SliderRow("Blur", background.blur, 0f..40f, { onChange(background.copy(blur = it)) })
            }
        }
        Spacer(Modifier.height(8.dp))
        SectionLabel("Finishing")
        SliderRow("Vignette", background.vignette, 0f..1f, { onChange(background.copy(vignette = it)) }, display = { "${(it * 100).toInt()}%" })
        SliderRow("Darken", background.darken, 0f..0.85f, { onChange(background.copy(darken = it)) }, display = { "${(it * 100).toInt()}%" })
        OutlinedButton(onClick = {
            val suggested = SmartTools.suggestedBackgroundFor(0xFFFFFFFF.toInt())
            onChange(background.copy(type = BgType.GRADIENT, color1 = suggested, color2 = SmartTools.shift(suggested, 40f)))
        }) { Text("Suggest a background") }
    }
}

// ---------------------------------------------------------------------------
// Layers panel
// ---------------------------------------------------------------------------

@Composable
fun LayersPanel(
    project: Project,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDuplicate, enabled = selectedId != null) { Text("Duplicate") }
            OutlinedButton(onClick = onDelete, enabled = selectedId != null) { Text("Delete") }
            Spacer(Modifier.weight(1f))
            Text(
                "${project.layers.size} layers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Spacer(Modifier.height(8.dp))
        project.layers.reversed().forEach { layer ->
            val isSelected = layer.id == selectedId
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Violet.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelect(layer.id) }
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (layer) {
                            is TextLayer -> "\uD83D\uDD24"
                            is ImageLayer -> "\uD83D\uDDBC\uFE0F"
                            is ShapeLayer -> "\u25B2"
                            is StickerLayer -> layer.glyph
                        },
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        layer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (layer.visible) "\uD83D\uDC41" else "\uD83D\uDE48",
                        modifier = Modifier.clickable { onToggleVisibility(layer.id) }.padding(6.dp)
                    )
                    Text(
                        if (layer.locked) "\uD83D\uDD12" else "\uD83D\uDD13",
                        modifier = Modifier.clickable { onToggleLock(layer.id) }.padding(6.dp)
                    )
                    Text("↑", modifier = Modifier.clickable { onMoveUp(layer.id) }.padding(6.dp))
                    Text("↓", modifier = Modifier.clickable { onMoveDown(layer.id) }.padding(6.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Smart tools
// ---------------------------------------------------------------------------

@Composable
fun ToolsPanel(
    project: Project,
    onApplyProject: (Project) -> Unit,
    onOpenExport: () -> Unit
) {
    val score = SmartTools.scoreThumbnail(project)
    val issues = SmartTools.contrastIssues(project)
    val checklist = SmartTools.checklist(project)
    val suggestions = SmartTools.layoutSuggestions(project)
    val palette = SmartTools.quantizePalette(SmartTools.backgroundColors(project) + listOf(0xFFFFFFFF.toInt()))

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Design check", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (issues.isEmpty()) "Contrast and layout look healthy" else "${issues.size} readability issue(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("$score/100", style = MaterialTheme.typography.headlineSmall, color = Violet)
        }
        Spacer(Modifier.height(6.dp))
        checklist.forEach { (label, ok) ->
            Text(
                (if (ok) "\u2714  " else "\u2716  ") + label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onApplyProject(SmartTools.autoFixContrast(project)) }) { Text("Fix contrast") }
            OutlinedButton(onClick = { onApplyProject(SmartTools.addReadabilityScrim(project)) }) { Text("Add scrim") }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Smart layouts")
        suggestions.forEach { suggestion ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onApplyProject(suggestion.apply(project)) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        suggestion.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Palette from this design")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(palette) { color ->
                ColorSwatch(color, selected = false, onClick = {
                    onApplyProject(
                        project.copy(
                            background = project.background.copy(
                                color1 = color,
                                color2 = SmartTools.shift(color, 35f),
                                type = BgType.GRADIENT
                            )
                        )
                    )
                })
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("YouTube feed preview")
        Row(verticalAlignment = Alignment.CenterVertically) {
            FeedPreview(project)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "This is how your thumbnail looks in the feed at small size.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenExport) { Text("Export") }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------------------
// Export dialog
// ---------------------------------------------------------------------------

@Composable
fun ExportDialog(
    project: Project,
    defaultFormat: ExportFormat,
    defaultQuality: Int,
    state: ExportUiState,
    onExport: (ExportFormat, Int, String) -> Unit,
    onShare: (com.thumbnailstudio.app.export.ExportResult, ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var format by remember { mutableStateOf(defaultFormat) }
    var quality by remember { mutableFloatStateOf(defaultQuality.toFloat()) }
    var fileName by remember { mutableStateOf(project.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export thumbnail") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.width(150.dp).aspectRatio(16f / 9f)) {
                        ProjectThumbnail(project, Modifier.fillMaxSize(), widthPx = 300)
                    }
                    Column {
                        Text("1280 × 720", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "16:9 · YouTube ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ChipRow(
                    options = ExportFormat.entries.map { it.label },
                    selected = format.label,
                    onSelect = { label -> format = ExportFormat.entries.first { it.label == label } }
                )
                if (format == ExportFormat.JPEG) {
                    SliderRow("Quality", quality, 60f..100f, { quality = it }, display = { "${it.toInt()}%" })
                }
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                when (state) {
                    is ExportUiState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Rendering…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ExportUiState.Done -> Column {
                        Text("Saved", style = MaterialTheme.typography.titleMedium, color = Violet)
                        Text(
                            state.result.displayPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${state.result.width} × ${state.result.height} · ${state.result.byteCount / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ExportUiState.Error -> Text(
                        "Export failed: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    ExportUiState.Idle -> Text(
                        "Saved to Pictures/Thumbnail Studio. You can share it straight to YouTube.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            when (val current = state) {
                is ExportUiState.Done -> Button(onClick = { onShare(current.result, current.format) }) { Text("Share") }
                is ExportUiState.Working -> TextButton(enabled = false, onClick = {}) { Text("Working…") }
                else -> Button(onClick = { onExport(format, quality.toInt(), fileName) }) { Text("Export") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
