package com.markedusduplicate.slopboard.suggestion

/**
 * Capitalize [word] when the user's [prefix] starts uppercase, so a suggestion's casing follows
 * what is being typed (e.g. `Andro` → `Android`, not `android`). Shared by the dictionary and LLM
 * suggestion sources.
 */
internal fun matchCase(word: String, prefix: String): String {
    val first = prefix.firstOrNull() ?: return word
    if (!first.isUpperCase() || word.isEmpty() || !word[0].isLowerCase()) return word
    return word.replaceFirstChar { it.uppercaseChar() }
}
