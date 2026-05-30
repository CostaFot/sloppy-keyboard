package com.markedusduplicate.slopboard.keyboard.observe

import org.junit.Assert.assertEquals
import org.junit.Test

class TextContextTest {

    @Test
    fun `prediction context is the last three complete words lowercased`() {
        assertEquals("the quick brown", TextContext.predictionContext("oh The Quick Brown "))
    }

    @Test
    fun `prediction context drops an in-progress trailing word`() {
        // "fox" is still being typed, so the context is the words before it.
        assertEquals("quick brown", TextContext.predictionContext("quick brown fox"))
    }

    @Test
    fun `current prefix is empty on a boundary and the in-progress word otherwise`() {
        assertEquals("", TextContext.currentPrefix("what the "))
        assertEquals("he", TextContext.currentPrefix("what the he"))
    }

    @Test
    fun `finalized words exclude the in-progress word`() {
        assertEquals(listOf("what", "the"), TextContext.finalizedWords("what the he"))
        assertEquals(listOf("what", "the"), TextContext.finalizedWords("what the "))
    }

    @Test
    fun `context key keeps the last three words joined and lowercased`() {
        assertEquals("b what the", TextContext.contextKey(listOf("a", "b", "What", "THE")))
    }
}
