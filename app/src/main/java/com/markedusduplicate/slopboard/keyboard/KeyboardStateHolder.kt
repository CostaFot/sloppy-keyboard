package com.markedusduplicate.slopboard.keyboard

import androidx.navigation3.runtime.NavBackStack
import com.markedusduplicate.logging.logDebug
import javax.inject.Inject

class KeyboardStateHolder @Inject constructor() {
    val backStack = NavBackStack<KeyboardRoute>(KeyboardRoute.Main)

    /** The route currently on top of the stack (observable when read in composition). */
    val currentRoute: KeyboardRoute?
        get() = backStack.lastOrNull()

    init {
        logDebug { "Init KeyboardStateHolder, id: ${hashCode()}" }
    }

    /** Show [route] over a deterministic stack: Main at the root, [route] on top (if not it). */
    fun navigateTo(route: KeyboardRoute) {
        backStack.clear()
        backStack.add(KeyboardRoute.Main)
        if (route != KeyboardRoute.Main) backStack.add(route)
    }

    fun back() {
        backStack.removeLastOrNull()
    }
}
