package com.markedusduplicate.slopboard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Giphy `/v1/gifs/search` response. Fields are nullable to tolerate partial payloads. */
@Serializable
data class GiphySearchResponse(
    val data: List<GiphyGif> = emptyList(),
)

@Serializable
data class GiphyGif(
    val id: String? = null,
    val title: String? = null,
    val images: GiphyImages? = null,
)

@Serializable
data class GiphyImages(
    @SerialName("fixed_width_small") val fixedWidthSmall: GiphyRendition? = null,
    @SerialName("downsized") val downsized: GiphyRendition? = null,
    @SerialName("original") val original: GiphyRendition? = null,
)

@Serializable
data class GiphyRendition(
    val url: String? = null,
)
