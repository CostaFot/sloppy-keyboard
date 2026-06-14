package com.markedusduplicate.slopboard.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the most recent on-screen text captured by [SlopboardAccessibilityService]. App-singleton
 * so it bridges the accessibility service (which produces it) and the slop-detection layer (which
 * reads it to decide what to check).
 */
@Singleton
class ScreenContextHolder @Inject constructor() {

    private val _screenText = MutableStateFlow("")

    /** Latest visible window text, or empty when nothing has been captured yet. */
    val screenText: StateFlow<String> = _screenText.asStateFlow()

    fun update(text: String) {
        _screenText.value = text
    }
}
