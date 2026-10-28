package com.thumbnailstudio.app

import com.thumbnailstudio.app.data.Templates
import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.ImageLayer
import com.thumbnailstudio.app.model.LayerOps
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.ShapeLayer
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.model.newId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectModelTest {

    private fun sampleProject() = Project(
        id = "p1",
        name = "Sample",
        layers = listOf(
            TextLayer(id = "t1", text = "HELLO", x = 640f, y = 200f),
            ShapeLayer(id = "s1", x = 400f, y = 400f)
        )
    )

    @Test
    fun addAndRemoveLayersKeepsOrder() {
        val project = sampleProject()
        val image = ImageLayer(id = "i1", path = "/tmp/x.png")
        val added = project.add(image)
        assertEquals(listOf("t1", "s1", "i1"), added.layers.map { it.id })
        assertEquals(listOf("t1", "s1"), added.remove("i1").layers.map { it.id })
    }

    @Test
    fun replaceKeepsZOrder() {
        val project = sampleProject()
        val updated = project.replace(TextLayer(id = "t1", text = "NEW", x = 10f, y = 10f))
        assertEquals(listOf("t1", "s1"), updated.layers.map { it.id })
        assertEquals("NEW", (updated.layer("t1") as TextLayer).text)
    }

    @Test
    fun duplicateInsertsDirectlyAboveSource() {
        val project = sampleProject()
        val duplicated = project.duplicate("t1")
        assertEquals(3, duplicated.layers.size)
        assertEquals("t1", duplicated.layers[0].id)
        assertEquals("s1", duplicated.layers[2].id)
        assertNotEquals(duplicated.layers[0].id, duplicated.layers[1].id)
        assertEquals("Text copy", duplicated.layers[1].name)
    }

    @Test
    fun reorderAndFrontBackWork() {
        val project = sampleProject()
        val moved = project.move(0, 1)
        assertEquals(listOf("s1", "t1"), moved.layers.map { it.id })
        assertEquals("s1", moved.sendToBack("s1").layers.first().id)
        assertEquals("t1", moved.bringToFront("t1").layers.last().id)
    }

    @Test
    fun translateScaleRotateClampSensibly() {
        val text = TextLayer(id = "t1", width = 400f, height = 200f, fontSize = 80f)
        val moved = LayerOps.translate(text, 40f, -20f) as TextLayer
        assertEquals(text.x + 40f, moved.x, 0.01f)
        val scaled = LayerOps.scaleTo(text, 800f, 400f) as TextLayer
        assertEquals(800f, scaled.width, 0.01f)
        assertEquals(160f, scaled.fontSize, 0.5f)
        val tiny = LayerOps.scaleTo(text, 1f, 1f)
        assertTrue(tiny.width >= 24f)
        val rotated = LayerOps.rotate(text, -90f)
        assertEquals(270f, rotated.rotation, 0.01f)
    }

    @Test
    fun lockAndVisibilityRoundTrip() {
        val text = TextLayer(id = "t1")
        assertFalse(LayerOps.withLock(text, true).locked.not())
        assertFalse(LayerOps.withVisibility(text, false).visible)
    }

    @Test
    fun jsonRoundTripPreservesEverything() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val project = sampleProject().copy(background = Background(BgType.GRADIENT, 0xFF000000.toInt(), 0xFFFFFFFF.toInt()))
        val encoded = json.encodeToString(Project.serializer(), project)
        val decoded = json.decodeFromString(Project.serializer(), encoded)
        assertEquals(project, decoded)
        assertTrue(decoded.layers[0] is TextLayer)
        assertTrue(decoded.layers[1] is ShapeLayer)
    }

    @Test
    fun visibleLayersFilterHiddenAndTransparent() {
        val project = Project(
            layers = listOf(
                TextLayer(id = "a"),
                TextLayer(id = "b", visible = false),
                TextLayer(id = "c", opacity = 0f)
            )
        )
        assertEquals(listOf("a"), project.visibleLayers().map { it.id })
    }
}

class TemplatesTest {

    @Test
    fun everyTemplateBuildsEditableLayers() {
        assertTrue(Templates.all.size >= 10)
        for (def in Templates.all) {
            val project = def.build()
            assertTrue("${def.id} has no layers", project.layers.isNotEmpty())
            assertEquals(1280, project.width)
            assertEquals(720, project.height)
            assertTrue(project.isTemplate)
            val ids = project.layers.map { it.id }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test
    fun instantiateProducesFreshNonTemplateProject() {
        val def = Templates.all.first()
        val a = Templates.instantiate(def)
        val b = Templates.instantiate(def)
        assertFalse(a.isTemplate)
        assertNotEquals(a.id, b.id)
        assertEquals(a.layers.size, b.layers.size)
    }

    @Test
    fun categoriesCoverEveryTemplate() {
        val fromCategories = Templates.categories
            .filter { it != "All" }
            .flatMap { Templates.byCategory(it) }
            .map { it.id }
            .toSet()
        assertEquals(Templates.all.map { it.id }.toSet(), fromCategories)
    }
}
