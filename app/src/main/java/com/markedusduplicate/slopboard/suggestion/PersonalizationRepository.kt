package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.data.db.SuggestionDao
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/write side of the personalization DB. Writes are insert-or-increment (in the DAO); reads
 * merge the user's most frequent next-words and previously-accepted chips for a given context,
 * optionally filtered by the word currently being typed.
 *
 * This repository is the DB boundary: every query/write hops to [DispatcherProvider.io] so callers
 * (the suggestion bar's main-thread view model included) never touch SQLite on the UI thread.
 */
@Singleton
class PersonalizationRepository @Inject constructor(
    private val dao: SuggestionDao,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun recordNgram(context: String, nextWord: String) =
        withContext(dispatcherProvider.io) {
            if (context.isBlank() || nextWord.isBlank()) return@withContext
            dao.recordNgram(context, nextWord)
        }

    suspend fun recordCorrection(original: String, replacement: String) =
        withContext(dispatcherProvider.io) {
            if (original.isBlank() || replacement.isBlank() || original == replacement) return@withContext
            dao.recordCorrection(original, replacement)
        }

    suspend fun recordAccepted(context: String, word: String) =
        withContext(dispatcherProvider.io) {
            if (word.isBlank()) return@withContext
            dao.recordAccepted(context, word)
        }

    /** Replacements the user has previously made for [original], most frequent first. */
    suspend fun learnedCorrections(original: String): List<String> =
        withContext(dispatcherProvider.io) {
            if (original.isBlank()) return@withContext emptyList()
            dao.topReplacements(original, FETCH_LIMIT)
        }

    /**
     * Up to [MAX_SUGGESTIONS] words for [context]: accepted chips first (a stronger signal than
     * passive typing), then frequent n-gram follow-ons. Deduped case-insensitively and, when the
     * user is mid-word, filtered to completions of [prefix].
     */
    suspend fun suggest(context: String, prefix: String): List<String> =
        withContext(dispatcherProvider.io) {
            if (context.isBlank()) return@withContext emptyList()
            val accepted = dao.topAccepted(context, FETCH_LIMIT)
            val ngrams = dao.topNextWords(context, FETCH_LIMIT)

            val seen = HashSet<String>()
            val out = ArrayList<String>(MAX_SUGGESTIONS)
            for (word in accepted + ngrams) {
                if (prefix.isNotEmpty() && !word.startsWith(prefix, ignoreCase = true)) continue
                if (word.equals(prefix, ignoreCase = true)) continue
                if (seen.add(word.lowercase())) {
                    out.add(word)
                    if (out.size == MAX_SUGGESTIONS) break
                }
            }
            out
        }

    companion object {
        const val MAX_SUGGESTIONS = 3
        private const val FETCH_LIMIT = 10
    }
}
