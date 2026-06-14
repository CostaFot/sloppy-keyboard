package com.markedusduplicate.slopboard.slop

import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.accessibility.ScreenshotCapturer
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.OcrPrompt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the text currently on screen for the slop detector via screenshot OCR: it grabs the screen
 * (through the accessibility service's [ScreenshotCapturer]) and asks the on-device multimodal model
 * ([LlmEngine.generateWithImage]) to transcribe it. A screenshot is inherently the visible viewport
 * only, so this captures just what the user can see — no off-screen feed scrollback to wade through.
 */
@Singleton
class ScreenTextReader @Inject constructor(
    private val screenshotCapturer: ScreenshotCapturer,
    private val engine: LlmEngine,
) {

    suspend fun read(): ScreenReadResult {
        if (!screenshotCapturer.isAvailable) {
            return ScreenReadResult.Unavailable("Turn on the accessibility service so I can read your screen.")
        }
        if (engine.engineOrNull() == null) {
            return ScreenReadResult.Unavailable("My brain isn't loaded yet (no model). Give me a sec.")
        }
        val jpeg = screenshotCapturer.capture()
            ?: return ScreenReadResult.Unavailable("I couldn't grab the screen, awkward.")
        val raw = engine.generateWithImage(jpeg, OcrPrompt.transcribe())
            ?: return ScreenReadResult.Unavailable("I couldn't read the screen. Try again.")
        logDebug { "ocr raw: $raw" }
        val text = OcrPrompt.clean(raw)
        return if (text.isEmpty()) {
            ScreenReadResult.Unavailable("I didn't find any text to check.")
        } else {
            ScreenReadResult.Text(text)
        }
    }
}
