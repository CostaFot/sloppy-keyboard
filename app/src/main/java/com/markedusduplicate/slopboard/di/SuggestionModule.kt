package com.markedusduplicate.slopboard.di

import com.markedusduplicate.slopboard.suggestion.NgramSuggestionSource
import com.markedusduplicate.slopboard.suggestion.SuggestionSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the suggestion seam to its current (DB-backed) implementation. Swap or compose with an
 * LLM-backed source here when LiteRT-LM lands.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SuggestionModule {

    @Binds
    fun bindsSuggestionSource(impl: NgramSuggestionSource): SuggestionSource
}
