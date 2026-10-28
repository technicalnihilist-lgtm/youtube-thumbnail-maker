package com.thumbnailstudio.app.render

import android.graphics.Path
import android.graphics.RectF
import com.thumbnailstudio.app.model.ShapeKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Vector geometry for every built-in shape, drawn inside a (0,0,w,h) box. */
object ShapePaths {

    fun build(kind: ShapeKind, w: Float, h: Float, corner: Float): Path {
        val p = Path()
        when (kind) {
            ShapeKind.RECT -> p.addRect(0f, 0f, w, h, Path.Direction.CW)
            ShapeKind.ROUND_RECT -> p.addRoundRect(
                RectF(0f, 0f, w, h),
                corner.coerceIn(0f, min(w, h) / 2f),
                corner.coerceIn(0f, min(w, h) / 2f),
                Path.Direction.CW
            )
            ShapeKind.CIRCLE -> p.addOval(RectF(0f, 0f, w, h), Path.Direction.CW)
            ShapeKind.TRIANGLE -> {
                p.moveTo(w / 2f, 0f); p.lineTo(w, h); p.lineTo(0f, h); p.close()
            }
            ShapeKind.STAR -> star(p, w, h, 5, 0.42f)
            ShapeKind.BURST -> star(p, w, h, 12, 0.72f)
            ShapeKind.ARROW_RIGHT -> {
                val shaft = h * 0.34f
                val top = (h - shaft) / 2f
                val headStart = w * 0.62f
                p.moveTo(0f, top)
                p.lineTo(headStart, top)
                p.lineTo(headStart, 0f)
                p.lineTo(w, h / 2f)
                p.lineTo(headStart, h)
                p.lineTo(headStart, top + shaft)
                p.lineTo(0f, top + shaft)
                p.close()
            }
            ShapeKind.LINE -> {
                val t = (h * 0.5f).coerceAtMost(h)
                p.addRoundRect(RectF(0f, (h - t) / 2f, w, (h + t) / 2f), t / 2f, t / 2f, Path.Direction.CW)
            }
            ShapeKind.FRAME -> {
                val t = (min(w, h) * 0.08f).coerceAtLeast(2f)
                val outer = Path().apply {
                    addRoundRect(RectF(0f, 0f, w, h), corner, corner, Path.Direction.CW)
                }
                val inner = Path().apply {
                    addRoundRect(
                        RectF(t, t, w - t, h - t),
                        (corner - t).coerceAtLeast(0f), (corner - t).coerceAtLeast(0f),
                        Path.Direction.CW
                    )
                }
                p.op(outer, inner, Path.Op.DIFFERENCE)
            }
            ShapeKind.BADGE -> {
                val notch = h * 0.18f
                p.moveTo(notch, 0f)
                p.lineTo(w - notch, 0f)
                p.lineTo(w, h / 2f)
                p.lineTo(w - notch, h)
                p.lineTo(notch, h)
                p.lineTo(0f, h / 2f)
                p.close()
            }
            ShapeKind.SPEECH -> {
                val tail = h * 0.18f
                p.addRoundRect(RectF(0f, 0f, w, h - tail), corner, corner, Path.Direction.CW)
                val tri = Path().apply {
                    moveTo(w * 0.22f, h - tail - 1f)
                    lineTo(w * 0.22f + tail * 1.6f, h - tail - 1f)
                    lineTo(w * 0.2f, h)
                    close()
                }
                p.op(p, tri, Path.Op.UNION)
            }
            ShapeKind.HEART -> {
                p.moveTo(w / 2f, h * 0.95f)
                p.cubicTo(-w * 0.15f, h * 0.55f, w * 0.12f, -h * 0.05f, w / 2f, h * 0.28f)
                p.cubicTo(w * 0.88f, -h * 0.05f, w * 1.15f, h * 0.55f, w / 2f, h * 0.95f)
                p.close()
            }
        }
        return p
    }

    private fun star(p: Path, w: Float, h: Float, points: Int, innerRatio: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val rx = w / 2f
        val ry = h / 2f
        val step = Math.PI / points
        var angle = -Math.PI / 2
        for (i in 0 until points * 2) {
            val ratio = if (i % 2 == 0) 1f else innerRatio
            val px = cx + (rx * ratio * cos(angle)).toFloat()
            val py = cy + (ry * ratio * sin(angle)).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            angle += step
        }
        p.close()
    }
}
