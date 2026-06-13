package com.markedusduplicate.slopboard.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Reads the text visible on the current screen and publishes it to [ScreenContextHolder].
 *
 * On a window change it walks the active window's [AccessibilityNodeInfo] tree, gathers the visible
 * text (skipping password fields and our own UI), and stores the result. This is capture-only for
 * now — the suggestion pipeline is untouched; a later step will read [ScreenContextHolder] to give
 * the on-device LLM screen context.
 *
 * The user must enable this service under Settings → Accessibility; capture stays on-device.
 */
@AndroidEntryPoint
class SlopboardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var screenContextHolder: ScreenContextHolder

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

        val root = rootInActiveWindow ?: return
        val text = buildString { collectText(root, this) }.trim()
        if (text.isEmpty() || text == lastText) return

        lastText = text
        screenContextHolder.update(text)
        logDebug { "screen (${event.packageName}): $text" }
    }

    override fun onInterrupt() = Unit

    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder) {
        if (node == null || out.length >= MAX_SCREEN_CHARS) return
        if (!node.isPassword) {
            node.text?.let { append(it, out) }
            node.contentDescription?.let { append(it, out) }
        }
        for (i in 0 until node.childCount) {
            if (out.length >= MAX_SCREEN_CHARS) break
            collectText(node.getChild(i), out)
        }
    }

    private fun append(text: CharSequence, out: StringBuilder) {
        val trimmed = text.toString().trim()
        if (trimmed.isEmpty()) return
        if (out.isNotEmpty()) out.append('\n')
        out.append(trimmed)
    }

    private companion object {
        const val MAX_SCREEN_CHARS = 2000
    }
}
