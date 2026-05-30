package com.feelokman.common.test

import com.feelsokman.common.di.RunningUiTestFlag
import com.feelsokman.common.di.UiTestFlagModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UiTestFlagModule::class],
)
object OverrideUiTestFlagModule {
    @Singleton
    @Provides
    @RunningUiTestFlag
    fun providesRunningUiTest(): Boolean = true
}
