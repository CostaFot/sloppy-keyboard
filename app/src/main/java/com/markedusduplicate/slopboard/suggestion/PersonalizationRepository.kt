package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.data.db.SuggestionDao
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/write side of the personalization DB. Writes are insert-or-increment (in the DAO); reads
 * merge the user's most frequent next-words and previously-accepted chips for a given context,
 * optionally filtered by the word currently being typed.
 */
@Singleton
class PersonalizationRepository @Inject constructor(
    private val dao: SuggestionDao,
) {
    suspend fun recordNgram(context: String, nextWord: String) {
        if (context.isBlank() || nextWord.isBlank()) return
        dao.recordNgram(context, nextWord)
    }

    suspend fun recordCorrection(original: String, replacement: String) {
        if (original.isBlank() || replacement.isBlank() || original == replacement) return
        dao.recordCorrection(original, replacement)
    }

    suspend fun recordAccepted(context: String, word: String) {
        if (word.isBlank()) return
        dao.recordAccepted(context, word)
    }

    /**
     * Up to [MAX_SUGGESTIONS] words for [context]: accepted chips first (a stronger signal than
     * passive typing), then frequent n-gram follow-ons. Deduped case-insensitively and, when the
     * user is mid-word, filtered to completions of [prefix].
     */
    suspend fun suggest(context: String, prefix: String): List<String> {
        if (context.isBlank()) return emptyList()
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
        return out
    }

    companion object {
        const val MAX_SUGGESTIONS = 3
        private const val FETCH_LIMIT = 10
    }
}
