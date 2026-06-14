package com.markedusduplicate.slopboard.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class OcrScreenText

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AccessibilityScreenText
