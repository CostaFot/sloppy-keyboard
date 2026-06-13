package com.markedusduplicate.slopboard.accessibility

import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-demand screenshot bridge. Only an [android.accessibilityservice.AccessibilityService] can call
 * `takeScreenshot`, so the service registers its capture implementation here and the keyboard's view
 * models call [capture] to get a JPEG of the current screen. Returns null when the service isn't
 * connected (accessibility disabled) or the capture fails.
 */
@Singleton
class ScreenshotCapturer @Inject constructor() {

    @Volatile
    private var handler: (suspend () -> ByteArray?)? = null

    /** True while the accessibility service is connected and able to take screenshots. */
    val isAvailable: Boolean get() = handler != null

    fun setHandler(handler: (suspend () -> ByteArray?)?) {
        this.handler = handler
    }

    suspend fun capture(): ByteArray? = handler?.invoke()
}
