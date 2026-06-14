package com.markedusduplicate.slopboard.suggestion.llm

/**
 * Prompt for the multimodal "reply from a screenshot" flow, and cleanup of the model's answer. Pure
 * (no LiteRT types) so it's unit-testable.
 */
object VisionPrompt {

    fun reply(): String = buildString {
        append("This is a screenshot of my phone (ignore the keyboard at the bottom).\n")
        append("Based on the conversation or content visible, write ONE natural reply I could send.\n")
        append("Output only the reply text — no quotes, no preamble.")
    }

    fun clean(raw: String): String = raw.trim().trim('"').trim()
}
