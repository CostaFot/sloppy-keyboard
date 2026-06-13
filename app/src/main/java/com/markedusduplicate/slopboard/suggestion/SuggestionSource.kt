package com.markedusduplicate.slopboard.suggestion

/**
 * The seam the suggestion coordinator pulls chips from: given the raw text before the cursor,
 * return the [Suggestions] to show (completions, a spelling fix, or next-word predictions). The
 * only implementation is the LiteRT-LM source.
 */
fun interface SuggestionSource {
    suspend fun suggest(textBeforeCursor: String): Suggestions
}
