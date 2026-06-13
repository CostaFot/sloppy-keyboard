package com.markedusduplicate.slopboard.suggestion.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds the "pick gif search queries" prompt from the captured on-screen text and parses the
 * model's reply into a short list of queries. Pure (no LiteRT types) so it's unit-testable —
 * mirrors [SmartReplyPrompt] / [SuggestionPrompt].
 */
object GifPrompt {

    private const val MAX_CONTEXT_CHARS = 4000
    private const val MAX_QUERIES = 3
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun queries(screenText: String): String = buildString {
        append("Based on this conversation:\n\n")
        append(screenText.takeLast(MAX_CONTEXT_CHARS))
        append("\n\nSuggest 3 short GIF search queries that would make a funny or fitting gif ")
        append("response. Return ONLY a JSON array of strings, max 3 words each.\n")
        append("Example: [\"surprised pikachu\", \"absolutely not\", \"chef kiss\"]")
    }

    /** Tolerant of a JSON array, or a loose comma/newline list. Trimmed, de-duped, capped at 3. */
    fun parse(raw: String): List<String> {
        val candidates = parseJsonArray(raw) ?: parseLoose(raw)
        val seen = LinkedHashSet<String>()
        for (candidate in candidates) {
            val query = candidate.trim().trim('"', '-', '*', '.').trim()
            if (query.isNotEmpty()) seen.add(query)
            if (seen.size == MAX_QUERIES) break
        }
        return seen.toList()
    }

    private fun parseJsonArray(raw: String): List<String>? {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start == -1 || end <= start) return null
        return runCatching {
            json.parseToJsonElement(raw.substring(start, end + 1))
                .jsonArray
                .map { it.jsonPrimitive.content }
        }.getOrNull()
    }

    private fun parseLoose(raw: String): List<String> =
        raw.lineSequence().flatMap { it.split(',').asSequence() }.toList()
}
