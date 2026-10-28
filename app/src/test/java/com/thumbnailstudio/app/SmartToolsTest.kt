package com.thumbnailstudio.app

import com.thumbnailstudio.app.export.Exporter
import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.tools.SmartTools
import com.thumbnailstudio.app.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartToolsTest {

    @Test
    fun contrastRatioMatchesWcagExtremes() {
        assertEquals(21.0, SmartTools.contrastRatio(0xFFFFFFFF.toInt(), 0xFF000000.toInt()), 0.01)
        assertEquals(1.0, SmartTools.contrastRatio(0xFF3366AA.toInt(), 0xFF3366AA.toInt()), 0.001)
    }

    @Test
    fun bestForegroundPicksReadableColour() {
        assertEquals(0xFFFFFFFF.toInt(), SmartTools.bestForeground(0xFF101010.toInt()))
        assertEquals(0xFF000000.toInt(), SmartTools.bestForeground(0xFFF5F5F5.toInt()))
    }

    @Test
    fun readabilityLabelsMapToBands() {
        assertEquals("Excellent", SmartTools.readabilityLabel(12.0))
        assertEquals("Good", SmartTools.readabilityLabel(5.0))
        assertEquals("Low", SmartTools.readabilityLabel(3.2))
        assertEquals("Poor", SmartTools.readabilityLabel(1.4))
    }

    @Test
    fun hexRoundTrip() {
        val colour = 0xFF6D5BFF.toInt()
        assertEquals(colour, SmartTools.parseHex(SmartTools.toHex(colour)))
        assertEquals(0xFF112233.toInt(), SmartTools.parseHex("#112233"))
        assertEquals(0xFF000000.toInt(), SmartTools.parseHex("nonsense", 0xFF000000.toInt()))
    }

    @Test
    fun harmonizeProducesDistinctColours() {
        val base = 0xFF3B82F6.toInt()
        val palette = SmartTools.harmonize(base, SmartTools.Harmony.COMPLEMENTARY)
        assertEquals(5, palette.size)
        assertTrue(palette.toSet().size >= 3)
        val mono = SmartTools.harmonize(base, SmartTools.Harmony.MONOCHROME, 3)
        assertEquals(3, mono.size)
    }

    @Test
    fun quantizePaletteReducesToDominantColours() {
        val red = 0xFFE11D48.toInt()
        val blue = 0xFF1D4ED8.toInt()
        val samples = List(60) { red } + List(10) { blue } + List(5) { 0x00FFFFFF }
        val palette = SmartTools.quantizePalette(samples, 2)
        assertEquals(2, palette.size)
        assertTrue(palette.first() == red || palette.first() == blue)
    }

    @Test
    fun autoFixContrastRaisesScore() {
        val project = Project(
            background = Background(BgType.SOLID, 0xFFFFFFFF.toInt()),
            layers = listOf(TextLayer(id = "t", color = 0xFFEEEEEE.toInt()))
        )
        val before = SmartTools.scoreThumbnail(project)
        val fixed = SmartTools.autoFixContrast(project)
        val after = SmartTools.scoreThumbnail(fixed)
        assertTrue(after >= before)
        val fixedLayer = fixed.layer("t") as TextLayer
        assertTrue(SmartTools.contrastRatio(fixedLayer.color, 0xFFFFFFFF.toInt()) >= 4.5)
    }

    @Test
    fun checklistFlagsMissingHeadline() {
        val empty = Project(background = Background(BgType.SOLID, 0xFF000000.toInt()))
        val checks = SmartTools.checklist(empty)
        assertFalse(checks.first { it.first.startsWith("Has a headline") }.second)
    }

    @Test
    fun layoutSuggestionsKeepLayersInsideCanvas() {
        val project = Project(layers = listOf(TextLayer(id = "t"), TextLayer(id = "t2", y = 500f)))
        for (suggestion in SmartTools.layoutSuggestions(project)) {
            val applied = suggestion.apply(project)
            assertEquals(project.layers.size, applied.layers.size)
            for (layer in applied.layers) {
                assertTrue("${suggestion.id}: x out of bounds", layer.x > 0f && layer.x < project.width)
                assertTrue("${suggestion.id}: y out of bounds", layer.y > 0f && layer.y < project.height)
            }
        }
    }

    @Test
    fun scoreIsBounded() {
        val project = Project(layers = List(20) { TextLayer(id = "t$it", fontSize = 20f) })
        val score = SmartTools.scoreThumbnail(project)
        assertTrue(score in 0..100)
    }
}

class ExportNamingTest {

    @Test
    fun sanitizeStripsUnsafeCharacters() {
        assertEquals("My_Thumbnail", Exporter.sanitize("My Thumbnail!"))
        assertEquals("ab", Exporter.sanitize("  a/b?  "))
        assertTrue(Exporter.sanitize("../../etc/passwd").none { it == '/' || it == '.' })
    }
}

class UpdateVersionTest {

    @Test
    fun comparesSemanticVersions() {
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("v1.1.0", "1.0.9"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("0.9.9", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.0.10", "1.0.9"))
    }
}
