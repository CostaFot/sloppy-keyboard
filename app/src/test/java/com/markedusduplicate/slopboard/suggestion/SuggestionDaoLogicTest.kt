package com.markedusduplicate.slopboard.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Exercises the DAO's default insert-or-increment methods against the fake's Room-like semantics. */
class SuggestionDaoLogicTest {

    @Test
    fun `recordNgram inserts once then increments on repeat`() = runTest {
        val dao = FakeSuggestionDao()

        dao.recordNgram("what the", "heck")
        dao.recordNgram("what the", "heck")
        dao.recordNgram("what the", "heck")

        assertEquals(3, dao.ngrams.getValue("what the" to "heck"))
    }

    @Test
    fun `distinct words for the same context are separate rows`() = runTest {
        val dao = FakeSuggestionDao()

        dao.recordNgram("what the", "heck")
        dao.recordNgram("what the", "hell")

        assertEquals(1, dao.ngrams.getValue("what the" to "heck"))
        assertEquals(1, dao.ngrams.getValue("what the" to "hell"))
    }
}
