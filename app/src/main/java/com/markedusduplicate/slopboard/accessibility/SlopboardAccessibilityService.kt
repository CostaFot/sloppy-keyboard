package com.markedusduplicate.slopboard.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.scale
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

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

    @Inject
    lateinit var screenshotCapturer: ScreenshotCapturer

    private var captureJob: Job? = null
    private var lastText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        logDebug { "accessibility service connected" }
        screenshotCapturer.setHandler(::captureScreenshot)
    }

    override fun onDestroy() {
        screenshotCapturer.setHandler(null)
        super.onDestroy()
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

    /** Capture the current display as a downscaled JPEG (null on failure). */
    private suspend fun captureScreenshot(): ByteArray? = withContext(Dispatchers.IO) {
        val result = awaitScreenshot() ?: return@withContext null
        return@withContext encodeJpeg(result)
    }

    private suspend fun awaitScreenshot(): ScreenshotResult? =
        suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                Dispatchers.IO.asExecutor(),
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        if (continuation.isActive) continuation.resume(result)
                    }

                    override fun onFailure(errorCode: Int) {
                        logDebug { "takeScreenshot failed: $errorCode" }
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }

    private fun encodeJpeg(result: ScreenshotResult): ByteArray? {
        val buffer = result.hardwareBuffer
        return buffer.use { buffer ->
            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) ?: return null
            val software = hardware.copy(Bitmap.Config.ARGB_8888, false)
            val scaled = downscale(software, MAX_SCREENSHOT_DIM)
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
        }
    }

    private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        return bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
        const val MAX_SCREEN_CHARS = 2000
        const val MAX_LINES = 200
        const val MAX_SCREENSHOT_DIM = 1024
        const val JPEG_QUALITY = 85
    }
}
