package com.thumbnailstudio.app.data

import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.Layer
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.ShapeKind
import com.thumbnailstudio.app.model.ShapeLayer
import com.thumbnailstudio.app.model.StickerLayer
import com.thumbnailstudio.app.model.TextAlignH
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.model.newId

/**
 * Built-in templates are generated in code, so the APK ships no template
 * bitmaps and nothing is downloaded at launch. Every element stays editable.
 */
object Templates {

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    data class TemplateDef(
        val id: String,
        val name: String,
        val category: String,
        val accent: Int,
        val build: () -> Project
    )

    private fun title(
        text: String,
        y: Float,
        size: Float,
        color: Int = WHITE,
        width: Float = 1040f,
        x: Float = 640f,
        font: String = "sans_black",
        strokeColor: Int? = null,
        strokeWidth: Float = 12f,
        shadow: Boolean = true,
        uppercase: Boolean = true,
        rotation: Float = 0f,
        box: Boolean = false,
        boxColor: Int = 0xCC000000.toInt(),
        curve: Float = 0f,
        letterSpacing: Float = 0f,
        align: TextAlignH = TextAlignH.CENTER,
        name: String = "Title"
    ) = TextLayer(
        id = newId(),
        name = name,
        x = x,
        y = y,
        width = width,
        height = size * 1.45f,
        rotation = rotation,
        text = text,
        fontKey = font,
        fontSize = size,
        bold = true,
        uppercase = uppercase,
        color = color,
        strokeEnabled = strokeColor != null,
        strokeColor = strokeColor ?: BLACK,
        strokeWidth = strokeWidth,
        shadowEnabled = shadow,
        shadowBlur = 18f,
        shadowDy = 8f,
        shadowDx = 2f,
        boxEnabled = box,
        boxColor = boxColor,
        curve = curve,
        letterSpacing = letterSpacing,
        align = align,
        depth3d = 0f
    )

    private fun shape(
        kind: ShapeKind,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        fill: Int,
        rotation: Float = 0f,
        gradient: Boolean = false,
        fill2: Int = fill,
        corner: Float = 28f,
        strokeColor: Int = 0,
        strokeWidth: Float = 0f,
        name: String = "Shape"
    ) = ShapeLayer(
        id = newId(), name = name, x = x, y = y, width = w, height = h,
        rotation = rotation, kind = kind, fill = fill, gradient = gradient, fill2 = fill2,
        cornerRadius = corner, strokeColor = strokeColor, strokeWidth = strokeWidth
    )

    private fun sticker(glyph: String, x: Float, y: Float, size: Float, rotation: Float = 0f) =
        StickerLayer(id = newId(), name = "Sticker", x = x, y = y, width = size, height = size,
            glyph = glyph, rotation = rotation)

    private fun project(
        name: String,
        category: String,
        bg: Background,
        layers: List<Layer>
    ) = Project(
        id = newId(),
        name = name,
        category = category,
        background = bg,
        layers = layers,
        isTemplate = true
    )

    private fun gradient(c1: Int, c2: Int, angle: Float = 135f, vignette: Float = 0.45f) =
        Background(type = BgType.GRADIENT, color1 = c1, color2 = c2, angle = angle, vignette = vignette)

    val all: List<TemplateDef> = listOf(

        TemplateDef("gaming_rage", "Rage Quit", "Gaming", 0xFFFF3B30.toInt()) {
            project(
                "Rage Quit", "Gaming",
                gradient(0xFF1A0505.toInt(), 0xFF7F1D1D.toInt(), 120f, 0.6f),
                listOf(
                    shape(ShapeKind.TRIANGLE, 1050f, 250f, 420f, 420f, 0x33FF3B30.toInt(), rotation = 18f),
                    shape(ShapeKind.TRIANGLE, 1080f, 250f, 380f, 380f, 0x22FF3B30.toInt(), rotation = 18f),
                    sticker("\uD83C\uDFAE", 1080f, 250f, 240f, 12f),
                    shape(ShapeKind.BADGE, 210f, 120f, 240f, 88f, 0xFFFF3B30.toInt(), rotation = -8f, name = "Badge"),
                    title("NEW", 120f, 46f, WHITE, width = 240f, x = 210f, rotation = -8f, shadow = false, name = "Badge text"),
                    title("I BROKE\nTHE GAME", 470f, 150f, WHITE, strokeColor = BLACK, strokeWidth = 14f, rotation = -3f),
                    title("episode 07", 640f, 44f, 0xFFFDA4AF.toInt(), width = 900f, font = "condensed", letterSpacing = 18f, shadow = false)
                )
            )
        },

        TemplateDef("gaming_neon", "Neon Setup", "Gaming", 0xFF22D3EE.toInt()) {
            project(
                "Neon Setup", "Gaming",
                gradient(0xFF020617.toInt(), 0xFF0E7490.toInt(), 200f, 0.55f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 640f, 560f, 1120f, 300f, 0x223B82F6.toInt(), corner = 40f, name = "Glow slab"),
                    shape(ShapeKind.FRAME, 640f, 360f, 1180f, 620f, 0x00000000.toInt(), strokeColor = 0xFF22D3EE.toInt(), strokeWidth = 8f, corner = 44f, name = "Frame"),
                    title("INSANE\nCLUTCH", 330f, 138f, WHITE, strokeColor = 0xFF22D3EE.toInt(), strokeWidth = 12f),
                    shape(ShapeKind.LINE, 640f, 470f, 520f, 12f, 0xFF22D3EE.toInt(), name = "Divider"),
                    title("ranked highlights", 530f, 46f, 0xFFA5F3FC.toInt(), font = "condensed", letterSpacing = 12f, shadow = false)
                )
            )
        },

