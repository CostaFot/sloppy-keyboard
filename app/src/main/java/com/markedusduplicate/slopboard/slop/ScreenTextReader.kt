package com.markedusduplicate.slopboard.slop

/**
 * Reads the text currently on screen for the slop detector. Two strategies sit behind this seam,
 * selected via Hilt qualifiers in [com.markedusduplicate.slopboard.di.ScreenTextModule]:
 * [OcrScreenTextReader] (screenshot OCR — in use) and [AccessibilityScreenTextReader] (a custom
 * accessibility-tree extraction — TODO).
 */
interface ScreenTextReader {
    suspend fun read(): ScreenReadResult
}
