package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.keyboard.observe.TextContext
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import com.markedusduplicate.slopboard.suggestion.dictionary.Dictionary
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The instant chip source, served from the bundled dictionary plus the user's personal history.
 *
 * - **Mid-word** (a prefix is being typed): completions of the word, with the user's own matching
 *   words ranked ahead of dictionary words. A spelling [Suggestions.correction] is offered only when
 *   the prefix completes to nothing (the signal it's a typo, not an unfinished word) — a learned
 *   correction wins over the dictionary's edit-distance guess.
 * - **On a boundary** (no prefix): the user's personal next-word n-grams (a plain dictionary can't
 *   predict the next word — that's the LLM's job).
 *
 * Returns [Suggestions.EMPTY] until the dictionary has finished loading.
 */
class DictionarySuggestionSource @Inject constructor(
    private val dictionary: Dictionary,
    private val personalization: PersonalizationRepository,
    private val dispatcherProvider: DispatcherProvider,
) : SuggestionSource {

    override suspend fun suggest(textBeforeCursor: String): Suggestions =
        withContext(dispatcherProvider.io) {
            val prefix = TextContext.currentPrefix(textBeforeCursor)
            val context = TextContext.predictionContext(textBeforeCursor)
            if (prefix.isEmpty()) {
                if (context.isBlank()) return@withContext Suggestions.EMPTY
                Suggestions(personalization.suggest(context, ""))
            } else {
                val index = dictionary.indexOrNull() ?: return@withContext Suggestions.EMPTY
                val personal = personalization.suggest(context, prefix)
                val completions = index.complete(prefix, MAX_SUGGESTIONS)
                // Only treat the word as a typo when it completes to nothing the user/dictionary knows.
                val correction = if (personal.isEmpty() && completions.isEmpty()) {
                    personalization.learnedCorrections(prefix).firstOrNull()
                        ?: index.correct(prefix, limit = 1).firstOrNull()
                } else {
                    null
                }
                assemble(prefix, correction, personal + completions)
            }
        }

    /** Correction first (when meaningful), then completions; deduped case-insensitively, capped. */
    private fun assemble(prefix: String, correction: String?, completions: List<String>): Suggestions {
        val seen = HashSet<String>()
        val words = ArrayList<String>(MAX_SUGGESTIONS)
        val fix = correction
            ?.let { matchCase(it, prefix) }
            ?.takeIf { it.isNotEmpty() && !it.equals(prefix, ignoreCase = true) }

        fun add(word: String) {
            if (word.isEmpty() || words.size == MAX_SUGGESTIONS) return
            if (seen.add(word.lowercase())) words.add(word)
        }

        fix?.let(::add)
        completions.forEach(::add)
        return Suggestions(words, correction = fix?.takeIf { words.contains(it) })
    }
}
