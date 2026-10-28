package com.thumbnailstudio.app

import com.thumbnailstudio.app.ai.AiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTextTest {

    @Test
    fun picksTheLongestLinesAsHeadline() {
        val ocr = """
            CHANNEL NAME
            subscribe
            This is the actual headline of the video
            tiny
            Another meaningful caption line here
        """.trimIndent()
        val headline = AiText.headlineFromOcr(ocr, maxLines = 2)
        assertTrue(headline.contains("ACTUAL HEADLINE"))
        assertTrue(headline.contains("MEANINGFUL CAPTION"))
        assertEquals(2, headline.lines().size)
    }

    @Test
    fun ignoresBlankAndSymbolOnlyLines() {
        val headline = AiText.headlineFromOcr("\n\n|||\n---\n..\n", maxLines = 2)
        assertEquals("", headline)
    }

    @Test
    fun respectsCharacterBudget() {
        val long = "word ".repeat(60)
        val headline = AiText.headlineFromOcr(long, maxLines = 3)
        assertTrue(headline.length <= 90)
    }

    @Test
    fun cleansOcrNoiseAndTrimsPunctuation() {
        assertEquals("Hello World", AiText.clean("  | Hello   World :  "))
        assertEquals("Clean", AiText.clean("...Clean..."))
    }

    @Test
    fun layerNameIsShort() {
        val name = AiText.layerName("A VERY LONG HEADLINE THAT SHOULD BE TRUNCATED")
        assertTrue(name.length <= 24)
    }

    @Test
    fun captionDetectionNeedsRealWords() {
        assertFalse(AiText.looksLikeCaption("the and"))
        assertTrue(AiText.looksLikeCaption("breaking news today"))
    }
}
