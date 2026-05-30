package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.slopboard.di.DbSuggestions
import com.markedusduplicate.slopboard.di.LlmSuggestions
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the live input context to the suggestion bar. Per debounced keystroke it emits the
 * instant n-gram suggestions first, then replaces them with the LiteRT-LM suggestions once
 * inference returns. [flatMapLatest] cancels any in-flight LLM call when new input arrives, and the
 * result is surfaced as a [StateFlow] the UI collects. Emits an empty list when suggestions aren't
 * allowed (e.g. password fields).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Singleton
class SuggestionCoordinator @Inject constructor(
    tracker: InputContextTracker,
    @DbSuggestions private val dbSource: SuggestionSource,
    @LlmSuggestions private val llmSource: SuggestionSource,
    @ApplicationCoroutineScope scope: CoroutineScope,
) {
    val suggestions: StateFlow<List<String>> =
        combine(tracker.textBeforeCursor, tracker.allowed) { text, allowed ->
            if (allowed) text else null
        }
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { text -> suggestionsFor(text) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun suggestionsFor(text: String?): Flow<List<String>> {
        if (text == null) return flowOf(emptyList())
        return flow {
            val instant = dbSource.suggest(text)
            emit(instant)
            val refined = llmSource.suggest(text)
            if (refined.isNotEmpty() && refined != instant) emit(refined)
        }
    }

    companion object {
        const val DEBOUNCE_MS = 300L
    }
}
