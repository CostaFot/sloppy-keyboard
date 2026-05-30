package com.markedusduplicate.slopboard.suggestion.llm

import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds the next-word prediction prompt (optionally injecting the user's personalization hints)
 * and parses the model's reply into at most [MAX_SUGGESTIONS] words. Pure and self-contained — no
 * LiteRT types — so it can be unit tested without the inference engine.
 */
object SuggestionPrompt {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun build(textBeforeCursor: String, context: String, hints: List<String>): String {
        val hintLine = if (hints.isEmpty()) {
            ""
        } else {
            "This user often follows \"$context\" with: ${hints.joinToString(", ")}.\n"
        }
        return buildString {
            append("You are a phone keyboard predicting the next word.\n")
            append(hintLine)
            append("Reply with ONLY a JSON array of $MAX_SUGGESTIONS short lowercase words, nothing else.\n")
            append("Text so far: \"${textBeforeCursor.takeLast(MAX_CONTEXT_CHARS)}\"")
        }
    }

    fun parse(raw: String): List<String> {
        val words = parseJsonArray(raw) ?: parseLoose(raw)
        val seen = HashSet<String>()
        val out = ArrayList<String>(MAX_SUGGESTIONS)
        for (word in words) {
            val cleaned = word.trim().trim('"', '\'')
            if (cleaned.isEmpty()) continue
            if (seen.add(cleaned.lowercase())) {
                out.add(cleaned)
                if (out.size == MAX_SUGGESTIONS) break
            }
        }
        return out
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

    private const val MAX_CONTEXT_CHARS = 200
}
