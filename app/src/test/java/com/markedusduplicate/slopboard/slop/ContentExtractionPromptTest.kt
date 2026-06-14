package com.markedusduplicate.slopboard.slop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentExtractionPromptTest {

    @Test
    fun `select numbers the lines and asks for a JSON array`() {
        val prompt = ContentExtractionPrompt.select(listOf("hello", "world"))
        assertTrue(prompt.contains("0: hello"))
        assertTrue(prompt.contains("1: world"))
        assertTrue(prompt.contains("JSON array"))
    }

    @Test
    fun `parse reads a JSON array`() {
        assertEquals(listOf(3, 4, 5), ContentExtractionPrompt.parse("[3,4,5]"))
    }

    @Test
    fun `parse reads a loose comma list`() {
        assertEquals(listOf(3, 4, 5), ContentExtractionPrompt.parse("3, 4, 5"))
    }

    @Test
    fun `parse expands ranges`() {
        assertEquals(listOf(3, 4, 5), ContentExtractionPrompt.parse("lines 3-5"))
    }

    @Test
    fun `parse dedupes and sorts`() {
        assertEquals(listOf(1, 2), ContentExtractionPrompt.parse("[2, 1, 1, 2]"))
    }

    @Test
    fun `parse returns empty when there are no numbers`() {
        assertTrue(ContentExtractionPrompt.parse("nothing here").isEmpty())
    }

    @Test
    fun `reconstruct joins selected lines verbatim and in order`() {
        val lines = listOf("a", "b", "c", "d")
        assertEquals("b\nd", ContentExtractionPrompt.reconstruct(lines, listOf(1, 3)))
    }

    @Test
    fun `reconstruct ignores out-of-range indices`() {
        val lines = listOf("a", "b")
        assertEquals("b", ContentExtractionPrompt.reconstruct(lines, listOf(1, 9)))
    }

    @Test
    fun `largestBlock returns the longest blank-line-delimited block`() {
        val text = "short\n\nthis is the much longer paragraph block\n\nmid"
        assertEquals("this is the much longer paragraph block", ContentExtractionPrompt.largestBlock(text))
    }
}
