package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.slopboard.di.DictionarySuggestions
import com.markedusduplicate.slopboard.di.LlmSuggestions
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import com.markedusduplicate.slopboard.keyboard.observe.TextContext
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives a single suggestion row whose content depends on the typing **mode**:
 *
 * - **[SuggestionMode.TYPING]** (a word is in progress): the instant dictionary's completions +
 *   spelling fix. The LLM is not consulted mid-word.
 * - **[SuggestionMode.NEXT_WORD]** (just after a space): the user's personal next-word n-grams for
 *   the first slots, and the LLM's single next-best word reserved for the last slot.
 *
 * A single debounced input is shared. The dictionary side carries its mode + suggestions atomically
 * (so mode and content never disagree); the LLM side is independent — it clears while computing and
 * fills when inference returns, so it never holds up or desynchronises the instant slots.
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

    /** Active flag, mode, and dictionary suggestions, computed together so they stay consistent. */
    private val dictionaryRow: StateFlow<SuggestionState> =
        input.mapLatest { text -> baseState(text) }
            .stateIn(scope, SharingStarted.Eagerly, SuggestionState.EMPTY)

    /** The LLM's next word — only on a boundary, cleared to empty (shows "TBD") while it computes. */
    private val llmRow: StateFlow<Suggestions> =
        input.flatMapLatest { text ->
            flow {
                emit(Suggestions.EMPTY)
                if (text != null && isNextWord(text)) emit(llmSource.suggest(text))
            }
        }
            .stateIn(scope, SharingStarted.Eagerly, Suggestions.EMPTY)

    val state: StateFlow<SuggestionState> =
        combine(dictionaryRow, llmRow) { base, llm ->
            if (base.active && base.mode == SuggestionMode.NEXT_WORD) {
                base.copy(llmNextWord = llm.words.firstOrNull())
            } else {
                base
            }
        }
            .stateIn(scope, SharingStarted.Eagerly, SuggestionState.EMPTY)

    private suspend fun baseState(text: String?): SuggestionState {
        if (text.isNullOrEmpty()) return SuggestionState.EMPTY
        val mode = if (isNextWord(text)) SuggestionMode.NEXT_WORD else SuggestionMode.TYPING
        return SuggestionState(
            active = true,
            mode = mode,
            dictionary = dictionarySource.suggest(text),
            llmNextWord = null,
        )
    }

    private fun isNextWord(text: String): Boolean =
        TextContext.currentPrefix(text).isEmpty() && TextContext.predictionContext(text).isNotBlank()

    companion object {
        const val DEBOUNCE_MS = 300L
    }
}

/** One source's chips: [words] plus an optional spelling [correction] to highlight. */
data class Suggestions(val words: List<String>, val correction: String? = null) {
    companion object {
        val EMPTY = Suggestions(emptyList())
    }
}

enum class SuggestionMode { TYPING, NEXT_WORD }

/**
 * The single suggestion row. [active] is true while typing (else the tools toolbar shows). In
 * [SuggestionMode.TYPING] the row is [dictionary]'s completions/fix; in [SuggestionMode.NEXT_WORD]
 * the first slots come from [dictionary] (personal n-grams) and the last from [llmNextWord].
 */
data class SuggestionState(
    val active: Boolean,
    val mode: SuggestionMode,
    val dictionary: Suggestions,
    val llmNextWord: String?,
) {
    companion object {
        val EMPTY = SuggestionState(
            active = false,
            mode = SuggestionMode.TYPING,
            dictionary = Suggestions.EMPTY,
            llmNextWord = null,
        )
    }
}
