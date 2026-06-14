package com.markedusduplicate.slopboard.agent

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the ambient screen assistant: read the foreground app's actionable elements
 * ([ScreenController]), ask the LLM ([LlmEngine]) for the single most useful next action over that
 * element list (no goal, no pixel coordinates) and surface it as a [AgentState.Suggest] — Clippy
 * highlights the target and offers to do it. On accept it performs the action, then suggests the next
 * one, forming a guided loop. Nothing runs without the user's tap.
 *
 * App-singleton so a run survives the overlay being shown/hidden; the loop runs on the
 * [ApplicationCoroutineScope].
 */
@Singleton
class AgentEngine @Inject constructor(
    private val engine: LlmEngine,
    private val screenController: ScreenController,
    private val dispatcherProvider: DispatcherProvider,
    @ApplicationCoroutineScope private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var nodes: List<ActionableNode> = emptyList()

    /** Begin suggesting from the current screen. */
    fun start() {
        if (_state.value == AgentState.Idle) suggestNext()
    }

    fun close() {
        loopJob?.cancel()
        _state.value = AgentState.Idle
    }

    /** Perform the suggested action, then suggest the next one. */
    fun accept() {
        val current = _state.value as? AgentState.Suggest ?: return
        val action = current.action
        loopJob?.cancel()
        loopJob = appScope.launch(dispatcherProvider.ui) {
            _state.value = AgentState.Acting
            if (!screenController.perform(action, nodes)) {
                _state.value = AgentState.Failed("That action didn't go through.")
                return@launch
            }
            delay(SETTLE_MS)
            suggestNext()
        }
    }

    private fun suggestNext() {
        loopJob?.cancel()
        loopJob = appScope.launch(dispatcherProvider.ui) {
            _state.value = AgentState.Thinking
            if (!screenController.isAvailable) {
                _state.value = AgentState.Failed("Enable the accessibility service so I can see the screen.")
                return@launch
            }
            if (engine.engineOrNull() == null) {
                _state.value = AgentState.Failed("The model isn't loaded yet.")
                return@launch
            }
            val snapshot = screenController.snapshot()
            if (snapshot == null || snapshot.nodes.isEmpty()) {
                _state.value = AgentState.Failed("I couldn't read anything actionable on screen.")
                return@launch
            }
            nodes = snapshot.nodes
            val raw = engine.generate(AgentPrompt.nextBestAction(snapshot.packageName, nodes))
            if (raw == null) {
                _state.value = AgentState.Failed("Inference failed.")
                return@launch
            }
            logDebug { "agent raw: $raw" }
            _state.value = when (val action = AgentAction.parse(raw)) {
                is AgentAction.Done -> AgentState.Done(action.message.ifBlank { "Nothing useful to do here." })
                is AgentAction.Unknown -> AgentState.Failed("I didn't understand the plan: ${raw.take(140)}")
                else -> AgentState.Suggest(action, label(action), targetOf(action))
            }
        }
    }

    private fun targetOf(action: AgentAction): Bounds? = when (action) {
        is AgentAction.Tap -> nodes.getOrNull(action.index)?.bounds
        is AgentAction.Type -> nodes.getOrNull(action.index)?.bounds
        else -> null
    }

    private fun label(action: AgentAction): String = when (action) {
        is AgentAction.Tap -> "Tap \"${nodeLabel(action.index)}\""
        is AgentAction.Type -> "Type \"${action.text}\" into \"${nodeLabel(action.index)}\""
        is AgentAction.Scroll -> "Scroll ${action.direction.name.lowercase()}"
        AgentAction.Back -> "Go back"
        is AgentAction.Done -> "Done"
        is AgentAction.Unknown -> "?"
    }

    private fun nodeLabel(index: Int): String =
        nodes.getOrNull(index)?.label?.takeIf { it.isNotEmpty() } ?: "#$index"

    private companion object {
        const val SETTLE_MS = 900L
    }
}
