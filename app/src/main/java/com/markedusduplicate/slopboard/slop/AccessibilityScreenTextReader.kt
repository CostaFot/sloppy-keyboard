package com.markedusduplicate.slopboard.slop

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder for a custom accessibility-tree [ScreenTextReader] — an alternative to
 * [OcrScreenTextReader] that pulls the best text straight out of the a11y node tree (faster, no model
 * needed). Not implemented yet.
 */
@Singleton
class AccessibilityScreenTextReader @Inject constructor() : ScreenTextReader {

    // TODO: custom accessibility-tree extraction. Read the foreground app's a11y tree on demand
    //  (needs a bridge to SlopboardAccessibilityService, like the old ScreenTextCapturer) and select
    //  the best content text. A naive whole-tree + viewport-bounds pass over-captured feed scrollback
    //  (LinkedIn reports off-screen nodes as visible), so this needs a smarter strategy.
    override suspend fun read(): ScreenReadResult =
        ScreenReadResult.Unavailable("Accessibility-tree reader not implemented yet.")
}
