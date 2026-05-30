package com.markedusduplicate.slopboard.keyboard

import androidx.compose.runtime.mutableStateListOf
import com.markedusduplicate.logging.logDebug
import javax.inject.Inject

class KeyboardStateHolder @Inject constructor() {
    val backStack = mutableStateListOf<Any>(First)

    init {
        logDebug { "Init KeyboardStateHolder, id: ${hashCode()}" }
    }
}
