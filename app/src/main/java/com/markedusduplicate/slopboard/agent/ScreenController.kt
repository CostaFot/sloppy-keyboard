package com.markedusduplicate.slopboard.agent

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implemented by the accessibility service: reads the foreground app's actionable elements and
 * performs a chosen action on them. Only an `AccessibilityService` can do either, so the service
 * registers itself here and the [AgentEngine] drives it through [ScreenController].
 */
interface ScreenAgentHandler {
    suspend fun snapshot(): ScreenSnapshot?
    suspend fun perform(action: AgentAction, nodes: List<ActionableNode>): Boolean
}

/**
 * App-singleton bridge between the accessibility service (which can see and touch the screen) and the
 * agent (which decides what to do). Mirrors
 * [com.markedusduplicate.slopboard.accessibility.ScreenshotCapturer]. [isAvailable] is false when the
 * accessibility service is disabled, in which case calls no-op.
 */
@Singleton
class ScreenController @Inject constructor() {

    @Volatile
    private var handler: ScreenAgentHandler? = null

    val isAvailable: Boolean get() = handler != null

    fun setHandler(handler: ScreenAgentHandler?) {
        this.handler = handler
    }

    suspend fun snapshot(): ScreenSnapshot? = handler?.snapshot()

    suspend fun perform(action: AgentAction, nodes: List<ActionableNode>): Boolean =
        handler?.perform(action, nodes) ?: false
}
