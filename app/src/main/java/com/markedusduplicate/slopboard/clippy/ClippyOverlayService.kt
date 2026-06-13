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
import com.markedusduplicate.slopboard.accessibility.ScreenshotCapturer
import com.markedusduplicate.slopboard.suggestion.llm.ClippyPrompt
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
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
 * Hosts the floating Clippy mascot in a system overlay window so it lives over every app, not just
 * the keyboard. Clippy is hidden until summoned by a left→right swipe on the [ClippyEdgeHandleView]
 * tab pinned to the left edge; summoning screenshots the current screen (via the accessibility
 * service's [ScreenshotCapturer]) and asks the on-device multimodal LLM ([LlmEngine]) for a snarky
 * one-liner, shown in a speech bubble — the same capture→infer path the keyboard's Vision feature
 * uses — then auto-hides. Tapping the mascot re-rolls; tapping the bubble dismisses it.
 *
 * Unlike [com.markedusduplicate.slopboard.keyboard.SlopboardKeyboardService] (whose lifecycle is
 * driven by IME bind callbacks), an overlay service has no such callbacks and no decor view, so it
 * drives its own [LifecycleRegistry] to RESUMED and sets the view-tree owners directly on the
 * overlay view — both required for Compose to compose and recompose.
 *
 * Requires the draw-over-apps permission (checked here) and the accessibility service enabled (for
 * the screenshot). Started/stopped from the setup screen; runs as a plain started service for now.
 */
@AndroidEntryPoint
class ClippyOverlayService :
    android.app.Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @Inject
    lateinit var engine: LlmEngine

    @Inject
    lateinit var screenshotCapturer: ScreenshotCapturer

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

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

    private var overlayView: ClippyComposeView? = null
    private var edgeHandleView: ClippyEdgeHandleView? = null
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
            onTap = ::roastNow,
            onDrag = ::onDrag,
            onDismiss = ::dismiss,
        ).also(::attachOwners)
        overlayView = view
        windowManager.addView(view, layoutParams)

        val handle = ClippyEdgeHandleView(this, onSummon = ::roastNow).also(::attachOwners)
        edgeHandleView = handle
        windowManager.addView(handle, handleParams)

        isRunning = true
        logDebug { "clippy overlay + edge handle added" }
    }

    /** Make the service the owner of [view]'s tree so Compose can find a lifecycle / saved state. */
    private fun attachOwners(view: android.view.View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /** Summon Clippy: show him, screenshot the screen, ask the LLM for a one-liner, then auto-hide. */
    private fun roastNow() {
        autoHideJob?.cancel()
        tapJob?.cancel()
        tapJob = scope.launch {
            state.value = ClippyState.Thinking
            state.value = roast()
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

    private suspend fun roast(): ClippyState {
        if (!screenshotCapturer.isAvailable) {
            return ClippyState.Unavailable("Turn on the accessibility service so I can see your screen.")
        }
        val jpeg = screenshotCapturer.capture()
            ?: return ClippyState.Unavailable("I couldn't grab the screen, awkward.")
        if (engine.engineOrNull() == null) {
            return ClippyState.Unavailable("My brain isn't loaded yet (no model). Give me a sec.")
        }
        val raw = engine.generateWithImage(jpeg, ClippyPrompt.roast())
            ?: return ClippyState.Unavailable("My circuits hiccuped. Tap me again.")
        logDebug { "clippy raw: $raw" }
        val remark = ClippyPrompt.clean(raw)
        return if (remark.isEmpty()) {
            ClippyState.Unavailable("…I've got nothing. That's a first.")
        } else {
            ClippyState.Speaking(remark)
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
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        edgeHandleView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        edgeHandleView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val AUTO_HIDE_MS = 8000L

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
