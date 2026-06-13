package com.markedusduplicate.slopboard.suggestion.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordIndexTest {

    private val index = WordIndex(
        mapOf(
            "the" to 100L,
            "there" to 80L,
            "their" to 60L,
            "they" to 90L,
            "android" to 50L,
            "androids" to 10L,
            "andromeda" to 5L,
            "receive" to 70L,
        ),
    )

    @Test
    fun `completion ranks by frequency and excludes the exact word`() {
        assertEquals(listOf("they", "there", "their"), index.complete("the", limit = 3))
    }

    @Test
    fun `completion follows the prefix casing`() {
        assertEquals(listOf("Android", "Androids"), index.complete("Andro", limit = 2))
    }

    @Test
    fun `a known word yields no correction`() {
        assertTrue(index.correct("the", limit = 3).isEmpty())
    }

    @Test
    fun `corrects a one-edit typo`() {
        assertEquals("receive", index.correct("recieve", limit = 1).firstOrNull())
    }

    @Test
    fun `corrects a transposition`() {
        assertEquals("the", index.correct("teh", limit = 1).firstOrNull())
    }

    @Test
    fun `correction follows the prefix casing`() {
        assertEquals("The", index.correct("Teh", limit = 1).firstOrNull())
    }

    @Test
    fun `far-apart words are not offered as corrections`() {
        assertTrue(index.correct("xyzzy", limit = 3).isEmpty())
    }

    @Test
    fun `top words are ranked by frequency`() {
        assertEquals(listOf("the", "they", "there"), index.topWords(limit = 3))
    }
}
