package com.markedusduplicate.slopboard.clippy

/** What the floating mascot is doing: idle, thinking up a remark, speaking it, or unable to look. */
sealed interface ClippyState {
    data object Idle : ClippyState
    data object Thinking : ClippyState
    data class Speaking(val remark: String) : ClippyState
    data class Unavailable(val reason: String) : ClippyState
}
