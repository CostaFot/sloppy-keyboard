package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.slopboard.di.DictionarySuggestions
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the live input context to the suggestion bar as two **independent** rows so neither one
 * makes the other jump:
 *
 * - the instant dictionary row updates on every debounced keystroke, and
 * - the slow LLM row updates only when its inference returns.
 *
 * A single debounced input is shared; each row is its own [mapLatest] (so it cancels only its own
 * stale work) and they are [combine]d. Because `combine` retains each side's last value, the
 * dictionary row refreshes immediately while the LLM row keeps its previous result until new
 * inference completes — no per-keystroke blanking, and the slow side never repaints the fast one.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Singleton
class SuggestionCoordinator @Inject constructor(
    tracker: InputContextTracker,
    @DictionarySuggestions private val dictionarySource: SuggestionSource,
    @LlmSuggestions private val llmSource: SuggestionSource,
    @ApplicationCoroutineScope scope: CoroutineScope,
) {
    private val input: Flow<String?> =
        combine(tracker.textBeforeCursor, tracker.allowed) { text, allowed ->
            if (allowed) text else null
        }
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    // Each row is its own StateFlow (initial EMPTY) so they update independently: the dictionary row
    // refreshes at once while the slow LLM row holds its previous value until new inference completes.
    private val dictionaryRow: StateFlow<Suggestions> =
        input.mapLatest { rowFor(dictionarySource, it) }
            .stateIn(scope, SharingStarted.Eagerly, Suggestions.EMPTY)

    private val llmRow: StateFlow<Suggestions> =
        input.mapLatest { rowFor(llmSource, it) }
            .stateIn(scope, SharingStarted.Eagerly, Suggestions.EMPTY)

    private val active: StateFlow<Boolean> =
        input.map { !it.isNullOrEmpty() }
            .stateIn(scope, SharingStarted.Eagerly, false)

    val state: StateFlow<SuggestionState> =
        combine(active, dictionaryRow, llmRow) { active, dictionary, llm ->
            SuggestionState(active = active, dictionary = dictionary, llm = llm)
        }
            .stateIn(scope, SharingStarted.Eagerly, SuggestionState.EMPTY)

    private suspend fun rowFor(source: SuggestionSource, text: String?): Suggestions =
        if (text == null) Suggestions.EMPTY else source.suggest(text)

    companion object {
        const val DEBOUNCE_MS = 300L
    }
}

/**
 * One row of chips. [correction] is the chip that is a spelling fix for the word being typed (the
 * row highlights its "best" word — the correction, else the first); `null` otherwise.
 */
data class Suggestions(val words: List<String>, val correction: String? = null) {
    companion object {
        val EMPTY = Suggestions(emptyList())
    }
}

/**
 * The whole suggestion area: the instant [dictionary] row and the [llm] row. [active] is true while
 * the user is typing (there is text before the cursor), so the bar shows the rows rather than the
 * idle tools toolbar.
 */
data class SuggestionState(
    val active: Boolean,
    val dictionary: Suggestions,
    val llm: Suggestions,
) {
    companion object {
        val EMPTY = SuggestionState(active = false, Suggestions.EMPTY, Suggestions.EMPTY)
    }
}
