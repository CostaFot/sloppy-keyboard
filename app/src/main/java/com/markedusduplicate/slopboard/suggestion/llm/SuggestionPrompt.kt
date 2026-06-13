package com.markedusduplicate.slopboard.suggestion.llm

import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository.Companion.MAX_SUGGESTIONS
import com.markedusduplicate.slopboard.suggestion.Suggestions
import com.markedusduplicate.slopboard.suggestion.matchCase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds the prediction prompt and parses the model's reply into [Suggestions]. Two modes:
 *
 * - **mid-word** (a [prefix] is being typed): asks for a spelling [Suggestions.correction] of the
 *   current word plus completions of it.
 * - **on a boundary** (no prefix): asks for next-word predictions.
 *
 * Pure and self-contained — no LiteRT types — so it can be unit tested without the inference engine.
 */
object SuggestionPrompt {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** A word is a run of letters/digits/apostrophes; matches `TextContext`'s notion of a word. */
    private val WORD = Regex("[\\p{L}\\p{N}']+")

    fun build(textBeforeCursor: String, context: String, prefix: String, hints: List<String>): String {
        val hintLine = if (hints.isEmpty()) {
            ""
        } else {
            "This user often follows \"$context\" with: ${hints.joinToString(", ")}.\n"
        }
        val tail = textBeforeCursor.takeLast(MAX_CONTEXT_CHARS)
        return if (prefix.isEmpty()) {
            buildString {
                append("You are a phone keyboard predicting the next word.\n")
                append(hintLine)
                append("Reply with ONLY a JSON array of $MAX_SUGGESTIONS single lowercase words (one word each), nothing else.\n")
                append("Text so far: \"$tail\"")
            }
        } else {
            buildString {
                append("You are a phone keyboard completing and spell-checking the word being typed.\n")
                append(hintLine)
                append("Current word: \"$prefix\".\n")
                append(
                    "Reply with ONLY JSON like {\"fix\": \"corrected spelling or empty\", " +
                            "\"words\": [up to $MAX_SUGGESTIONS single-word completions]}. " +
                            "Each entry is one word. Preserve capitalization.\n",
                )
                append("Text so far: \"$tail\"")
            }
        }
    }

    /**
     * Parses [raw] into [Suggestions]. The optional [prefix] is the word being typed: a non-blank,
     * different `fix` becomes the leading correction chip, and completions are recased to match the
     * prefix's capitalization.
     */
    fun parse(raw: String, prefix: String = ""): Suggestions {
        val obj = parseObject(raw)
        val rawWords = obj?.words ?: parseJsonArray(raw) ?: parseLoose(raw)
        val fix = normalizeFix(obj?.fix, prefix)

        val seen = HashSet<String>()
        val words = ArrayList<String>(MAX_SUGGESTIONS)
        fun add(candidate: String?) {
            val word = candidate?.let { matchCase(firstWord(it), prefix) } ?: return
            if (word.isEmpty() || words.size == MAX_SUGGESTIONS) return
            if (seen.add(word.lowercase())) words.add(word)
        }

        add(fix)
        rawWords.forEach { add(it) }
        return Suggestions(words, correction = fix?.takeIf { words.contains(it) })
    }

    private data class ParsedObject(val fix: String?, val words: List<String>)

    private fun parseObject(raw: String): ParsedObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return runCatching {
            val obj = json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
            val fix = obj["fix"]?.jsonPrimitive?.contentOrNull
            val words = obj["words"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            ParsedObject(fix, words)
        }.getOrNull()
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

    /** A meaningful spelling fix: a single word, recased, and actually different from what was typed. */
    private fun normalizeFix(fix: String?, prefix: String): String? {
        val cleaned = fix?.let { firstWord(it) }?.takeIf { it.isNotEmpty() } ?: return null
        val recased = matchCase(cleaned, prefix)
        return recased.takeIf { prefix.isNotEmpty() && !it.equals(prefix, ignoreCase = true) }
    }

    /** First word of [raw] — a chip is always a single token, even if the model replied with a phrase. */
    private fun firstWord(raw: String): String = WORD.find(raw)?.value.orEmpty()

    private const val MAX_CONTEXT_CHARS = 200
}
