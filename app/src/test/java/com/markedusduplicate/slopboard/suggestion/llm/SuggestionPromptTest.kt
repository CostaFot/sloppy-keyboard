package com.markedusduplicate.slopboard.suggestion.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionPromptTest {

    @Test
    fun `next-word reply parses a clean json array`() {
        val parsed = SuggestionPrompt.parse("""["the","quick","brown"]""")
        assertEquals(listOf("the", "quick", "brown"), parsed.words)
        assertNull(parsed.correction)
    }

    @Test
    fun `extracts the array from surrounding prose`() {
        assertEquals(
            listOf("a", "b", "c"),
            SuggestionPrompt.parse("""Sure! ["a", "b", "c"] hope that helps""").words,
        )
    }

    @Test
    fun `falls back to a loose comma list`() {
        assertEquals(listOf("cat", "dog", "bird"), SuggestionPrompt.parse("cat, dog, bird").words)
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

    @Test
    fun `keeps only the first word of a multi-word suggestion`() {
        val parsed = SuggestionPrompt.parse("""["going to","want to","the"]""")
        assertEquals(listOf("going", "want", "the"), parsed.words)
    }

    @Test
    fun `a multi-word fix is reduced to a single word`() {
        val parsed = SuggestionPrompt.parse("""{"fix":"the cat","words":[]}""", prefix = "teh")
        assertEquals("the", parsed.correction)
        assertEquals(listOf("the"), parsed.words)
    }

    @Test
    fun `object reply surfaces the fix first as the correction`() {
        val parsed = SuggestionPrompt.parse("""{"fix":"the","words":["then","they"]}""", prefix = "teh")
        assertEquals("the", parsed.correction)
        assertEquals(listOf("the", "then", "they"), parsed.words)
    }

    @Test
    fun `a fix equal to the typed word is not a correction`() {
        val parsed = SuggestionPrompt.parse("""{"fix":"the","words":["there","they"]}""", prefix = "the")
        assertNull(parsed.correction)
        assertEquals(listOf("there", "they"), parsed.words)
    }

    @Test
    fun `null fix yields completions only`() {
        val parsed =
            SuggestionPrompt.parse("""{"fix":null,"words":["android","androids"]}""", prefix = "andro")
        assertNull(parsed.correction)
        assertEquals(listOf("android", "androids"), parsed.words)
    }

    @Test
    fun `completions are recased to match an uppercase prefix`() {
        val parsed =
            SuggestionPrompt.parse("""{"fix":null,"words":["android","androids"]}""", prefix = "Andro")
        assertEquals(listOf("Android", "Androids"), parsed.words)
    }

    @Test
    fun `next-word prompt omits the personalization line when there are no hints`() {
        val prompt = SuggestionPrompt.build("what the ", "what the", prefix = "", hints = emptyList())
        assertFalse(prompt.contains("often follows"))
        assertTrue(prompt.contains("next word"))
        assertTrue(prompt.contains("what the "))
    }

    @Test
    fun `next-word prompt injects personalization hints when present`() {
        val prompt =
            SuggestionPrompt.build("what the ", "what the", prefix = "", hints = listOf("heck", "hell"))
        assertTrue(prompt.contains("""often follows "what the" with: heck, hell"""))
    }

    @Test
    fun `mid-word prompt asks to complete and spell-check the current word`() {
        val prompt =
            SuggestionPrompt.build("I am writing teh", "i am writing", prefix = "teh", hints = emptyList())
        assertTrue(prompt.contains("""Current word: "teh""""))
        assertTrue(prompt.contains("fix"))
    }
}
