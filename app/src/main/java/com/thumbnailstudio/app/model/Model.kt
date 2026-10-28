package com.thumbnailstudio.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** Canonical export size for YouTube thumbnails. */
const val PROJECT_WIDTH = 1280
const val PROJECT_HEIGHT = 720

/** Safe area (title/duration overlay on YouTube) as a fraction of the canvas. */
const val SAFE_INSET_FRACTION = 0.06f

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(10)

@Serializable
enum class BgType { SOLID, GRADIENT, IMAGE }

@Serializable
data class Background(
    val type: BgType = BgType.GRADIENT,
    val color1: Int = 0xFF0F172A.toInt(),
    val color2: Int = 0xFF312E81.toInt(),
    val angle: Float = 135f,
    val imagePath: String? = null,
    val blur: Float = 0f,
    val vignette: Float = 0.35f,
    val darken: Float = 0f
)

@Serializable
enum class TextAlignH { LEFT, CENTER, RIGHT }

@Serializable
enum class ShapeKind { RECT, ROUND_RECT, CIRCLE, TRIANGLE, STAR, ARROW_RIGHT, LINE, FRAME, BADGE, SPEECH, BURST, HEART }

/**
 * A layer lives in a fixed 1280 x 720 coordinate space. [x] and [y] are the
 * centre of the layer, [width]/[height] its untransformed bounding box and
 * [rotation] is applied around that centre. Keeping every layer in canvas
 * coordinates makes export deterministic and unit-testable.
 */
@Serializable
sealed class Layer {
    abstract val id: String
    abstract val name: String
    abstract val x: Float
    abstract val y: Float
    abstract val width: Float
    abstract val height: Float
    abstract val rotation: Float
    abstract val opacity: Float
    abstract val locked: Boolean
    abstract val visible: Boolean

    val centerX: Float get() = x
    val centerY: Float get() = y
}

@Serializable
data class TextLayer(
    override val id: String = newId(),
    override val name: String = "Text",
    override val x: Float = PROJECT_WIDTH / 2f,
    override val y: Float = PROJECT_HEIGHT / 2f,
    override val width: Float = 760f,
    override val height: Float = 200f,
    override val rotation: Float = 0f,
    override val opacity: Float = 1f,
    override val locked: Boolean = false,
    override val visible: Boolean = true,
    val text: String = "YOUR TITLE",
    val fontKey: String = "sans_black",
    val customFontPath: String? = null,
    val fontSize: Float = 96f,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val uppercase: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1f,
    val align: TextAlignH = TextAlignH.CENTER,
    val color: Int = 0xFFFFFFFF.toInt(),
    val gradient: Boolean = false,
    val gradientColor: Int = 0xFFFFD166.toInt(),
    val gradientAngle: Float = 90f,
    val strokeEnabled: Boolean = false,
    val strokeColor: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 8f,
    val shadowEnabled: Boolean = true,
    val shadowColor: Int = 0xCC000000.toInt(),
    val shadowBlur: Float = 14f,
    val shadowDx: Float = 4f,
    val shadowDy: Float = 6f,
    val glowEnabled: Boolean = false,
    val glowColor: Int = 0xFF6D5BFF.toInt(),
    val glowRadius: Float = 22f,
    val depth3d: Float = 0f,
    val boxEnabled: Boolean = false,
    val boxColor: Int = 0xCC000000.toInt(),
    val boxPadding: Float = 26f,
    val boxCorner: Float = 20f,
    val curve: Float = 0f
) : Layer()

@Serializable
data class ImageLayer(
    override val id: String = newId(),
    override val name: String = "Image",
    override val x: Float = PROJECT_WIDTH / 2f,
    override val y: Float = PROJECT_HEIGHT / 2f,
    override val width: Float = 760f,
    override val height: Float = 428f,
    override val rotation: Float = 0f,
    override val opacity: Float = 1f,
    override val locked: Boolean = false,
    override val visible: Boolean = true,
    val path: String,
    val cornerRadius: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val hue: Float = 0f,
    val sharpen: Float = 0f,
    val flipH: Boolean = false,
    val strokeColor: Int = 0x00000000,
    val strokeWidth: Float = 0f
) : Layer()

@Serializable
data class ShapeLayer(
    override val id: String = newId(),
    override val name: String = "Shape",
    override val x: Float = PROJECT_WIDTH / 2f,
    override val y: Float = PROJECT_HEIGHT / 2f,
    override val width: Float = 420f,
    override val height: Float = 240f,
    override val rotation: Float = 0f,
    override val opacity: Float = 1f,
    override val locked: Boolean = false,
    override val visible: Boolean = true,
    val kind: ShapeKind = ShapeKind.ROUND_RECT,
    val fill: Int = 0xFFEF4444.toInt(),
    val gradient: Boolean = false,
    val fill2: Int = 0xFFF97316.toInt(),
    val strokeColor: Int = 0x00000000,
    val strokeWidth: Float = 0f,
    val cornerRadius: Float = 32f
) : Layer()

@Serializable
data class StickerLayer(
    override val id: String = newId(),
    override val name: String = "Sticker",
    override val x: Float = PROJECT_WIDTH / 2f,
    override val y: Float = PROJECT_HEIGHT / 2f,
    override val width: Float = 160f,
    override val height: Float = 160f,
    override val rotation: Float = 0f,
    override val opacity: Float = 1f,
    override val locked: Boolean = false,
    override val visible: Boolean = true,
    val glyph: String = "\uD83D\uDD25"
) : Layer()

