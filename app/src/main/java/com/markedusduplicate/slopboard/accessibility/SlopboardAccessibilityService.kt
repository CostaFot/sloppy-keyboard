package com.markedusduplicate.slopboard.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reads the text visible on the current screen and publishes it to [ScreenContextHolder].
 *
 * Capture is gated on the user actually editing a text field (the only time the keyboard needs
 * screen context) and filtered to cut chrome: only visible nodes, only their `text` (icon buttons
 * expose their label as `contentDescription`, which is almost all noise), de-duplicated, and with
 * the focused field itself skipped (the IME already has that text). Bursts of
 * `TYPE_WINDOW_CONTENT_CHANGED` are debounced so a settling screen is captured once.
 *
 * Capture-only for now — the suggestion pipeline is untouched; a later step will read
 * [ScreenContextHolder] to give the on-device LLM screen context. The user must enable this service
 * under Settings → Accessibility; capture stays on-device.
 */
@AndroidEntryPoint
class SlopboardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var screenContextHolder: ScreenContextHolder

    @Inject
    @ApplicationCoroutineScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private var captureJob: Job? = null
    private var lastText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        logDebug { "accessibility service connected" }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                -> Unit

            else -> return
        }
        if (event.packageName == packageName) return
        scheduleCapture()
    }

    override fun onInterrupt() = Unit

    /** Coalesce a storm of content-changed events into a single capture of the settled screen. */
    private fun scheduleCapture() {
        captureJob?.cancel()
        captureJob = scope.launch(dispatcherProvider.io) {
            delay(DEBOUNCE_MS)
            capture()
        }
    }

    private fun capture() {
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return

        val lines = LinkedHashSet<String>()
        collectText(root, focused, lines)
        val text = lines.joinToString("\n").take(MAX_SCREEN_CHARS)
        if (text.isEmpty() || text == lastText) return

        lastText = text
        screenContextHolder.update(text)
        logDebug { "screen (${root.packageName}): $text" }
    }

    private fun collectText(
        node: AccessibilityNodeInfo?,
        focused: AccessibilityNodeInfo,
        out: LinkedHashSet<String>,
    ) {
        if (node == null || out.size >= MAX_LINES) return
        if (node != focused && node.isVisibleToUser && !node.isPassword) {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) out.add(text)
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), focused, out)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
        const val MAX_SCREEN_CHARS = 2000
        const val MAX_LINES = 200
    }
}
