package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.CoroutinesTestRule
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionCoordinatorTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule(eager = false)

    private val noLlm = SuggestionSource { emptyList() }

    private fun pastDebounce() = SuggestionCoordinator.DEBOUNCE_MS + 1

    @Test
    fun `query is debounced and reflects the latest input`() = runTest {
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val db = SuggestionSource { text -> calls.add(text); listOf("x") }
        val coordinator = SuggestionCoordinator(tracker, db, noLlm, backgroundScope)

        tracker.updateText("hello ")
        advanceTimeBy(200)
        runCurrent()
        assertTrue("should not query before the debounce window elapses", calls.isEmpty())

        advanceTimeBy(150)
        runCurrent()

        assertEquals(listOf("hello "), calls)
        assertEquals(listOf("x"), coordinator.suggestions.value)
    }

    @Test
    fun `new input after settling triggers a fresh query`() = runTest {
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val db = SuggestionSource { text -> calls.add(text); listOf(text.trim()) }
        val coordinator = SuggestionCoordinator(tracker, db, noLlm, backgroundScope)

        tracker.updateText("foo ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        tracker.updateText("foo bar ")
        advanceTimeBy(pastDebounce())
        runCurrent()

        assertEquals(listOf("foo ", "foo bar "), calls)
        assertEquals(listOf("foo bar"), coordinator.suggestions.value)
    }

    @Test
    fun `db suggestions show first, then the llm refines them`() = runTest {
        val tracker = InputContextTracker()
        val db = SuggestionSource { listOf("db") }
        val llm = SuggestionSource { delay(100); listOf("llm") }
        val coordinator = SuggestionCoordinator(tracker, db, llm, backgroundScope)

        tracker.updateText("hi ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        assertEquals(listOf("db"), coordinator.suggestions.value)

        advanceTimeBy(101)
        runCurrent()
        assertEquals(listOf("llm"), coordinator.suggestions.value)
    }
}
