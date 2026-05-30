package com.markedusduplicate.slopboard.keyboard.observe

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.suggestion.FakeSuggestionDao
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservationManagerTest {

    private fun providerOf(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val io = dispatcher
        override val ui = dispatcher
        override val default = dispatcher
        override val unconfined = dispatcher
    }

    @Test
    fun `learns ngrams as words are finalized`() = runTest {
        val dao = FakeSuggestionDao()
        val tracker = InputContextTracker()
        val manager = ObservationManager(PersonalizationRepository(dao), tracker, providerOf(StandardTestDispatcher(testScheduler)))

        manager.onTextBeforeCursor("what ")
        manager.onTextBeforeCursor("what the ")
        manager.onTextBeforeCursor("what the heck ")
        advanceUntilIdle()

        assertEquals(1, dao.ngrams.getValue("what" to "the"))
        assertEquals(1, dao.ngrams.getValue("what the" to "heck"))
        // The very first word has no preceding context, so nothing is recorded for it.
        assertTrue(dao.ngrams.keys.none { it.first == "" })
    }

    @Test
    fun `records a correction when a finalized word is replaced`() = runTest {
        val dao = FakeSuggestionDao()
        val tracker = InputContextTracker()
        val manager = ObservationManager(PersonalizationRepository(dao), tracker, providerOf(StandardTestDispatcher(testScheduler)))

        manager.onTextBeforeCursor("the cat ")     // "cat" finalized after "the"
        manager.onTextBeforeCursor("the ")          // backspaced "cat"
        manager.onTextBeforeCursor("the dog ")      // typed "dog" instead
        advanceUntilIdle()

        assertEquals(1, dao.corrections.getValue("cat" to "dog"))
    }

    @Test
    fun `learns nothing in a password field`() = runTest {
        val dao = FakeSuggestionDao()
        val tracker = InputContextTracker().apply {
            onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            })
        }
        val manager = ObservationManager(PersonalizationRepository(dao), tracker, providerOf(StandardTestDispatcher(testScheduler)))

        manager.onTextBeforeCursor("hunter two ")
        advanceUntilIdle()

        assertTrue(dao.ngrams.isEmpty())
    }
}
