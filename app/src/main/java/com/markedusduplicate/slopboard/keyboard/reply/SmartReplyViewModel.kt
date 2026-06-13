package com.markedusduplicate.slopboard.keyboard.reply

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.accessibility.ScreenContextHolder
import com.markedusduplicate.slopboard.keyboard.KeyboardHandler
import com.markedusduplicate.slopboard.keyboard.KeyboardMessage
import com.markedusduplicate.slopboard.retain.RetainedViewModel
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.SmartReplyPrompt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What the reply screen shows: a spinner, the drafted reply, an error, or "nothing captured yet". */
sealed interface SmartReplyUiState {
    data object Loading : SmartReplyUiState
    data object NoContext : SmartReplyUiState
    data class Ready(val reply: String) : SmartReplyUiState
    data class Failed(val message: String) : SmartReplyUiState
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmartReplyViewModelEntryPoint {
    fun smartReplyViewModel(): SmartReplyViewModel
}

/**
 * Drafts a single reply from the latest captured screen text ([ScreenContextHolder]) via the
 * on-device LLM, and commits the accepted reply into the field. Inference runs on the IO dispatcher
 * so the multi-second call never blocks the UI thread.
 */
class SmartReplyViewModel @Inject constructor(
    private val engine: LlmEngine,
    private val screenContextHolder: ScreenContextHolder,
    private val keyboardHandler: KeyboardHandler,
    private val dispatcherProvider: DispatcherProvider,
) : RetainedViewModel() {

    init {
        logDebug { "SmartReplyViewModel init" }
    }

    private val _state = MutableStateFlow<SmartReplyUiState>(SmartReplyUiState.Loading)
    val state: StateFlow<SmartReplyUiState> = _state.asStateFlow()

    fun generate() {
        val context = screenContextHolder.screenText.value.trim()
        if (context.isEmpty()) {
            _state.value = SmartReplyUiState.NoContext
            return
        }
        _state.value = SmartReplyUiState.Loading
        viewModelScope.launch {
            _state.value = withContext(dispatcherProvider.io) {
                val activeEngine = engine.engineOrNull()
                    ?: return@withContext SmartReplyUiState.Failed("Model still loading…")
                try {
                    activeEngine.createConversation().use { conversation ->
                        val reply = SmartReplyPrompt.clean(
                            conversation.sendMessage(SmartReplyPrompt.reply(context)).toString(),
                        )
                        if (reply.isEmpty()) {
                            SmartReplyUiState.Failed("No reply produced")
                        } else {
                            SmartReplyUiState.Ready(reply)
                        }
                    }
                } catch (t: Throwable) {
                    SmartReplyUiState.Failed(t.message ?: "Inference failed")
                }
            }
        }
    }

    fun commit(reply: String) {
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.Text(reply))
        }
    }

    override fun onCleared() {
        super.onCleared()
        logDebug { "SmartReplyViewModel onCleared" }
    }
}
