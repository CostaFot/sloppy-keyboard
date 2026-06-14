package com.markedusduplicate.slopboard.slop

/**
 * Builds the "which lines are the real content?" prompt and parses the model's reply into line
 * indices, plus pure helpers to rebuild the selected text verbatim and a heuristic fallback.
 *
 * The model only ever picks line numbers — it never reproduces the text — so the bytes handed onward
 * stay exactly the user's own. That matters: AI-detectors score exact tokens, so letting the model
 * rewrite or "clean up" the text would bias the verdict toward "AI". Mirrors the index-based
 * [com.markedusduplicate.slopboard.agent.AgentPrompt] pattern. Pure (no LiteRT types) so it's
 * unit-testable.
 */
object ContentExtractionPrompt {

    private val RANGE = Regex("(\\d+)\\s*-\\s*(\\d+)")
    private val INTEGER = Regex("\\d+")

    fun select(lines: List<String>): String = buildString {
        append("You are looking at the raw text captured from a phone screen, one item per line.\n")
        append("Each line is numbered. Identify the lines that make up the main post or article — ")
        append("the actual written content a person wrote.\n")
        append("EXCLUDE interface text: buttons (Like, Share, Follow, Comment), author names and job ")
        append("titles, connection degrees (2nd, 3rd), reactions (\"X likes this\"), comment / repost ")
        append("counts, standalone hashtags, image descriptions, and separators.\n")
        append("If several posts are present, choose only the single most prominent one.\n")
        append("Reply with ONLY a JSON array of the line numbers, e.g. [3,4,5]. No other text.\n\n")
        lines.forEachIndexed { index, line -> append(index).append(": ").append(line).append('\n') }
    }

    /** Parses [raw] into selected line indices: tolerant of a JSON array, a loose list, or ranges. */
    fun parse(raw: String): List<Int> {
        val indices = sortedSetOf<Int>()
        for (match in RANGE.findAll(raw)) {
            val from = match.groupValues[1].toIntOrNull() ?: continue
            val to = match.groupValues[2].toIntOrNull() ?: continue
            if (from <= to && to - from < MAX_RANGE_SPAN) indices.addAll(from..to)
        }
        for (match in INTEGER.findAll(raw)) {
            match.value.toIntOrNull()?.let(indices::add)
        }
        return indices.toList()
    }

    /** Joins the in-range [indices] of [lines] back into text, verbatim and in order. */
    fun reconstruct(lines: List<String>, indices: List<Int>): String =
        indices.filter { it in lines.indices }
            .joinToString("\n") { lines[it] }
            .trim()

    /** Heuristic fallback: the longest blank-line-delimited block of [text]. */
    fun largestBlock(text: String): String =
        text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .maxByOrNull { it.length }
            ?: text.trim()

    private const val MAX_RANGE_SPAN = 1000
}
