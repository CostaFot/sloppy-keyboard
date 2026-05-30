package com.feelsokman.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiTestFlagModule {
    @Singleton
    @Provides
    @RunningUiTestFlag
    fun providesRunningUiTest(): Boolean = false
}
