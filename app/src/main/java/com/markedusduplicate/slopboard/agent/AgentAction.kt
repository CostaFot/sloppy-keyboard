package com.markedusduplicate.slopboard.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Direction for a [AgentAction.Scroll]. */
enum class ScrollDirection { UP, DOWN }

/**
 * The single next step the agent model chose, parsed from its JSON reply. Tap/Type reference a node
 * by its snapshot [index] (see [ActionableNode]); the rest are global. Pure + tolerant of the usual
 * model sloppiness (code fences, prose around the JSON) so it's unit-testable.
 */
sealed interface AgentAction {
    data class Tap(val index: Int) : AgentAction
    data class Type(val index: Int, val text: String) : AgentAction
    data class Scroll(val direction: ScrollDirection) : AgentAction
    data object Back : AgentAction
    data class Done(val message: String) : AgentAction
    data class Unknown(val raw: String) : AgentAction

    companion object {
        private val json = Json { isLenient = true; ignoreUnknownKeys = true }

        fun parse(raw: String): AgentAction {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start == -1 || end <= start) return Unknown(raw)
            val obj = runCatching {
                json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
            }.getOrNull() ?: return Unknown(raw)

            fun str(key: String): String? = runCatching { obj[key]?.jsonPrimitive?.content }.getOrNull()
            fun int(key: String): Int? = str(key)?.trim()?.toIntOrNull()

            return when (str("action")?.trim()?.lowercase()) {
                "tap", "click" -> int("index")?.let { Tap(it) } ?: Unknown(raw)
                "type", "settext", "input" -> {
                    val i = int("index")
                    val text = str("text")
                    if (i != null && text != null) Type(i, text) else Unknown(raw)
                }

                "scroll" -> Scroll(
                    if (str("direction")?.trim()?.lowercase() == "up") ScrollDirection.UP
                    else ScrollDirection.DOWN,
                )

                "back" -> Back
                "done", "finish", "stop" -> Done(str("message").orEmpty())
                else -> Unknown(raw)
            }
        }
    }
}
