package com.feelsokman.androidtemplate.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import com.feelsokman.common.coroutine.DispatcherProvider
import com.feelsokman.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TemplateKeyboardService : InputMethodService() {
    @Inject
    lateinit var keyboardStateHolder: KeyboardStateHolder

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var keyboardHandler: KeyboardHandler

    private val scope by lazy {
        CoroutineScope(dispatcherProvider.ui + SupervisorJob())
    }


    override fun onCreate() {
        logDebug { "keyboard on create" }
        super.onCreate()
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
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        logDebug { "keyboard onCreateInputView" }
        return createKeyboardComposeView(this, keyboardStateHolder)
    }


    override fun onDestroy() {
        logDebug { "keyboard onDestroy" }
        super.onDestroy()
    }
}
