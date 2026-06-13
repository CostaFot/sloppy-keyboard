package com.markedusduplicate.slopboard.di

import com.markedusduplicate.slopboard.suggestion.DictionarySuggestionSource
import com.markedusduplicate.slopboard.suggestion.LlmSuggestionSource
import com.markedusduplicate.slopboard.suggestion.SuggestionSource
import com.markedusduplicate.slopboard.suggestion.dictionary.Dictionary
import com.markedusduplicate.slopboard.suggestion.dictionary.DictionaryEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the two suggestion sources the coordinator blends: the instant dictionary and the LLM. */
@Module
@InstallIn(SingletonComponent::class)
interface SuggestionModule {

    @Binds
    @DictionarySuggestions
    fun bindsDictionarySuggestionSource(impl: DictionarySuggestionSource): SuggestionSource

    @Binds
    @LlmSuggestions
    fun bindsLlmSuggestionSource(impl: LlmSuggestionSource): SuggestionSource

    @Binds
    fun bindsDictionary(impl: DictionaryEngine): Dictionary
}
