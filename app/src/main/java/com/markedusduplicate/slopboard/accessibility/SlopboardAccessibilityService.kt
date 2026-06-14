package com.markedusduplicate.slopboard.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.scale
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.agent.ActionableNode
import com.markedusduplicate.slopboard.agent.AgentAction
import com.markedusduplicate.slopboard.agent.Bounds
import com.markedusduplicate.slopboard.agent.ScreenAgentHandler
import com.markedusduplicate.slopboard.agent.ScreenController
import com.markedusduplicate.slopboard.agent.ScreenSnapshot
import com.markedusduplicate.slopboard.agent.ScrollDirection
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
 * The app's eyes and hands on the screen. It does three things, all of which only an
 * `AccessibilityService` can do, so it registers itself with the singletons the rest of the app
 * drives it through:
 *
 * 1. **Screenshots** for OCR — registers [ScreenshotCapturer]'s handler (the in-use screen reader,
 *    [com.markedusduplicate.slopboard.slop.OcrScreenTextReader], asks for a JPEG of the display).
 * 2. **Agent actions** — registers a [ScreenAgentHandler] on [ScreenController]: snapshots the
 *    foreground app's actionable elements and taps/types/scrolls on the agent's behalf.
 * 3. **Window text** — publishes the visible on-screen text to [ScreenContextHolder]. Text capture
 *    is filtered to cut chrome (only visible nodes, only their `text`, de-duplicated) and bursts of
 *    `TYPE_WINDOW_CONTENT_CHANGED` are debounced so a settling screen is captured once.
 *
 * The user must enable this service under Settings → Accessibility; everything stays on-device.
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

    @Inject
    lateinit var screenController: ScreenController

    private var captureJob: Job? = null
    private var lastText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        logDebug { "accessibility service connected" }
        screenshotCapturer.setHandler(::captureScreenshot)
        screenController.setHandler(agentHandler)
    }

    override fun onDestroy() {
        screenshotCapturer.setHandler(null)
        screenController.setHandler(null)
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

    // ---- Agent: read the actionable elements of the foreground app and act on them. ----

    private val agentHandler = object : ScreenAgentHandler {
        override suspend fun snapshot(): ScreenSnapshot? = withContext(dispatcherProvider.ui) {
            val root = targetRoot() ?: return@withContext null
            val nodes = ArrayList<ActionableNode>()
            collectActionable(root, nodes)
            ScreenSnapshot(root.packageName?.toString().orEmpty(), nodes)
        }

        override suspend fun perform(action: AgentAction, nodes: List<ActionableNode>): Boolean =
            withContext(dispatcherProvider.ui) {
                when (action) {
                    is AgentAction.Tap -> tap(nodes.getOrNull(action.index) ?: return@withContext false)
                    is AgentAction.Type ->
                        type(nodes.getOrNull(action.index) ?: return@withContext false, action.text)

                    is AgentAction.Scroll -> scroll(action.direction)
                    AgentAction.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
                    is AgentAction.Done -> true
                    is AgentAction.Unknown -> false
                }
            }
    }

    /** The foreground app's window, skipping our own overlay windows if one happens to hold focus. */
    private fun targetRoot(): AccessibilityNodeInfo? {
        rootInActiveWindow?.let { if (it.packageName != packageName) return it }
        windows.sortedByDescending { it.layer }.forEach { window ->
            val root = window.root
            if (root != null && root.packageName != packageName) return root
        }
        return rootInActiveWindow
    }

    private fun collectActionable(node: AccessibilityNodeInfo?, out: ArrayList<ActionableNode>) {
        if (node == null || out.size >= MAX_NODES) return
        if (node.isVisibleToUser && isActionable(node)) {
            val label = labelOf(node)
            if (label.isNotEmpty() || node.isEditable) {
                val rect = Rect().also(node::getBoundsInScreen)
                out.add(
                    ActionableNode(
                        index = out.size,
                        label = label,
                        role = node.className?.toString()?.substringAfterLast('.').orEmpty(),
                        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                        clickable = node.isClickable,
                        editable = node.isEditable,
                        scrollable = node.isScrollable,
                    ),
                )
            }
        }
        for (i in 0 until node.childCount) collectActionable(node.getChild(i), out)
    }

    private fun isActionable(n: AccessibilityNodeInfo): Boolean =
        n.isClickable || n.isEditable || n.isScrollable || n.isLongClickable

    private fun labelOf(n: AccessibilityNodeInfo): String =
        (n.text ?: n.contentDescription ?: n.hintText)?.toString()?.trim().orEmpty()

    private suspend fun tap(target: ActionableNode): Boolean {
        val clickable = findNodeAt(target.bounds.centerX, target.bounds.centerY) { it.isClickable }
        if (clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
        return tapGesture(target.bounds.centerX, target.bounds.centerY)
    }

    private fun type(target: ActionableNode, text: String): Boolean {
        val field = findNodeAt(target.bounds.centerX, target.bounds.centerY) { it.isEditable }
            ?: return false
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scroll(direction: ScrollDirection): Boolean {
        val scrollable = findFirst(targetRoot()) { it.isScrollable } ?: return false
        val action = if (direction == ScrollDirection.DOWN) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return scrollable.performAction(action)
    }

    /** Deepest visible node containing the point that matches [predicate] (re-resolved live). */
    private fun findNodeAt(
        x: Int,
        y: Int,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val root = targetRoot() ?: return null
        var result: AccessibilityNodeInfo? = null
        val rect = Rect()
        fun dfs(n: AccessibilityNodeInfo?) {
            n ?: return
            n.getBoundsInScreen(rect)
            if (n.isVisibleToUser && rect.contains(x, y) && predicate(n)) result = n
            for (i in 0 until n.childCount) dfs(n.getChild(i))
        }
        dfs(root)
        return result
    }

    private fun findFirst(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        root ?: return null
        if (root.isVisibleToUser && predicate(root)) return root
        for (i in 0 until root.childCount) {
            findFirst(root.getChild(i), predicate)?.let { return it }
        }
        return null
    }

    private suspend fun tapGesture(x: Int, y: Int): Boolean =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION_MS))
                .build()
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(d: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(d: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
                null,
            )
            if (!dispatched && continuation.isActive) continuation.resume(false)
        }

    private companion object {
        const val DEBOUNCE_MS = 350L
        const val MAX_SCREEN_CHARS = 2000
        const val MAX_LINES = 200
        const val MAX_SCREENSHOT_DIM = 1024
        const val JPEG_QUALITY = 85
        const val MAX_NODES = 60
        const val GESTURE_DURATION_MS = 50L
    }
}
