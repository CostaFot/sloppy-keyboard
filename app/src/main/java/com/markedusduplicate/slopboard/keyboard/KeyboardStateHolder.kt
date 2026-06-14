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

    /**
     * Navigate to [route], single-top: if it's already in the back stack, pop everything above it
     * to bring it forward; otherwise push it. Never duplicates an entry or clears the stack.
     */
    fun navigateTo(route: KeyboardRoute) {
        val index = backStack.indexOf(route)
        if (index == -1) {
            backStack.add(route)
        } else {
            while (backStack.lastIndex > index) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    fun back() {
        backStack.removeLastOrNull()
    }
}
