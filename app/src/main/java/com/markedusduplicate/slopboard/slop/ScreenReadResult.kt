package com.markedusduplicate.slopboard.slop

/** Outcome of reading the on-screen text: the captured text, or why it couldn't be read. */
sealed interface ScreenReadResult {
    data class Text(val value: String) : ScreenReadResult
    data class Unavailable(val reason: String) : ScreenReadResult
}