@Serializable
data class Project(
    val id: String = newId(),
    val name: String = "Untitled thumbnail",
    val width: Int = PROJECT_WIDTH,
    val height: Int = PROJECT_HEIGHT,
    val background: Background = Background(),
    val layers: List<Layer> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = "Custom",
    val isTemplate: Boolean = false
) {
    fun layer(id: String): Layer? = layers.firstOrNull { it.id == id }

    fun indexOf(id: String): Int = layers.indexOfFirst { it.id == id }

    /** Replaces one layer, preserving z-order. */
    fun replace(layer: Layer): Project {
        val idx = indexOf(layer.id)
        if (idx < 0) return this
        val copy = layers.toMutableList()
        copy[idx] = layer
        return copy(layers = copy, updatedAt = System.currentTimeMillis())
    }

    fun add(layer: Layer): Project =
        copy(layers = layers + layer, updatedAt = System.currentTimeMillis())

    fun remove(id: String): Project =
        copy(layers = layers.filterNot { it.id == id }, updatedAt = System.currentTimeMillis())

    fun duplicate(id: String): Project {
        val idx = indexOf(id)
        if (idx < 0) return this
        val src = layers[idx]
        val copy = LayerOps.duplicated(src)
        val list = layers.toMutableList()
        list.add(idx + 1, copy)
        return copy(layers = list, updatedAt = System.currentTimeMillis())
    }

    fun move(from: Int, to: Int): Project {
        if (from !in layers.indices || to !in layers.indices || from == to) return this
        val list = layers.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return copy(layers = list, updatedAt = System.currentTimeMillis())
    }

    fun bringToFront(id: String): Project = move(indexOf(id), layers.lastIndex)

    fun sendToBack(id: String): Project = move(indexOf(id), 0)

    fun rename(name: String): Project =
        copy(name = name, updatedAt = System.currentTimeMillis())

    fun touch(): Project = copy(updatedAt = System.currentTimeMillis())

    /** Canvas-space bounds of every visible, unlocked-agnostic layer. */
    fun visibleLayers(): List<Layer> = layers.filter { it.visible && it.opacity > 0.01f }
}

/** Transform helpers that work uniformly across the sealed layer hierarchy. */
object LayerOps {

    fun translate(layer: Layer, dx: Float, dy: Float): Layer = when (layer) {
        is TextLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
        is ImageLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
        is ShapeLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
        is StickerLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
    }

    fun scaleTo(layer: Layer, w: Float, h: Float): Layer {
        val minW = 24f
        val minH = 16f
        val nw = w.coerceAtLeast(minW)
        val nh = h.coerceAtLeast(minH)
        return when (layer) {
            is TextLayer -> layer.copy(
                width = nw, height = nh,
                // font size follows box height so text stays visually proportional
                fontSize = (layer.fontSize * (nh / layer.height.coerceAtLeast(1f))).coerceIn(8f, 400f)
            )
            is ImageLayer -> layer.copy(width = nw, height = nh)
            is ShapeLayer -> layer.copy(width = nw, height = nh)
            is StickerLayer -> layer.copy(width = nw, height = nh)
        }
    }

    fun rotate(layer: Layer, degrees: Float): Layer {
        val r = ((degrees % 360f) + 360f) % 360f
        return when (layer) {
            is TextLayer -> layer.copy(rotation = r)
            is ImageLayer -> layer.copy(rotation = r)
            is ShapeLayer -> layer.copy(rotation = r)
            is StickerLayer -> layer.copy(rotation = r)
        }
    }

    fun withVisibility(layer: Layer, visible: Boolean): Layer = when (layer) {
        is TextLayer -> layer.copy(visible = visible)
        is ImageLayer -> layer.copy(visible = visible)
        is ShapeLayer -> layer.copy(visible = visible)
        is StickerLayer -> layer.copy(visible = visible)
    }

    fun withLock(layer: Layer, locked: Boolean): Layer = when (layer) {
        is TextLayer -> layer.copy(locked = locked)
        is ImageLayer -> layer.copy(locked = locked)
        is ShapeLayer -> layer.copy(locked = locked)
        is StickerLayer -> layer.copy(locked = locked)
    }

    fun withOpacity(layer: Layer, opacity: Float): Layer {
        val o = opacity.coerceIn(0f, 1f)
        return when (layer) {
            is TextLayer -> layer.copy(opacity = o)
            is ImageLayer -> layer.copy(opacity = o)
            is ShapeLayer -> layer.copy(opacity = o)
            is StickerLayer -> layer.copy(opacity = o)
        }
    }

    fun duplicated(layer: Layer): Layer {
        val offset = 28f
        return when (layer) {
            is TextLayer -> layer.copy(id = newId(), name = layer.name + " copy", x = layer.x + offset, y = layer.y + offset)
            is ImageLayer -> layer.copy(id = newId(), name = layer.name + " copy", x = layer.x + offset, y = layer.y + offset)
            is ShapeLayer -> layer.copy(id = newId(), name = layer.name + " copy", x = layer.x + offset, y = layer.y + offset)
            is StickerLayer -> layer.copy(id = newId(), name = layer.name + " copy", x = layer.x + offset, y = layer.y + offset)
        }
    }

    fun defaultName(layer: Layer): String = when (layer) {
        is TextLayer -> "Text"
        is ImageLayer -> "Image"
        is ShapeLayer -> layer.kind.name.lowercase().replace('_', ' ')
        is StickerLayer -> "Sticker"
    }
}
