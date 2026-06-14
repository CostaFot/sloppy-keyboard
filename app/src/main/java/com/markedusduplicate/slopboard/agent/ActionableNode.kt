package com.markedusduplicate.slopboard.agent

/**
 * Screen coordinates of a node, kept as plain ints (no `android.graphics.Rect`) so the agent model
 * and prompt stay unit-testable without the Android framework.
 */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

/**
 * One thing on screen the agent can act on, as distilled from the accessibility tree. The [index] is
 * the node's position in the snapshot list — the LLM refers to elements by this index, so it never
 * has to reason about pixel coordinates (the accessibility tree is the action space).
 */
data class ActionableNode(
    val index: Int,
    val label: String,
    val role: String,
    val bounds: Bounds,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
)

/** A single read of the foreground app: its package and the elements the agent may act on. */
data class ScreenSnapshot(
    val packageName: String,
    val nodes: List<ActionableNode>,
)
