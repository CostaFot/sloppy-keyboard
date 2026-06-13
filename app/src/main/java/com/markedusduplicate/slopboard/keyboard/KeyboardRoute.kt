package com.markedusduplicate.slopboard.keyboard

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The keyboard's navigation destinations. Sealed so the back stack is strongly typed; `@Serializable`
 * so the routes work with a saveable [androidx.navigation3.runtime.NavBackStack] if we ever persist
 * navigation across config changes / process death.
 */
@Serializable
sealed interface KeyboardRoute : NavKey {

    /** Chip label for the debug nav bar, derived from the type name (override for a nicer name). */
    val label: String get() = this::class.simpleName.orEmpty()

    @Serializable
    data object Main : KeyboardRoute

    @Serializable
    data object SmartReply : KeyboardRoute {
        override val label: String get() = "Reply"
    }
}
