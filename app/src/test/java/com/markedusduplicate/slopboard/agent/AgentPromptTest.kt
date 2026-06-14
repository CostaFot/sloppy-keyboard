package com.markedusduplicate.slopboard.agent

import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPromptTest {

    private val nodes = listOf(
        ActionableNode(0, "Message", "EditText", Bounds(0, 0, 100, 50), clickable = true, editable = true, scrollable = false),
        ActionableNode(1, "Send", "Button", Bounds(0, 60, 100, 110), clickable = true, editable = false, scrollable = false),
    )

    @Test
    fun `prompt names the app and numbers the elements with editable hints`() {
        val prompt = AgentPrompt.nextBestAction("com.example.chat", nodes)
        assertTrue(prompt.contains("com.example.chat"))
        assertTrue(prompt.contains("0: [EditText] \"Message\" (editable)"))
        assertTrue(prompt.contains("1: [Button] \"Send\""))
    }

    @Test
    fun `prompt asks for the single next action and states the JSON schema`() {
        val prompt = AgentPrompt.nextBestAction("y", nodes)
        assertTrue(prompt.contains("SINGLE most useful next action"))
        assertTrue(prompt.contains("\"action\":\"tap\""))
        assertTrue(prompt.contains("\"action\":\"done\""))
    }
}