        TemplateDef("music_lyric", "Lyric Wave", "Music", 0xFFC084FC.toInt()) {
            project(
                "Lyric Wave", "Music",
                gradient(0xFF2E1065.toInt(), 0xFF9D174D.toInt(), 160f, 0.5f),
                listOf(
                    shape(ShapeKind.CIRCLE, 1060f, 500f, 520f, 520f, 0x33FFFFFF.toInt(), name = "Halo"),
                    sticker("\uD83C\uDFB5", 1060f, 500f, 260f, -14f),
                    title("MIDNIGHT\nDRIVE", 260f, 132f, WHITE, width = 900f, x = 470f, curve = 34f, strokeColor = 0xFF2E1065.toInt(), strokeWidth = 10f),
                    title("official lyric video", 470f, 40f, 0xFFF9A8D4.toInt(), width = 860f, x = 470f, font = "condensed", letterSpacing = 14f, shadow = false)
                )
            )
        },

        TemplateDef("music_ai", "AI Beats", "Music", 0xFF34D399.toInt()) {
            project(
                "AI Beats", "Music",
                gradient(0xFF052E2B.toInt(), 0xFF0F766E.toInt(), 90f, 0.55f),
                listOf(
                    shape(ShapeKind.BURST, 1080f, 200f, 340f, 340f, 0x3334D399.toInt(), name = "Burst"),
                    sticker("\uD83C\uDFA7", 1080f, 200f, 200f),
                    title("MADE BY\nA MACHINE", 430f, 118f, WHITE, width = 900f, x = 520f, strokeColor = BLACK, strokeWidth = 10f),
                    shape(ShapeKind.ROUND_RECT, 520f, 600f, 560f, 84f, 0xFF34D399.toInt(), corner = 42f, name = "Tag"),
                    title("10 TRACKS FREE", 600f, 40f, 0xFF022C22.toInt(), width = 560f, x = 520f, shadow = false, name = "Tag text")
                )
            )
        },

