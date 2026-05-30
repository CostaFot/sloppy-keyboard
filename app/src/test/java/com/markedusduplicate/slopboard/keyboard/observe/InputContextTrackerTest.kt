package com.markedusduplicate.slopboard.keyboard.observe

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputContextTrackerTest {

    private fun editorInfo(inputType: Int) = EditorInfo().apply { this.inputType = inputType }

    @Test
    fun `plain text fields allow learning`() {
        val tracker = InputContextTracker()
        tracker.onStartInput(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL))
        assertTrue(tracker.allowed.value)
    }

    @Test
    fun `password fields disable learning`() {
        val tracker = InputContextTracker()
        tracker.onStartInput(
            editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        )
        assertFalse(tracker.allowed.value)
    }

    @Test
    fun `no-suggestions flag disables learning`() {
        val tracker = InputContextTracker()
        tracker.onStartInput(
            editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        )
        assertFalse(tracker.allowed.value)
    }

    @Test
    fun `starting a new field clears the prior text`() {
        val tracker = InputContextTracker()
        tracker.updateText("leftover")
        tracker.onStartInput(editorInfo(InputType.TYPE_CLASS_TEXT))
        assertEquals("", tracker.textBeforeCursor.value)
    }
}
