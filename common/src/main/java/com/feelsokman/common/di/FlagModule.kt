package com.feelsokman.common.di

import com.feelsokman.common.FlagProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FlagModule {

    @Singleton
    @Provides
    fun providesFlagProvider(
        @DebugFlag debugFlag: Boolean,
        @RunningUiTestFlag uiTestFlag: Boolean
    ): FlagProvider = object : FlagProvider {
        override val isDebugEnabled: Boolean
            get() = debugFlag
        override val isRunningUiTest: Boolean
            get() = uiTestFlag
    }
}
