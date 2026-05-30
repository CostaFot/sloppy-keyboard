package com.markedusduplicate.slopboard.keyboard.observe

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.markedusduplicate.slopboard.CoroutinesTestRule
import com.markedusduplicate.slopboard.suggestion.FakeSuggestionDao
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservationManagerTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule(eager = false)

    private fun repository(dao: FakeSuggestionDao) =
        PersonalizationRepository(dao, coroutinesTestRule.testDispatcherProvider)

    @Test
    fun `learns ngrams as words are finalized`() = runTest {
        val dao = FakeSuggestionDao()
        val manager = ObservationManager(repository(dao), InputContextTracker(), this)

        manager.onTextBeforeCursor("what ")
        manager.onTextBeforeCursor("what the ")
        manager.onTextBeforeCursor("what the heck ")
        advanceUntilIdle()

        assertEquals(1, dao.ngrams.getValue("what" to "the"))
        assertEquals(1, dao.ngrams.getValue("what the" to "heck"))
        assertTrue(dao.ngrams.keys.none { it.first == "" })
    }

    @Test
    fun `records a correction when a finalized word is replaced`() = runTest {
        val dao = FakeSuggestionDao()
        val manager = ObservationManager(repository(dao), InputContextTracker(), this)

        manager.onTextBeforeCursor("the cat ")
        manager.onTextBeforeCursor("the ")
        manager.onTextBeforeCursor("the dog ")

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
        val manager = ObservationManager(repository(dao), tracker, this)

        manager.onTextBeforeCursor("hunter two ")

        assertTrue(dao.ngrams.isEmpty())
    }
}
