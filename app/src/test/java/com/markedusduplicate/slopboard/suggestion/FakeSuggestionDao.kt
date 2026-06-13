package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.slopboard.data.db.AcceptedSuggestion
import com.markedusduplicate.slopboard.data.db.NgramEntry
import com.markedusduplicate.slopboard.data.db.SuggestionDao
import com.markedusduplicate.slopboard.data.db.UserCorrection

/**
 * In-memory [SuggestionDao] that mimics Room's unique-key semantics: insert returns -1 when the
 * composite key already exists (so the default `record*` methods fall through to a count bump).
 */
class FakeSuggestionDao : SuggestionDao {

    val ngrams = linkedMapOf<Pair<String, String>, Int>()
    val accepted = linkedMapOf<Pair<String, String>, Int>()
    val corrections = linkedMapOf<Pair<String, String>, Int>()

    override suspend fun insertNgramIgnore(entry: NgramEntry): Long =
        insertIgnore(ngrams, entry.context to entry.nextWord)

    override suspend fun bumpNgram(context: String, nextWord: String) {
        ngrams[context to nextWord] = ngrams.getValue(context to nextWord) + 1
    }

    override suspend fun topNextWords(context: String, limit: Int): List<String> =
        top(ngrams, context, limit)

    override suspend fun insertCorrectionIgnore(entry: UserCorrection): Long =
        insertIgnore(corrections, entry.original to entry.replacement)

    override suspend fun bumpCorrection(original: String, replacement: String) {
        corrections[original to replacement] = corrections.getValue(original to replacement) + 1
    }

    override suspend fun topReplacements(original: String, limit: Int): List<String> =
        top(corrections, original, limit)

    override suspend fun insertAcceptedIgnore(entry: AcceptedSuggestion): Long =
        insertIgnore(accepted, entry.context to entry.acceptedWord)

    override suspend fun bumpAccepted(context: String, acceptedWord: String) {
        accepted[context to acceptedWord] = accepted.getValue(context to acceptedWord) + 1
    }

    override suspend fun topAccepted(context: String, limit: Int): List<String> =
        top(accepted, context, limit)

    private fun insertIgnore(map: MutableMap<Pair<String, String>, Int>, key: Pair<String, String>): Long {
        if (map.containsKey(key)) return -1L
        map[key] = 1
        return 1L
    }

    private fun top(map: Map<Pair<String, String>, Int>, context: String, limit: Int): List<String> =
        map.entries
            .filter { it.key.first == context }
            .sortedWith(compareByDescending<Map.Entry<Pair<String, String>, Int>> { it.value }
                .thenBy { it.key.second })
            .take(limit)
            .map { it.key.second }
}
