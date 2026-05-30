package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.CoroutinesTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizationRepositoryTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule(eager = true)

    private fun repo(dao: FakeSuggestionDao) =
        PersonalizationRepository(dao, coroutinesTestRule.testDispatcherProvider)

    @Test
    fun `accepted chips rank above ngrams and results are capped at three`() =
        runTest {
            val dao = FakeSuggestionDao()
            dao.ngrams["hello" to "world"] = 3
            dao.ngrams["hello" to "there"] = 2
            dao.ngrams["hello" to "friend"] = 1
            dao.ngrams["hello" to "pal"] = 1
            dao.accepted["hello" to "stranger"] = 1

            assertEquals(listOf("stranger", "world", "there"), repo(dao).suggest("hello", prefix = ""))
        }

    @Test
    fun `duplicates across accepted and ngrams are deduped case-insensitively`() =
        runTest {
            val dao = FakeSuggestionDao()
            dao.accepted["hi" to "World"] = 1
            dao.ngrams["hi" to "world"] = 5
            dao.ngrams["hi" to "there"] = 4

            assertEquals(listOf("World", "there"), repo(dao).suggest("hi", prefix = ""))
        }

    @Test
    fun `prefix filters to completions and excludes the prefix itself`() =
        runTest {
            val dao = FakeSuggestionDao()
            dao.ngrams["what the" to "heck"] = 3
            dao.ngrams["what the" to "hello"] = 2
            dao.ngrams["what the" to "world"] = 5
            dao.ngrams["what the" to "he"] = 9

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
