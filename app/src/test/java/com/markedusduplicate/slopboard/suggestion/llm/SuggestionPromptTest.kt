package com.markedusduplicate.slopboard.suggestion.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionPromptTest {

    @Test
    fun `parses a clean json array`() {
        assertEquals(
            listOf("the", "quick", "brown"),
            SuggestionPrompt.parse("""["the","quick","brown"]"""),
        )
    }

    @Test
    fun `extracts the array from surrounding prose`() {
        assertEquals(
            listOf("a", "b", "c"),
            SuggestionPrompt.parse("""Sure! ["a", "b", "c"] hope that helps"""),
        )
    }

    @Test
    fun `falls back to a loose comma list`() {
        assertEquals(listOf("cat", "dog", "bird"), SuggestionPrompt.parse("cat, dog, bird"))
    }

    @Test
    fun `caps at three and dedupes case-insensitively`() {
        assertEquals(listOf("Hi", "yo"), SuggestionPrompt.parse("""["Hi","hi","yo"]"""))
        assertEquals(listOf("a", "b", "c"), SuggestionPrompt.parse("""["a","b","c","d"]"""))
    }

    @Test
    fun `returns nothing for empty input`() {
        assertTrue(SuggestionPrompt.parse("").isEmpty())
    }

    @Test
    fun `omits the personalization line when there are no hints`() {
        val prompt = SuggestionPrompt.build("what the ", "what the", emptyList())
        assertFalse(prompt.contains("often follows"))
        assertTrue(prompt.contains("what the "))
    }

    @Test
    fun `injects personalization hints when present`() {
        val prompt = SuggestionPrompt.build("what the ", "what the", listOf("heck", "hell"))
        assertTrue(prompt.contains("""often follows "what the" with: heck, hell"""))
    }
}
