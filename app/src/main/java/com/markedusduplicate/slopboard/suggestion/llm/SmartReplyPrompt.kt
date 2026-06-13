package com.markedusduplicate.slopboard.suggestion.llm

/**
 * Builds the "draft a full reply" prompt from the captured on-screen text and cleans the model's
 * answer. Pure and self-contained (no LiteRT types) so it can be unit tested without the engine —
 * mirrors [SuggestionPrompt], but asks for a whole message rather than the next word.
 */
object SmartReplyPrompt {

    private const val MAX_CONTEXT_CHARS = 4000

    fun reply(screenText: String): String = buildString {
        append("You are helping me write a reply on my phone.\n")
        append("Below is the text currently on my screen — a chat, thread, or email.\n")
        append("Write ONE complete, natural reply I could send. Output only the reply text — ")
        append("no quotes, no labels, no explanation.\n\n---\n")
        append(screenText.takeLast(MAX_CONTEXT_CHARS))
        append("\n---")
    }

    /** Strip wrapping whitespace/quotes the model often adds around the message. */
    fun clean(raw: String): String = raw.trim().trim('"').trim()
}
