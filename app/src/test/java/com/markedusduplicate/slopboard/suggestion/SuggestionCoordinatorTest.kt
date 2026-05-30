package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionCoordinatorTest {

    private fun providerOf(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val io = dispatcher
        override val ui = dispatcher
        override val default = dispatcher
        override val unconfined = dispatcher
    }

    @Test
    fun `query is debounced and reflects the latest input`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val source = SuggestionSource { text -> calls.add(text); listOf("x") }
        val coordinator = SuggestionCoordinator(tracker, source, providerOf(dispatcher))

        tracker.updateText("hello ")
        advanceTimeBy(200)
        runCurrent()
        assertTrue("should not query before the debounce window elapses", calls.isEmpty())

        advanceTimeBy(150) // total 350ms > 300ms debounce
        runCurrent()

        assertEquals(listOf("hello "), calls)
        assertEquals(listOf("x"), coordinator.suggestions.value)
    }

    @Test
    fun `new input after settling triggers a fresh query`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = InputContextTracker()
        val calls = mutableListOf<String>()
        val source = SuggestionSource { text -> calls.add(text); listOf(text.trim()) }
        val coordinator = SuggestionCoordinator(tracker, source, providerOf(dispatcher))

        tracker.updateText("foo ")
        advanceUntilIdle()
        tracker.updateText("foo bar ")
        advanceUntilIdle()

        assertEquals(listOf("foo ", "foo bar "), calls)
        assertEquals(listOf("foo bar"), coordinator.suggestions.value)
    }
}
