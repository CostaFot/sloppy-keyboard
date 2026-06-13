package com.markedusduplicate.slopboard.keyboard

import androidx.compose.runtime.mutableStateListOf
import com.markedusduplicate.logging.logDebug
import javax.inject.Inject

class KeyboardStateHolder @Inject constructor() {
    val backStack = mutableStateListOf<Any>(KeyboardRoute)

    /** The route currently on top of the stack (observable when read in composition). */
    val currentRoute: Any?
        get() = backStack.lastOrNull()

    init {
        logDebug { "Init KeyboardStateHolder, id: ${hashCode()}" }
    }

    /** Show [route] over a deterministic stack: keyboard at the root, [route] on top (if not it). */
    fun navigateTo(route: Any) {
        backStack.clear()
        backStack.add(KeyboardRoute)
        if (route != KeyboardRoute) backStack.add(route)
    }

    fun back() {
        backStack.removeLastOrNull()
    }
}
