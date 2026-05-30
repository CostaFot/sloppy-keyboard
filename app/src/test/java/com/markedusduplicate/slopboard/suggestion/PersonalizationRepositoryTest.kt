package com.markedusduplicate.slopboard.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationRepositoryTest {

    private fun repo(dao: FakeSuggestionDao) = PersonalizationRepository(dao)

    @Test
    fun `accepted chips rank above ngrams and results are capped at three`() = runTest {
        val dao = FakeSuggestionDao()
        // ngrams: world(3), there(2), friend(1), pal(1)
        dao.ngrams["hello" to "world"] = 3
        dao.ngrams["hello" to "there"] = 2
        dao.ngrams["hello" to "friend"] = 1
        dao.ngrams["hello" to "pal"] = 1
        // accepted: stranger(1) — weaker count but stronger signal, so it leads
        dao.accepted["hello" to "stranger"] = 1

        assertEquals(listOf("stranger", "world", "there"), repo(dao).suggest("hello", prefix = ""))
    }

    @Test
    fun `duplicates across accepted and ngrams are deduped case-insensitively`() = runTest {
        val dao = FakeSuggestionDao()
        dao.accepted["hi" to "World"] = 1
        dao.ngrams["hi" to "world"] = 5
        dao.ngrams["hi" to "there"] = 4

        assertEquals(listOf("World", "there"), repo(dao).suggest("hi", prefix = ""))
    }

    @Test
    fun `prefix filters to completions and excludes the prefix itself`() = runTest {
        val dao = FakeSuggestionDao()
        dao.ngrams["what the" to "heck"] = 3
        dao.ngrams["what the" to "hello"] = 2
        dao.ngrams["what the" to "world"] = 5
        dao.ngrams["what the" to "he"] = 9

        // Only words starting with "he", and not the literal "he" the user already typed.
        assertEquals(listOf("heck", "hello"), repo(dao).suggest("what the", prefix = "he"))
    }

    @Test
    fun `blank context yields nothing`() = runTest {
        val dao = FakeSuggestionDao()
        dao.ngrams["" to "x"] = 9
        assertTrue(repo(dao).suggest("", prefix = "").isEmpty())
    }

    @Test
    fun `record helpers ignore blank input`() = runTest {
        val dao = FakeSuggestionDao()
        val repo = repo(dao)
        repo.recordNgram("ctx", " ")
        repo.recordCorrection("same", "same")
        assertTrue(dao.ngrams.isEmpty())
        assertTrue(dao.corrections.isEmpty())
    }
}
