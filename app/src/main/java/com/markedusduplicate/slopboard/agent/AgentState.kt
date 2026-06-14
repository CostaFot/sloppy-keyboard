package com.markedusduplicate.slopboard.agent

/**
 * The suggestion loop's UI state. [Idle] = nothing showing. A cycle is Thinking → Suggest → (on
 * accept) Acting → Thinking… It never auto-acts: [Suggest] waits for the user to tap the spotlight or
 * the "Do it" chip. [Suggest.target] is the on-screen bounds to highlight (null for scroll/back,
 * which have no single element).
 */
sealed interface AgentState {
    data object Idle : AgentState
    data object Thinking : AgentState
    data class Suggest(val action: AgentAction, val label: String, val target: Bounds?) : AgentState
    data object Acting : AgentState
    data class Done(val message: String) : AgentState
    data class Failed(val message: String) : AgentState
}
