package com.markedusduplicate.slopboard.clippy

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.agent.AgentEngine
import com.markedusduplicate.slopboard.agent.AgentOverlayView
import com.markedusduplicate.slopboard.agent.AgentState
import com.markedusduplicate.slopboard.di.OcrScreenText
import com.markedusduplicate.slopboard.slop.ScreenReadResult
import com.markedusduplicate.slopboard.slop.ScreenTextReader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the floating Clippy mascot in a system overlay window so it lives over every app. Clippy is
 * hidden until summoned by a left→right swipe on the [ClippyEdgeHandleView] tab pinned to the left
 * edge; summoning reads the text on the current screen (via the `@OcrScreenText` [ScreenTextReader]
 * — screenshot OCR) and shows it in a speech bubble, then auto-hides. Tapping the mascot re-runs the
 * check; tapping the bubble dismisses it. The captured text is destined for an AI-detection backend
 * ([com.markedusduplicate.slopboard.slop.AiDetectorRepository]) to judge whether it's AI-generated
 * "slop" — see the TODO in [detectSlop].
 *
 * An overlay service has no bind callbacks and no decor view, so it drives its own
 * [LifecycleRegistry] to RESUMED and sets the view-tree owners directly on the overlay view — both
 * required for Compose to compose and recompose.
 *
 * Requires the draw-over-apps permission (checked here) and the accessibility service enabled (for
 * reading the screen). Started/stopped from the setup screen; runs as a plain started service for now.
 */
@AndroidEntryPoint
class ClippyOverlayService :
    android.app.Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @Inject
    @OcrScreenText
    lateinit var screenTextReader: ScreenTextReader

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var agentEngine: AgentEngine

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val scope by lazy { CoroutineScope(dispatcherProvider.ui + SupervisorJob()) }

    private val state = MutableStateFlow<ClippyState>(ClippyState.Hidden)

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }

    private fun overlayParams(buildGravity: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = buildGravity
        this.x = x
        this.y = y
    }

    // The mascot is summoned next to the edge tab, so it speaks from the left-centre.
    private val layoutParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = dp(44), y = 0)
    }
    private val handleParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = 0, y = 0)
    }

    // Full-screen so the highlight box can be drawn at any element's screen coordinates. Pass-through
    // (FLAG_NOT_TOUCHABLE) by default; made touchable only while a suggestion is showing.
    private val agentOverlayParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }

    private var overlayView: ClippyComposeView? = null
    private var edgeHandleView: ClippyEdgeHandleView? = null
    private var agentOverlayView: AgentOverlayView? = null
    private var agentOverlayAdded = false
    private var tapJob: Job? = null
    private var autoHideJob: Job? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        if (!Settings.canDrawOverlays(this)) {
            logDebug { "clippy: no draw-over permission, stopping" }
            stopSelf()
            return
        }

        val view = ClippyComposeView(
            context = this,
            state = state.asStateFlow(),
            onTap = ::detectSlopNow,
            onDrag = ::onDrag,
            onDismiss = ::dismiss,
        ).also(::attachOwners)
        overlayView = view
        windowManager.addView(view, layoutParams)

        val handle = ClippyEdgeHandleView(
            context = this,
            onSummon = ::detectSlopNow,
            onOpenAgent = agentEngine::start,
        ).also(::attachOwners)
        edgeHandleView = handle
        windowManager.addView(handle, handleParams)

        agentOverlayView = AgentOverlayView(this, agentEngine).also(::attachOwners)
        scope.launch { agentEngine.state.collect(::applyAgentState) }

        isRunning = true
        logDebug { "clippy overlay + edge handle added" }
    }

    /** Make the service the owner of [view]'s tree so Compose can find a lifecycle / saved state. */
    private fun attachOwners(view: android.view.View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    /** Add/remove the agent overlay window and tune touch/visibility as the suggestion loop runs. */
    private fun applyAgentState(st: AgentState) {
        val overlay = agentOverlayView ?: return
        if (st == AgentState.Idle) {
            if (agentOverlayAdded) {
                runCatching { windowManager.removeView(overlay) }
                agentOverlayAdded = false
            }
            return
        }
        if (!agentOverlayAdded) {
            windowManager.addView(overlay, agentOverlayParams)
            agentOverlayAdded = true
        }
        // Touchable only when there's something to tap (a suggestion / a result); pass-through while
        // thinking, and hidden while acting so the injected tap lands on the app beneath, not on us.
        setAgentTouchable(st is AgentState.Suggest || st is AgentState.Done || st is AgentState.Failed)
        val acting = st == AgentState.Acting
        overlay.visibility = if (acting) android.view.View.GONE else android.view.View.VISIBLE
        edgeHandleView?.visibility = if (acting) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun setAgentTouchable(touchable: Boolean) {
        val overlay = agentOverlayView ?: return
        val flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        agentOverlayParams.flags =
            if (touchable) agentOverlayParams.flags and flag.inv() else agentOverlayParams.flags or flag
        if (agentOverlayAdded) windowManager.updateViewLayout(overlay, agentOverlayParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /** Summon Clippy: show him, read the screen's text, surface it, then auto-hide. */
    private fun detectSlopNow() {
        autoHideJob?.cancel()
        tapJob?.cancel()
        tapJob = scope.launch {
            state.value = ClippyState.Thinking
            state.value = detectSlop()
            scheduleAutoHide()
        }
    }

    private fun dismiss() {
        autoHideJob?.cancel()
        tapJob?.cancel()
        state.value = ClippyState.Hidden
    }

    private fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(AUTO_HIDE_MS)
            state.value = ClippyState.Hidden
        }
    }

    private suspend fun detectSlop(): ClippyState =
        when (val result = screenTextReader.read()) {
            is ScreenReadResult.Unavailable -> ClippyState.Unavailable(result.reason)
            is ScreenReadResult.Text -> {
                logDebug { "slop: captured ${result.value.length} chars" }
                // TODO(ai-detector): hand result.value to AiDetectorRepository.detect(...) and show
                //  the verdict here instead of this raw captured-text preview.
                ClippyState.Speaking(result.value.take(PREVIEW_CHARS))
            }
        }

    private fun onDrag(dx: Float, dy: Float) {
        val view = overlayView ?: return
        layoutParams.x += dx.toInt()
        layoutParams.y += dy.toInt()
        windowManager.updateViewLayout(view, layoutParams)
    }

    override fun onDestroy() {
        isRunning = false
        tapJob?.cancel()
        autoHideJob?.cancel()
        agentEngine.close()
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        edgeHandleView?.let { runCatching { windowManager.removeView(it) } }
        agentOverlayView?.let { if (agentOverlayAdded) runCatching { windowManager.removeView(it) } }
        overlayView = null
        edgeHandleView = null
        agentOverlayView = null
        agentOverlayAdded = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val AUTO_HIDE_MS = 8000L
        private const val PREVIEW_CHARS = 200

        /** True while the overlay is up; read by the setup screen to drive the start/stop toggle. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            context.startService(Intent(context, ClippyOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClippyOverlayService::class.java))
        }
    }
}
