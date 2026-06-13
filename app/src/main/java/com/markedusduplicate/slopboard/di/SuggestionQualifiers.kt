package com.markedusduplicate.slopboard.di

import javax.inject.Qualifier

/** The instant, offline dictionary + personal-history suggestion source. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DictionarySuggestions

/** The slower LiteRT-LM source that refines the instant suggestions with context. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LlmSuggestions
