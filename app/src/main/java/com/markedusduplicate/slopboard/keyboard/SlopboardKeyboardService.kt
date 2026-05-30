package com.markedusduplicate.slopboard.keyboard

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.keyboard.observe.InputContextTracker
import com.markedusduplicate.slopboard.keyboard.observe.ObservationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SlopboardKeyboardService :
    LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @Inject
    lateinit var keyboardStateHolder: KeyboardStateHolder

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var keyboardHandler: KeyboardHandler

    @Inject
    lateinit var inputContextTracker: InputContextTracker

    @Inject
    lateinit var observationManager: ObservationManager

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val scope by lazy {
        CoroutineScope(dispatcherProvider.ui + SupervisorJob())
    }

    override fun onCreate() {
        logDebug { "keyboard on create" }
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        scope.launch {
            keyboardHandler.queue.collect {
                when (it) {
                    is KeyboardMessage.Text -> {
                        currentInputConnection.commitText(it.text, 1)
                    }

                    is KeyboardMessage.Delete -> {
                        val selectedText = currentInputConnection.getSelectedText(0)

                        if (selectedText.isNotBlank()) {
                            currentInputConnection.deleteSurroundingText(1, 0)
                        } else {
                            currentInputConnection.commitText("", 1)
                        }
                    }

                    is KeyboardMessage.CommitSuggestion -> {
                        if (it.replacePrefixLength > 0) {
                            currentInputConnection.deleteSurroundingText(it.replacePrefixLength, 0)
                        }
                        currentInputConnection.commitText("${it.word} ", 1)
                    }
                }
                refreshInputContext()
            }
        }
    }

    /** Snapshot the text left of the cursor and feed the learning + suggestion layers. */
    private fun refreshInputContext() {
        logDebug { "refreshInputContext" }
        val before = currentInputConnection?.getTextBeforeCursor(CONTEXT_LOOKBACK, 0)?.toString() ?: ""
        inputContextTracker.updateText(before)
        observationManager.onTextBeforeCursor(before)
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        logDebug { "onStartInput" }
        inputContextTracker.onStartInput(info)
        observationManager.reset()
        refreshInputContext()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd,
        )
        refreshInputContext()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onCreateInputView(): View {
        logDebug { "keyboard onCreateInputView" }
        val view = KeyboardComposeView(this, keyboardStateHolder)

        // Make the service the owner of the view tree so AbstractComposeView (and NavDisplay) can
        // find a lifecycle / saved-state / view-model store, plus a navigation event dispatcher.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
            decorView.setViewTreeNavigationEventDispatcherOwner(FakeNavigationEventDispatcherOwner)
        }
        return view
    }

    override fun onDestroy() {
        logDebug { "keyboard onDestroy" }
        super.onDestroy()
    }

    private companion object {
        const val CONTEXT_LOOKBACK = 100
    }
}
