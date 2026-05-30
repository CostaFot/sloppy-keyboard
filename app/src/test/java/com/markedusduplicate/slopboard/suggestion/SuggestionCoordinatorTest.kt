package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.CoroutinesTestRule
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionCoordinatorTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun `query is debounced and reflects the latest input`() =
        runTest {
            val tracker = InputContextTracker()
            val calls = mutableListOf<String>()
            val source = SuggestionSource { text -> calls.add(text); listOf("x") }
            val coordinator =
                SuggestionCoordinator(tracker, source, coroutinesTestRule.testDispatcherProvider)

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
    fun `new input after settling triggers a fresh query`() =
        runTest {
            val tracker = InputContextTracker()
            val calls = mutableListOf<String>()
            val source = SuggestionSource { text -> calls.add(text); listOf(text.trim()) }
            val coordinator =
                SuggestionCoordinator(tracker, source, coroutinesTestRule.testDispatcherProvider)

            tracker.updateText("foo ")
            advanceUntilIdle()
            tracker.updateText("foo bar ")
            advanceUntilIdle()

            assertEquals(listOf("foo ", "foo bar "), calls)
            assertEquals(listOf("foo bar"), coordinator.suggestions.value)
        }
}
