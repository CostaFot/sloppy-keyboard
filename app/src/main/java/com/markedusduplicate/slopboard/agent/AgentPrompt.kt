package com.markedusduplicate.slopboard.agent

/**
 * Builds the goal-free "what should I do here?" prompt: the foreground app's actionable elements,
 * asking the model for the single most useful next action as JSON. The model picks elements by
 * index, so it never deals in pixel coordinates. Pure (no Android / LiteRT types) so it's
 * unit-testable; parsing lives in [AgentAction.parse].
 */
object AgentPrompt {

    fun nextBestAction(packageName: String, nodes: List<ActionableNode>): String =
        buildString {
            append("You are a proactive phone assistant looking at the user's current screen.\n")
            append("Foreground app: $packageName\n\n")
            append("Actionable elements on screen (refer to them by index):\n")
            for (node in nodes) {
                append(node.index)
                append(": [").append(node.role.ifEmpty { "View" }).append("] ")
                append('"').append(node.label).append('"')
                if (node.editable) append(" (editable)")
                if (node.scrollable) append(" (scrollable)")
                append('\n')
            }
            append("\nSuggest the SINGLE most useful next action the user probably wants here.\n")
            append("If nothing is clearly useful, choose done.\n")
            append("Reply with ONLY a JSON object, no prose:\n")
            append("{\"action\":\"tap\",\"index\":N}\n")
            append("{\"action\":\"type\",\"index\":N,\"text\":\"...\"}   (N must be editable)\n")
            append("{\"action\":\"scroll\",\"direction\":\"down\"}   (or \"up\")\n")
            append("{\"action\":\"back\"}\n")
            append("{\"action\":\"done\",\"message\":\"...\"}")
        }
}
