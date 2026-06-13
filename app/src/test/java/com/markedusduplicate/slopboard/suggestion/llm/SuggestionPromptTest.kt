package com.markedusduplicate.slopboard.suggestion.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionPromptTest {

    @Test
    fun `next-word prompt asks for one word and includes the text, with no personalization`() {
        val prompt = SuggestionPrompt.nextWord("how are ")
        assertTrue(prompt.contains("next word"))
        assertTrue(prompt.contains("single next word"))
        assertTrue(prompt.contains("how are "))
        assertFalse(prompt.contains("often follows"))
    }

    @Test
    fun `parses a bare word reply`() {
        assertEquals(listOf("morning"), SuggestionPrompt.parse("morning").words)
    }

    @Test
    fun `parses a json array and keeps the leading word first`() {
        assertEquals(listOf("you", "things"), SuggestionPrompt.parse("""["you", "things"]""").words)
    }

    @Test
    fun `falls back to a loose comma list`() {
        assertEquals(listOf("cat", "dog", "bird"), SuggestionPrompt.parse("cat, dog, bird").words)
    }

    @Test
    fun `keeps only the first word of a multi-word entry`() {
        assertEquals(listOf("going", "want"), SuggestionPrompt.parse("""["going to", "want to"]""").words)
    }

    @Test
    fun `caps at three and dedupes case-insensitively`() {
        assertEquals(listOf("Hi", "yo"), SuggestionPrompt.parse("""["Hi","hi","yo"]""").words)
        assertEquals(listOf("a", "b", "c"), SuggestionPrompt.parse("""["a","b","c","d"]""").words)
    }

    @Test
    fun `returns nothing for empty input`() {
        assertTrue(SuggestionPrompt.parse("").words.isEmpty())
    }
}
