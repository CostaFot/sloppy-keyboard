package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the live input context to the suggestion bar: debounces typing, cancels any in-flight
 * query when new input arrives ([mapLatest]), and surfaces the result as a [StateFlow] the UI
 * collects. Emits an empty list when suggestions aren't allowed (e.g. password fields).
 */
@Singleton
class SuggestionCoordinator @Inject constructor(
    tracker: InputContextTracker,
    source: SuggestionSource,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(dispatcherProvider.default + SupervisorJob())

    val suggestions: StateFlow<List<String>> =
        combine(tracker.textBeforeCursor, tracker.allowed) { text, allowed ->
            if (allowed) text else null
        }
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .mapLatest { text -> if (text == null) emptyList() else source.suggest(text) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    companion object {
        const val DEBOUNCE_MS = 300L
    }
}
