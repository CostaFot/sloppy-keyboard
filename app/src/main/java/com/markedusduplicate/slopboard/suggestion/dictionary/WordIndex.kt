package com.markedusduplicate.slopboard.suggestion.dictionary

import com.markedusduplicate.slopboard.suggestion.matchCase
import kotlin.math.abs
import kotlin.math.min

/**
 * Pure, in-memory dictionary over a `word -> frequency` table. No Android types, so it unit-tests
 * without a `Context`. Words are stored lowercase; results are recased to follow the typed prefix.
 *
 * - [complete] returns frequency-ranked words that start with the prefix.
 * - [correct] returns frequency-ranked dictionary words within a small edit distance of a word that
 *   isn't itself in the dictionary (i.e. a likely typo). Candidates are restricted to the same
 *   first letter and a similar length to keep per-keystroke cost low — first-letter typos are not
 *   corrected, which is an accepted trade-off.
 */
class WordIndex(private val frequencies: Map<String, Long>) {

    /** Words grouped by first char, sorted alphabetically for prefix scans. */
    private val byFirstChar: Map<Char, List<String>> =
        frequencies.keys.groupBy { it.first() }.mapValues { (_, words) -> words.sorted() }

    fun contains(word: String): Boolean = frequencies.containsKey(word.lowercase())

    fun complete(prefix: String, limit: Int): List<String> {
        if (prefix.isEmpty() || limit <= 0) return emptyList()
        val p = prefix.lowercase()
        val bucket = byFirstChar[p.first()] ?: return emptyList()
        return bucket.asSequence()
            .filter { it != p && it.startsWith(p) }
            .sortedByDescending { frequencies[it] ?: 0L }
            .take(limit)
            .map { matchCase(it, prefix) }
            .toList()
    }

    fun correct(word: String, limit: Int): List<String> {
        if (word.isEmpty() || limit <= 0) return emptyList()
        val w = word.lowercase()
        if (frequencies.containsKey(w)) return emptyList()
        val bucket = byFirstChar[w.first()] ?: return emptyList()
        return bucket.asSequence()
            .filter { abs(it.length - w.length) <= MAX_EDITS }
            .mapNotNull { candidate ->
                val distance = boundedDistance(w, candidate, MAX_EDITS)
                if (distance in 1..MAX_EDITS) Scored(candidate, distance, frequencies[candidate] ?: 0L) else null
            }
            .sortedWith(compareBy({ it.distance }, { -it.frequency }))
            .take(limit)
            .map { matchCase(it.word, word) }
            .toList()
    }

    private data class Scored(val word: String, val distance: Int, val frequency: Long)

    /**
     * Optimal string alignment (Damerau-Levenshtein with adjacent transpositions) distance, capped:
     * returns [max] + 1 as soon as the whole row exceeds [max], so far-apart words are skipped fast.
     */
    private fun boundedDistance(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        var prevPrev = IntArray(b.length + 1)
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            var rowMin = curr[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = min(min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, prevPrev[j - 2] + 1)
                }
                curr[j] = value
                rowMin = min(rowMin, value)
            }
            if (rowMin > max) return max + 1
            val spare = prevPrev
            prevPrev = prev
            prev = curr
            curr = spare
        }
        return prev[b.length]
    }

    private companion object {
        const val MAX_EDITS = 2
    }
}
