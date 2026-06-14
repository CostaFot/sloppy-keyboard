package com.markedusduplicate.slopboard.slop

import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolates the main post / article text from a noisy screen capture (UI chrome, several stacked
 * posts) so the AI-detector judges real content, not buttons and author names. Asks the on-device
 * [LlmEngine] which captured lines are the content and rebuilds the text **verbatim** from those
 * original lines via [ContentExtractionPrompt] — the model never rewrites the text, which would
 * otherwise bias detection toward "AI". Falls back to a heuristic largest-block (and ultimately the
 * raw text) when the model is unavailable or its answer doesn't hold up.
 */
@Singleton
class ContentExtractor @Inject constructor(
    private val engine: LlmEngine,
) {

    suspend fun extract(rawText: String): String {
        val lines = rawText.lines()
        if (lines.size <= MIN_LINES_TO_EXTRACT) return rawText.trim()

        val capped = lines.take(MAX_LINES)
        return selectViaLlm(capped) ?: ContentExtractionPrompt.largestBlock(rawText)
    }

    private suspend fun selectViaLlm(lines: List<String>): String? {
        if (engine.engineOrNull() == null) return null
        val raw = engine.generate(ContentExtractionPrompt.select(lines)) ?: return null
        val indices = ContentExtractionPrompt.parse(raw)
        if (indices.isEmpty()) return null
        return ContentExtractionPrompt.reconstruct(lines, indices).takeIf { it.length >= MIN_RESULT_CHARS }
    }

    private companion object {
        const val MIN_LINES_TO_EXTRACT = 4
        const val MAX_LINES = 120
        const val MIN_RESULT_CHARS = 20
    }
}
