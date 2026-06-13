package com.markedusduplicate.slopboard.suggestion.llm

import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import com.markedusduplicate.slopboard.suggestion.Suggestions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds the next-word prompt and parses the model's reply into [Suggestions]. Full-freedom: the
 * model sees only the text so far (no personalization hints) and is asked for the single next word.
 *
 * Pure and self-contained — no LiteRT types — so it can be unit tested without the inference engine.
 */
object SuggestionPrompt {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** A word is a run of letters/digits/apostrophes; matches `TextContext`'s notion of a word. */
    private val WORD = Regex("[\\p{L}\\p{N}']+")

    fun nextWord(textBeforeCursor: String): String = buildString {
        append("You are a phone keyboard predicting the next word.\n")
        append("Reply with ONLY the single next word, nothing else.\n")
        append("Text so far: \"${textBeforeCursor.takeLast(MAX_CONTEXT_CHARS)}\"")
    }

    /** Parses [raw] into next-word [Suggestions]: tolerant of a bare word, a JSON array, or a list. */
    fun parse(raw: String): Suggestions {
        val rawWords = parseJsonArray(raw) ?: parseLoose(raw)
        val seen = HashSet<String>()
        val words = ArrayList<String>(MAX_SUGGESTIONS)
        for (candidate in rawWords) {
            val word = firstWord(candidate)
            if (word.isEmpty()) continue
            if (seen.add(word.lowercase())) {
                words.add(word)
                if (words.size == MAX_SUGGESTIONS) break
            }
        }
        return Suggestions(words)
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
        raw.trim().trim('[', ']')
            .split(',', '\n')
            .map { it.trim() }

    /** First word of [raw] — a chip is always a single token, even if the model replied with a phrase. */
    private fun firstWord(raw: String): String = WORD.find(raw)?.value.orEmpty()

    private const val MAX_CONTEXT_CHARS = 200
}
