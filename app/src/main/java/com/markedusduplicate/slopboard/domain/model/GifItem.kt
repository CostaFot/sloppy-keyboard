package com.markedusduplicate.slopboard.domain.model

/** A single gif result: a small [previewUrl] for the tray and the [gifUrl] inserted on pick. */
data class GifItem(
    val previewUrl: String,
    val gifUrl: String,
    val description: String,
)