        TemplateDef("tech_ai", "Neural Tech", "Technology", 0xFF60A5FA.toInt()) {
            project(
                "Neural Tech", "Technology",
                gradient(0xFF0B1220.toInt(), 0xFF1D4ED8.toInt(), 145f, 0.5f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 640f, 600f, 1160f, 200f, 0x331E40AF.toInt(), corner = 32f, name = "Panel"),
                    shape(ShapeKind.LINE, 640f, 512f, 900f, 8f, 0xFF60A5FA.toInt(), name = "Rule"),
                    title("THIS CHANGES\nEVERYTHING", 300f, 118f, WHITE, width = 1000f),
                    title("gpt-5 explained", 585f, 44f, 0xFF93C5FD.toInt(), font = "mono", letterSpacing = 10f, shadow = false),
                    sticker("\u26A1", 1140f, 120f, 150f, 10f)
                )
            )
        },

        TemplateDef("tech_review", "Spec Sheet", "Technology", 0xFFF59E0B.toInt()) {
            project(
                "Spec Sheet", "Technology",
                gradient(0xFF111827.toInt(), 0xFF374151.toInt(), 200f, 0.45f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 1000f, 360f, 460f, 520f, 0x22F59E0B.toInt(), corner = 36f, name = "Product frame"),
                    shape(ShapeKind.FRAME, 1000f, 360f, 460f, 520f, 0x00000000.toInt(), strokeColor = 0xFFF59E0B.toInt(), strokeWidth = 6f, corner = 36f, name = "Frame"),
                    sticker("\uD83D\uDCF1", 1000f, 360f, 260f),
                    title("WORTH\nTHE MONEY?", 250f, 110f, WHITE, width = 700f, x = 380f, align = TextAlignH.LEFT),
                    title("full review", 470f, 42f, 0xFFFCD34D.toInt(), width = 700f, x = 380f, font = "condensed", align = TextAlignH.LEFT, shadow = false)
                )
            )
        },

        TemplateDef("edu_lesson", "Clean Lesson", "Education", 0xFF2563EB.toInt()) {
            project(
                "Clean Lesson", "Education",
                Background(type = BgType.SOLID, color1 = 0xFFF8FAFC.toInt(), vignette = 0.1f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 640f, 300f, 1100f, 300f, 0xFF2563EB.toInt(), corner = 40f, name = "Highlight"),
                    title("LEARN PYTHON\nIN 20 MINUTES", 300f, 104f, WHITE, width = 1000f, shadow = false),
                    shape(ShapeKind.ROUND_RECT, 640f, 560f, 560f, 92f, 0xFF0F172A.toInt(), corner = 46f, name = "Pill"),
                    title("BEGINNER FRIENDLY", 560f, 38f, WHITE, width = 540f, shadow = false, name = "Pill text"),
                    sticker("\uD83D\uDCDA", 1130f, 130f, 150f)
                )
            )
        },

        TemplateDef("edu_tutorial", "Chalkboard", "Education", 0xFF10B981.toInt()) {
            project(
                "Chalkboard", "Education",
                gradient(0xFF064E3B.toInt(), 0xFF065F46.toInt(), 120f, 0.4f),
                listOf(
                    shape(ShapeKind.FRAME, 640f, 360f, 1160f, 600f, 0x00000000.toInt(), strokeColor = 0x66FFFFFF.toInt(), strokeWidth = 5f, corner = 24f, name = "Board"),
                    title("5 MISTAKES\nTO AVOID", 300f, 120f, WHITE, width = 980f, font = "serif_bold"),
                    title("study smarter", 560f, 46f, 0xFF6EE7B7.toInt(), width = 900f, font = "serif", shadow = false),
                    sticker("\u270F\uFE0F", 1110f, 140f, 140f, -12f)
                )
            )
        },

        TemplateDef("vlog_day", "Day In Life", "Vlogs", 0xFFFB7185.toInt()) {
            project(
                "Day In Life", "Vlogs",
                gradient(0xFF7C2D12.toInt(), 0xFFF59E0B.toInt(), 60f, 0.35f),
                listOf(
                    shape(ShapeKind.CIRCLE, 980f, 400f, 560f, 560f, 0x33FFFFFF.toInt(), name = "Photo circle"),
                    shape(ShapeKind.CIRCLE, 980f, 400f, 500f, 500f, 0x22000000.toInt(), name = "Photo ring"),
                    sticker("\uD83D\uDCF8", 980f, 400f, 240f),
                    title("A DAY IN\nMY LIFE", 260f, 122f, WHITE, width = 760f, x = 380f, align = TextAlignH.LEFT, strokeColor = 0xFF7C2D12.toInt(), strokeWidth = 10f),
                    title("ep 12 \u00B7 tokyo", 470f, 40f, 0xFFFFE4E6.toInt(), width = 760f, x = 380f, align = TextAlignH.LEFT, font = "condensed", shadow = false)
                )
            )
        },

        TemplateDef("vlog_travel", "Wander", "Vlogs", 0xFF38BDF8.toInt()) {
            project(
                "Wander", "Vlogs",
                gradient(0xFF0C4A6E.toInt(), 0xFF0284C7.toInt(), 180f, 0.4f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 640f, 590f, 1200f, 220f, 0x33FFFFFF.toInt(), corner = 40f, name = "Glass bar"),
                    title("7 DAYS\nIN BALI", 280f, 130f, WHITE, width = 940f),
                    title("under 30k rupees", 600f, 46f, 0xFFE0F2FE.toInt(), font = "condensed", letterSpacing = 8f, shadow = false),
                    sticker("\uD83C\uDF34", 1120f, 150f, 160f)
                )
            )
        },

        TemplateDef("cine_drama", "Letterbox", "Cinematic", 0xFFEAB308.toInt()) {
            project(
                "Letterbox", "Cinematic",
                gradient(0xFF000000.toInt(), 0xFF1F2937.toInt(), 180f, 0.7f),
                listOf(
                    shape(ShapeKind.RECT, 640f, 52f, 1280f, 104f, BLACK, name = "Bar top"),
                    shape(ShapeKind.RECT, 640f, 668f, 1280f, 104f, BLACK, name = "Bar bottom"),
                    title("THE LAST\nSIGNAL", 360f, 128f, WHITE, width = 1000f, font = "serif_bold", strokeColor = null, letterSpacing = 6f),
                    shape(ShapeKind.LINE, 640f, 480f, 320f, 6f, 0xFFEAB308.toInt(), name = "Rule"),
                    title("a short film", 540f, 40f, 0xFFEAB308.toInt(), font = "serif", letterSpacing = 20f, shadow = false)
                )
            )
        },

        TemplateDef("news_breaking", "Breaking", "News", 0xFFDC2626.toInt()) {
            project(
                "Breaking", "News",
                gradient(0xFF111827.toInt(), 0xFF1F2937.toInt(), 90f, 0.4f),
                listOf(
                    shape(ShapeKind.RECT, 640f, 620f, 1280f, 200f, 0xFFDC2626.toInt(), name = "Banner"),
                    shape(ShapeKind.ROUND_RECT, 190f, 110f, 300f, 80f, WHITE, corner = 12f, name = "Breaking badge"),
                    title("BREAKING", 80f, 44f, 0xFFDC2626.toInt(), width = 280f, x = 190f, shadow = false, name = "Badge text"),
                    title("MARKET CRASHES\nAFTER ANNOUNCEMENT", 300f, 108f, WHITE, width = 1120f),
                    title("live updates \u00B7 6 pm", 620f, 52f, WHITE, width = 1100f, font = "condensed", letterSpacing = 10f, shadow = false, name = "Banner text")
                )
            )
        },

        TemplateDef("review_unbox", "Unboxed", "Reviews", 0xFFA3E635.toInt()) {
            project(
                "Unboxed", "Reviews",
                gradient(0xFF1C1917.toInt(), 0xFF44403C.toInt(), 140f, 0.5f),
                listOf(
                    shape(ShapeKind.ROUND_RECT, 640f, 360f, 1180f, 620f, 0x22A3E635.toInt(), corner = 48f, name = "Card"),
                    shape(ShapeKind.STAR, 1120f, 150f, 150f, 150f, 0xFFA3E635.toInt(), name = "Star"),
                    sticker("\uD83D\uDCE6", 950f, 430f, 300f),
                    title("IS IT WORTH\nTHE HYPE?", 250f, 112f, WHITE, width = 720f, x = 420f, align = TextAlignH.LEFT),
                    title("honest review", 450f, 40f, 0xFFBEF264.toInt(), width = 720f, x = 420f, align = TextAlignH.LEFT, font = "condensed", shadow = false)
                )
            )
        },

        TemplateDef("podcast_talk", "Podcast Cover", "Podcast", 0xFF8B5CF6.toInt()) {
            project(
                "Podcast Cover", "Podcast",
                gradient(0xFF1E1B4B.toInt(), 0xFF4C1D95.toInt(), 160f, 0.5f),
                listOf(
                    shape(ShapeKind.CIRCLE, 640f, 250f, 380f, 380f, 0x448B5CF6.toInt(), name = "Halo"),
                    sticker("\uD83C\uDF99\uFE0F", 640f, 250f, 220f),
                    title("THE FOUNDER\nTAPES", 480f, 96f, WHITE, width = 1000f, strokeColor = 0xFF1E1B4B.toInt(), strokeWidth = 8f),
                    shape(ShapeKind.ROUND_RECT, 640f, 620f, 640f, 80f, 0x33FFFFFF.toInt(), corner = 40f, name = "Pill"),
                    title("episode 24 \u00B7 growth", 618f, 38f, 0xFFE9D5FF.toInt(), width = 620f, shadow = false, name = "Pill text")
                )
            )
        },

        TemplateDef("ent_react", "Reaction", "Entertainment", 0xFFF97316.toInt()) {
            project(
                "Reaction", "Entertainment",
                gradient(0xFF111827.toInt(), 0xFF7C2D12.toInt(), 45f, 0.5f),
                listOf(
                    shape(ShapeKind.BURST, 980f, 380f, 620f, 620f, 0x33F97316.toInt(), name = "Burst"),
                    sticker("\uD83D\uDE31", 980f, 380f, 320f),
                    title("I CAN'T\nBELIEVE THIS", 250f, 118f, WHITE, width = 720f, x = 400f, align = TextAlignH.LEFT, strokeColor = BLACK, strokeWidth = 12f, rotation = -2f),
                    title("reaction video", 460f, 42f, 0xFFFDBA74.toInt(), width = 720f, x = 400f, align = TextAlignH.LEFT, font = "condensed", shadow = false)
                )
            )
        }
    )

    val categories: List<String> = listOf(
        "All", "Gaming", "Music", "Technology", "Education", "Vlogs", "Cinematic", "News", "Reviews", "Podcast", "Entertainment"
    )

    fun byId(id: String): TemplateDef? = all.firstOrNull { it.id == id }

    fun byCategory(category: String): List<TemplateDef> =
        if (category == "All") all else all.filter { it.category == category }

    /** Fresh, editable project from a template. */
    fun instantiate(def: TemplateDef): Project {
        val base = def.build()
        return base.copy(
            id = newId(),
            isTemplate = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
