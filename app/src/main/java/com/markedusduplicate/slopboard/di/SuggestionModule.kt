package com.markedusduplicate.slopboard.di

import com.markedusduplicate.slopboard.suggestion.LlmSuggestionSource
import com.markedusduplicate.slopboard.suggestion.NgramSuggestionSource
import com.markedusduplicate.slopboard.suggestion.SuggestionSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the two suggestion sources the coordinator blends: the instant DB source and the LLM source. */
@Module
@InstallIn(SingletonComponent::class)
interface SuggestionModule {

    @Binds
    @DbSuggestions
    fun bindsDbSuggestionSource(impl: NgramSuggestionSource): SuggestionSource

    @Binds
    @LlmSuggestions
    fun bindsLlmSuggestionSource(impl: LlmSuggestionSource): SuggestionSource
}
