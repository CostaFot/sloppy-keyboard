package com.markedusduplicate.slopboard.keyboard.observe

import com.markedusduplicate.slopboard.keyboard.observe.TextContext.CONTEXT_WORDS


/**
 * Pure helpers for turning the raw "text before the cursor" into the pieces the suggestion and
 * learning layers need: a normalized n-gram context key, plus the in-progress word being typed.
 */
object TextContext {

    /** Words are runs of letters/digits/apostrophes; everything else is a boundary. */
    private val WORD = Regex("[\\p{L}\\p{N}']+")

    const val CONTEXT_WORDS = 3

    fun words(text: String): List<String> = WORD.findAll(text).map { it.value }.toList()

    /** Words that are fully typed (a trailing in-progress word is excluded). */
    fun finalizedWords(text: String): List<String> {
        val all = words(text)
        return if (endsOnBoundary(text)) all else all.dropLast(1)
    }

    /** True when the cursor sits just after a word boundary (space/punctuation), not mid-word. */
    fun endsOnBoundary(text: String): Boolean {
        val last = text.lastOrNull() ?: return true
        return !(last.isLetterOrDigit() || last == '\'')
    }

    /**
     * The n-gram key for predicting the *next* word: the last [CONTEXT_WORDS] complete words,
     * lowercased. A trailing in-progress word (no boundary yet) is excluded — it's the [prefix].
     */
    fun predictionContext(textBeforeCursor: String): String {
        val all = words(textBeforeCursor)
        val complete = if (endsOnBoundary(textBeforeCursor)) all else all.dropLast(1)
        return complete.takeLast(CONTEXT_WORDS).joinToString(" ") { it.lowercase() }
    }

    /** The word currently being typed (empty if the cursor is on a boundary). */
    fun currentPrefix(textBeforeCursor: String): String =
        if (endsOnBoundary(textBeforeCursor)) "" else (words(textBeforeCursor).lastOrNull() ?: "")

    /** Context key for learning, given the [completeWords] that precede a freshly finished word. */
    fun contextKey(completeWords: List<String>): String =
        completeWords.takeLast(CONTEXT_WORDS).joinToString(" ") { it.lowercase() }
}
