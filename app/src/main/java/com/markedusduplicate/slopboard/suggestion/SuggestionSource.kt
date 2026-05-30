package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.keyboard.observe.TextContext
import javax.inject.Inject

/**
 * The seam the LLM will eventually plug into: given the raw text before the cursor, return up to
 * three next-word suggestions. Today the only implementation is DB-backed; a future
 * `LlmSuggestionSource` (LiteRT-LM) implements the same contract.
 */
fun interface SuggestionSource {
    suspend fun suggest(textBeforeCursor: String): List<String>
}

/** Suggestions from the user's personal n-gram / accepted-chip history. */
class NgramSuggestionSource @Inject constructor(
    private val repository: PersonalizationRepository,
) : SuggestionSource {
    override suspend fun suggest(textBeforeCursor: String): List<String> {
        val context = TextContext.predictionContext(textBeforeCursor)
        val prefix = TextContext.currentPrefix(textBeforeCursor)
        return repository.suggest(context, prefix)
    }
}
