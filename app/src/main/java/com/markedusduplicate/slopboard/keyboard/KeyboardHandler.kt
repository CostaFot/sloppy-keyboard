package com.markedusduplicate.slopboard.keyboard

import android.net.Uri
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

    /** Insert a gif via the rich-content API ([uri] is a FileProvider `content://`), clipboard fallback. */
    data class CommitGif(val uri: Uri, val mimeType: String, val description: String) : KeyboardMessage
}