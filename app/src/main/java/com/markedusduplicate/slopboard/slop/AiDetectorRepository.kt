package com.markedusduplicate.slopboard.slop

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.Result
import com.markedusduplicate.common.result.attempt
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic boundary for AI-content ("slop") detection: callers hand it the on-screen text and get back
 * a [SlopVerdict]. Provider-agnostic by design — the concrete detection backend lives behind this
 * repository and can be swapped without touching callers.
 *
 * The first planned backend is Pangram: a Retrofit service provided in
 * [com.markedusduplicate.slopboard.di.NetworkModule], authenticated with
 * `BuildConfig.AI_DETECTOR_API_KEY`, called inside `withContext(dispatcherProvider.io)` and wrapped
 * with `attempt {}` — mirroring [com.markedusduplicate.slopboard.domain.JsonPlaceHolderRepository].
 * Until those API details land, [detect] is a placeholder that reports it isn't wired yet, and is not
 * on the live overlay path.
 */
@Singleton
class AiDetectorRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun detect(text: String): Result<Throwable, SlopVerdict> =
        withContext(dispatcherProvider.io) {
            attempt {
                error("AI-detection backend not wired yet (received ${text.length} chars)")
            }
        }
}
