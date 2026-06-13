package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.CoroutinesTestRule
import com.markedusduplicate.slopboard.suggestion.dictionary.Dictionary
import com.markedusduplicate.slopboard.suggestion.dictionary.WordIndex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionarySuggestionSourceTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule(eager = true)

    private val index = WordIndex(
        mapOf(
            "android" to 50L,
            "androids" to 10L,
            "the" to 100L,
            "there" to 80L,
            "they" to 90L,
        ),
    )
    private val dictionary = Dictionary { index }

    private fun source(dao: FakeSuggestionDao) = DictionarySuggestionSource(
        dictionary,
        PersonalizationRepository(dao, coroutinesTestRule.testDispatcherProvider),
        coroutinesTestRule.testDispatcherProvider,
    )

    @Test
    fun `mid-word offers completions and no correction`() = runTest {
        val result = source(FakeSuggestionDao()).suggest("Andro")
        assertEquals(listOf("Android", "Androids"), result.words)
        assertNull(result.correction)
    }

    @Test
    fun `a prefix that completes to nothing is corrected`() = runTest {
        val result = source(FakeSuggestionDao()).suggest("teh")
        assertEquals("the", result.correction)
        assertEquals("the", result.words.first())
    }

    @Test
    fun `a learned correction wins over the dictionary guess`() = runTest {
        val dao = FakeSuggestionDao()
        dao.corrections["teh" to "tha"] = 5
        val result = source(dao).suggest("teh")
        assertEquals("tha", result.correction)
    }

    @Test
    fun `on a boundary it delegates to personal next-word n-grams`() = runTest {
        val dao = FakeSuggestionDao()
        dao.ngrams["hello" to "world"] = 3
        dao.ngrams["hello" to "there"] = 1
        val result = source(dao).suggest("hello ")
        assertEquals(listOf("world", "there"), result.words)
        assertNull(result.correction)
    }
}
