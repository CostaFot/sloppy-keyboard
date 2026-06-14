package com.markedusduplicate.slopboard.suggestion.llm

/**
 * Prompt for transcribing the readable text out of a screenshot, plus cleanup of the model's answer.
 * Used by the slop detector as a fallback when the accessibility tree yields too little text. Pure
 * (no LiteRT types) so it's unit-testable.
 */
object OcrPrompt {

    fun transcribe(): String = buildString {
        append("This is a screenshot of my phone.\n")
        append("Transcribe all the readable text, in reading order.\n")
        append("Ignore the floating mascot, overlays, the keyboard, and the status / navigation bars.\n")
        append("Output only the transcribed text — no commentary, no labels, no explanation.")
    }

    fun clean(raw: String): String = raw.trim().trim('"').trim()
}
