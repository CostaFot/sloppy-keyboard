package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.CoroutinesTestRule
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionCoordinatorTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule(eager = false)

    private val noLlm = SuggestionSource { Suggestions.EMPTY }

    private fun pastDebounce() = SuggestionCoordinator.DEBOUNCE_MS + 1

    @Test
    fun `dictionary row is debounced and reflects the latest input`() = runTest {
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val dictionary = SuggestionSource { text -> calls.add(text); Suggestions(listOf("x")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, noLlm, backgroundScope)

        tracker.updateText("hello ")
        advanceTimeBy(200)
        runCurrent()
        assertTrue("should not query before the debounce window elapses", calls.isEmpty())

        advanceTimeBy(150)
        runCurrent()

        assertEquals(listOf("hello "), calls)
        assertEquals(Suggestions(listOf("x")), coordinator.state.value.dictionary)
        assertTrue(coordinator.state.value.active)
    }

    @Test
    fun `empty input is inactive`() = runTest {
        val tracker = InputContextTracker()
        val coordinator =
            SuggestionCoordinator(tracker, SuggestionSource { Suggestions(listOf("x")) }, noLlm, backgroundScope)

        advanceTimeBy(pastDebounce())
        runCurrent()

        assertFalse(coordinator.state.value.active)
    }

    @Test
    fun `mid-word query runs without waiting for a word boundary`() = runTest {
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val dictionary = SuggestionSource { text -> calls.add(text); Suggestions(listOf("Android")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, noLlm, backgroundScope)

        tracker.updateText("Andro")
        advanceTimeBy(pastDebounce())
        runCurrent()

        assertEquals(listOf("Andro"), calls)
        assertEquals(Suggestions(listOf("Android")), coordinator.state.value.dictionary)
    }

    @Test
    fun `dictionary row shows at once while the llm row arrives later`() = runTest {
        val tracker = InputContextTracker()
        val dictionary = SuggestionSource { Suggestions(listOf("db")) }
        val llm = SuggestionSource { delay(100); Suggestions(listOf("llm")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, llm, backgroundScope)

        tracker.updateText("hi ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        assertEquals(Suggestions(listOf("db")), coordinator.state.value.dictionary)
        assertEquals(Suggestions.EMPTY, coordinator.state.value.llm)

        advanceTimeBy(101)
        runCurrent()
        assertEquals(Suggestions(listOf("db")), coordinator.state.value.dictionary)
        assertEquals(Suggestions(listOf("llm")), coordinator.state.value.llm)
    }

    @Test
    fun `llm row keeps its previous value while the dictionary updates`() = runTest {
        val tracker = InputContextTracker()
        val dictionary = SuggestionSource { text -> Suggestions(listOf(text.trim())) }
        val llm = SuggestionSource { text -> delay(100); Suggestions(listOf("llm:${text.trim()}")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, llm, backgroundScope)

        tracker.updateText("a ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        advanceTimeBy(101)
        runCurrent()
        assertEquals(Suggestions(listOf("llm:a")), coordinator.state.value.llm)

        tracker.updateText("a b ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        assertEquals(Suggestions(listOf("a b")), coordinator.state.value.dictionary)
        assertEquals(Suggestions(listOf("llm:a")), coordinator.state.value.llm)

        advanceTimeBy(101)
        runCurrent()
        assertEquals(Suggestions(listOf("llm:a b")), coordinator.state.value.llm)
    }
}
