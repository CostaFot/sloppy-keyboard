package com.feelsokman.common.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationCoroutineScope


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DebugFlag

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RunningUiTestFlag
