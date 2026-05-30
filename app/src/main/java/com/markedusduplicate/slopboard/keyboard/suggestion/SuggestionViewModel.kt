package com.markedusduplicate.slopboard.keyboard.suggestion

import com.markedusduplicate.slopboard.keyboard.KeyboardHandler
import com.markedusduplicate.slopboard.keyboard.KeyboardMessage
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import com.markedusduplicate.slopboard.keyboard.observe.TextContext
import com.markedusduplicate.slopboard.retain.RetainedViewModel
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository
import com.markedusduplicate.slopboard.suggestion.SuggestionCoordinator
import com.markedusduplicate.slopboard.suggestion.Suggestions
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SuggestionViewModelEntryPoint {
    fun suggestionViewModel(): SuggestionViewModel
}

class SuggestionViewModel @Inject constructor(
    coordinator: SuggestionCoordinator,
    private val tracker: InputContextTracker,
    private val repository: PersonalizationRepository,
    private val keyboardHandler: KeyboardHandler,
) : RetainedViewModel() {

    val suggestions: StateFlow<Suggestions> = coordinator.suggestions

    /** Insert the chip (replacing any in-progress word) and log it as an accepted suggestion. */
    fun onAccept(word: String) {
        val before = tracker.textBeforeCursor.value
        val context = TextContext.predictionContext(before)
        val prefix = TextContext.currentPrefix(before)
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.CommitSuggestion(word, prefix.length))
            repository.recordAccepted(context, word)
        }
    }
}
