package com.markedusduplicate.slopboard.keyboard.observe

import android.text.InputType
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "what is to the left of the cursor" plus whether the current field
 * is one we're allowed to learn from / suggest into. The service pushes updates here; the
 * observation and suggestion layers read from it.
 */
@Singleton
class InputContextTracker @Inject constructor() {

    private val _textBeforeCursor = MutableStateFlow("")
    val textBeforeCursor: StateFlow<String> = _textBeforeCursor.asStateFlow()

    private val _allowed = MutableStateFlow(true)

    /** False for password / no-suggestion fields — chips hidden and nothing is learned. */
    val allowed: StateFlow<Boolean> = _allowed.asStateFlow()

    fun updateText(textBeforeCursor: String) {
        _textBeforeCursor.value = textBeforeCursor
    }

    fun onStartInput(editorInfo: EditorInfo?) {
        _textBeforeCursor.value = ""
        _allowed.value = editorInfo?.let { allowsLearning(it.inputType) } ?: true
    }

    private fun allowsLearning(inputType: Int): Boolean {
        if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
                    variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
                    variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

            InputType.TYPE_CLASS_NUMBER ->
                variation != InputType.TYPE_NUMBER_VARIATION_PASSWORD

            else -> true
        }
    }
}
