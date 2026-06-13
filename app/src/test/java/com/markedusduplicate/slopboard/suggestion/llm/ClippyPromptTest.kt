package com.markedusduplicate.slopboard.suggestion.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClippyPromptTest {

    @Test
    fun `roast prompt asks for one snarky in-character remark`() {
        val prompt = ClippyPrompt.roast()
        assertTrue(prompt.contains("Clippy"))
        assertTrue(prompt.contains("screenshot"))
        assertTrue(prompt.contains("It looks like"))
    }

    @Test
    fun `clean trims whitespace and surrounding quotes`() {
        assertEquals("It looks like you're stalling.", ClippyPrompt.clean("  \"It looks like you're stalling.\" "))
    }

    @Test
    fun `clean leaves a plain remark untouched`() {
        assertEquals("It looks like you're busy.", ClippyPrompt.clean("It looks like you're busy."))
    }
}
