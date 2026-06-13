package com.markedusduplicate.slopboard.suggestion.llm

/**
 * Prompt for the floating "Clippy" mascot: hands the model a screenshot of whatever's on screen and
 * asks for one snarky remark about it, plus cleanup of the model's answer. Pure (no LiteRT types) so
 * it's unit-testable.
 */
object ClippyPrompt {

    fun roast(): String = buildString {
        append("You are Clippy, the infamously nosy assistant, reincarnated with opinions.\n")
        append("This is a screenshot of my phone (ignore any floating mascot and the system bars).\n")
        append("In ONE short sentence starting with \"It looks like\", make a witty, sarcastic ")
        append("remark about what I'm doing. Use very bad swear words, as bad as you can.\n")
        append("Output only the remark — no quotes, no preamble.")
    }

    fun clean(raw: String): String = raw.trim().trim('"').trim()
}
