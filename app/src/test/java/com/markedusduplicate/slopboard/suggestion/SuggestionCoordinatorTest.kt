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
import org.junit.Assert.assertNull
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
    fun `empty input is inactive`() = runTest {
        val tracker = InputContextTracker()
        val coordinator =
            SuggestionCoordinator(tracker, SuggestionSource { Suggestions(listOf("x")) }, noLlm, backgroundScope)

        advanceTimeBy(pastDebounce())
        runCurrent()

        assertFalse(coordinator.state.value.active)
    }

    @Test
    fun `mid-word shows the dictionary and never calls the llm`() = runTest {
        val tracker = InputContextTracker()
        val llmCalls = mutableListOf<String>()
        val dictionary = SuggestionSource { Suggestions(listOf("Android", "androids")) }
        val llm = SuggestionSource { text -> llmCalls.add(text); Suggestions(listOf("x")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, llm, backgroundScope)

        tracker.updateText("Andro")
        advanceTimeBy(pastDebounce())
        runCurrent()

        val state = coordinator.state.value
        assertEquals(SuggestionMode.TYPING, state.mode)
        assertEquals(listOf("Android", "androids"), state.dictionary.words)
        assertNull(state.llmNextWord)
        assertTrue("the LLM must not run mid-word", llmCalls.isEmpty())
    }

    @Test
    fun `after a space the db fills the first slots and the llm the last`() = runTest {
        val tracker = InputContextTracker()
        val llmCalls = mutableListOf<String>()
        val dictionary = SuggestionSource { Suggestions(listOf("world", "there")) }
        val llm = SuggestionSource { text -> llmCalls.add(text); Suggestions(listOf("planet")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, llm, backgroundScope)

        tracker.updateText("hello ")
        advanceTimeBy(pastDebounce())
        runCurrent()

        val state = coordinator.state.value
        assertEquals(SuggestionMode.NEXT_WORD, state.mode)
        assertEquals(listOf("world", "there"), state.dictionary.words)
        assertEquals("planet", state.llmNextWord)
        assertEquals(listOf("hello "), llmCalls)
    }

    @Test
    fun `db next-words resolve before the delayed llm word`() = runTest {
        val tracker = InputContextTracker()
        val dictionary = SuggestionSource { Suggestions(listOf("world", "there")) }
        val llm = SuggestionSource { delay(100); Suggestions(listOf("planet")) }
        val coordinator = SuggestionCoordinator(tracker, dictionary, llm, backgroundScope)

        tracker.updateText("hello ")
        advanceTimeBy(pastDebounce())
        runCurrent()
        assertEquals(listOf("world", "there"), coordinator.state.value.dictionary.words)
        assertNull("llm slot shows the placeholder until inference returns", coordinator.state.value.llmNextWord)

        advanceTimeBy(101)
        runCurrent()
        assertEquals("planet", coordinator.state.value.llmNextWord)
        assertEquals(listOf("world", "there"), coordinator.state.value.dictionary.words)
    }
}
