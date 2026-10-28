package com.thumbnailstudio.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.render.ThumbnailRenderer
import com.thumbnailstudio.app.tools.SmartTools
import kotlin.math.roundToInt

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    corner: Int = 22,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(corner.dp)
    val base = Modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    scheme.surface.copy(alpha = 0.92f),
                    scheme.surfaceVariant.copy(alpha = 0.78f)
                )
            )
        )
        .border(1.dp, scheme.onSurface.copy(alpha = 0.08f), shape)
    Box(modifier = if (onClick != null) base.clickable(onClick = onClick) else base) {
        content()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onChangeFinished: (() -> Unit)? = null,
    display: ((Float) -> String)? = null
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                display?.invoke(value) ?: value.roundToInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            val isSelected = option == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSelect(option) }
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

val presetSwatches = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF6D5BFF, 0xFFFF4D6D, 0xFF22D3EE, 0xFFFFC857,
    0xFF34D399, 0xFFF97316, 0xFF8B5CF6, 0xFF0EA5E9, 0xFFE11D48, 0xFF84CC16,
    0xFF111827, 0xFF1F2937, 0xFF374151, 0xFF9CA3AF
).map { it.toInt() }

@Composable
fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
    size: Int = 34
) {
    val c = Color(color)
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(c)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ColorField(label: String, color: Int, onChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { showPicker = true }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                SmartTools.toHex(color),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
    if (showPicker) {
        ColorPickerDialog(initial = color, onDismiss = { showPicker = false }, onPick = {
            onChange(it)
            showPicker = false
        })
    }
}

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var hue by remember { mutableFloatStateOf(SmartTools.hueOf(initial)) }
    var sat by remember { mutableFloatStateOf(SmartTools.saturationOf(initial)) }
    var value by remember { mutableFloatStateOf(SmartTools.valueOf(initial)) }
    var alpha by remember { mutableFloatStateOf(SmartTools.alpha(initial) / 255f) }
    val current = SmartTools.argb(
        (alpha * 255).roundToInt(),
        SmartTools.red(SmartTools.fromHsv(hue, sat, value)),
        SmartTools.green(SmartTools.fromHsv(hue, sat, value)),
        SmartTools.blue(SmartTools.fromHsv(hue, sat, value))
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a colour") },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(current))
                )
                Spacer(Modifier.height(10.dp))
                SliderRow("Hue", hue, 0f..360f, { hue = it })
                SliderRow("Saturation", sat, 0f..1f, { sat = it }, display = { "${(it * 100).roundToInt()}%" })
                SliderRow("Brightness", value, 0f..1f, { value = it }, display = { "${(it * 100).roundToInt()}%" })
                SliderRow("Alpha", alpha, 0f..1f, { alpha = it }, display = { "${(it * 100).roundToInt()}%" })
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetSwatches) { swatch ->
                        ColorSwatch(swatch, selected = false, onClick = {
                            hue = SmartTools.hueOf(swatch)
                            sat = SmartTools.saturationOf(swatch)
                            value = SmartTools.valueOf(swatch)
                            alpha = 1f
                        })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("Use colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Renders a live preview of a project; the same renderer used for export. */
@Composable
fun ProjectThumbnail(
    project: Project,
    modifier: Modifier = Modifier,
    widthPx: Int = 420
) {
    val image = remember(project, widthPx) {
        val h = (widthPx * project.height / project.width.toFloat()).roundToInt().coerceAtLeast(1)
        ThumbnailRenderer.renderToBitmap(project, widthPx, h).asImageBitmap()
    }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun EmptyState(title: String, message: String, emoji: String = "✨", action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun TagPill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.18f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun readableOn(color: Int): Color =
    if (Color(color).luminance() > 0.5f) Color.Black else Color.White
