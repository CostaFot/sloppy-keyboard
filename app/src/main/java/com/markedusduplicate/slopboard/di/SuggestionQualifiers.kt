package com.markedusduplicate.slopboard.di

import javax.inject.Qualifier

/** The instant, on-device n-gram suggestion source. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DbSuggestions

/** The slower LiteRT-LM suggestion source that refines the n-gram suggestions. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LlmSuggestions
