package com.markedusduplicate.slopboard.slop

/**
 * Verdict on whether a piece of text is AI-generated ("slop"). Provider-agnostic: the shape the rest
 * of the app consumes regardless of which detection backend produced it.
 */
data class SlopVerdict(
    val isAi: Boolean,
    val aiLikelihood: Double,
    val summary: String,
)
