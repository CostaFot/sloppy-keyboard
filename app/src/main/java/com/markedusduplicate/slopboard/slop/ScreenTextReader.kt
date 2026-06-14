package com.markedusduplicate.slopboard.slop

import com.markedusduplicate.slopboard.accessibility.ScreenContextHolder
import com.markedusduplicate.slopboard.accessibility.ScreenshotCapturer
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.OcrPrompt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the text currently on screen for the slop detector. Prefers the accessibility-captured text
 * ([ScreenContextHolder]) — instant and free — and falls back to screenshot OCR through the on-device
 * multimodal model ([LlmEngine.generateWithImage]) only when the accessibility tree yields too little
 * text (e.g. a screen that is mostly an image). The short accessibility text, if any, is still used
 * when OCR isn't possible, so the reader degrades gracefully rather than going silent.
 */
@Singleton
class ScreenTextReader @Inject constructor(
    private val screenContextHolder: ScreenContextHolder,
    private val screenshotCapturer: ScreenshotCapturer,
    private val engine: LlmEngine,
) {

    suspend fun read(): ScreenReadResult {
        val accessibilityText = screenContextHolder.screenText.value.trim()
        if (accessibilityText.length >= MIN_ACCESSIBILITY_CHARS) {
            return ScreenReadResult.Text(accessibilityText)
        }
        val ocrText = ocrOrNull()
        return when {
            ocrText != null -> ScreenReadResult.Text(ocrText)
            accessibilityText.isNotEmpty() -> ScreenReadResult.Text(accessibilityText)
            else -> ScreenReadResult.Unavailable(unavailableReason())
        }
    }

    private suspend fun ocrOrNull(): String? {
        if (!screenshotCapturer.isAvailable || engine.engineOrNull() == null) return null
        val jpeg = screenshotCapturer.capture() ?: return null
        val raw = engine.generateWithImage(jpeg, OcrPrompt.transcribe()) ?: return null
        return OcrPrompt.clean(raw).ifEmpty { null }
    }

    private fun unavailableReason(): String = when {
        !screenshotCapturer.isAvailable ->
            "Turn on the accessibility service so I can read your screen."

        engine.engineOrNull() == null ->
            "My brain isn't loaded yet (no model). Give me a sec."

        else ->
            "I didn't find any text to check."
    }

    private companion object {
        const val MIN_ACCESSIBILITY_CHARS = 40
    }
}
