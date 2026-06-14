package com.markedusduplicate.slopboard.keyboard.vision

import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.accessibility.ScreenshotCapturer
import com.markedusduplicate.slopboard.keyboard.KeyboardHandler
import com.markedusduplicate.slopboard.keyboard.KeyboardMessage
import com.markedusduplicate.slopboard.retain.RetainedViewModel
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.VisionPrompt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the vision screen shows: working, the drafted reply, an error, or accessibility-off. */
sealed interface VisionUiState {
    data object Loading : VisionUiState
    data object Unavailable : VisionUiState
    data class Ready(val reply: String) : VisionUiState
    data class Failed(val message: String) : VisionUiState
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VisionViewModelEntryPoint {
    fun visionViewModel(): VisionViewModel
}

/**
 * Captures a screenshot (via the accessibility service) and asks the multimodal LLM to draft a reply
 * from what's visible, then commits the accepted reply into the field. Capture + inference run on the
 * IO dispatcher.
 */
class VisionViewModel @Inject constructor(
    private val engine: LlmEngine,
    private val screenshotCapturer: ScreenshotCapturer,
    private val keyboardHandler: KeyboardHandler,
) : RetainedViewModel() {

    private val _state = MutableStateFlow<VisionUiState>(VisionUiState.Loading)
    val state: StateFlow<VisionUiState> = _state.asStateFlow()

    fun generate() {
        _state.value = VisionUiState.Loading
        viewModelScope.launch { _state.value = draftReply() }
    }

    private suspend fun draftReply(): VisionUiState {
        if (!screenshotCapturer.isAvailable) return VisionUiState.Unavailable
        val jpeg = screenshotCapturer.capture()
            ?: return VisionUiState.Failed("Couldn't capture the screen")
        if (engine.engineOrNull() == null) return VisionUiState.Failed("Model still loading…")
        val raw = engine.generateWithImage(jpeg, VisionPrompt.reply())
            ?: return VisionUiState.Failed("Vision inference failed")
        logDebug { "vision raw: $raw" }
        val reply = VisionPrompt.clean(raw)
        return if (reply.isEmpty()) VisionUiState.Failed("No reply produced") else VisionUiState.Ready(reply)
    }

    fun commit(reply: String) {
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.Text(reply))
        }
    }
}
