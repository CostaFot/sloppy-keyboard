package com.markedusduplicate.slopboard.keyboard

import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardHandler @Inject constructor() {

    val queue = MutableSharedFlow<KeyboardMessage>()
}

sealed interface KeyboardMessage {
    data class Text(val text: String) : KeyboardMessage
    data class Delete(val count: Int = 1) : KeyboardMessage

    /**
     * Accept a suggestion chip: drop the [replacePrefixLength] chars of the in-progress word, then
     * commit [word] followed by a space.
     */
    data class CommitSuggestion(val word: String, val replacePrefixLength: Int) : KeyboardMessage
}