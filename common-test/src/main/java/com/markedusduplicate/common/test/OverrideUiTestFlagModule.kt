package com.markedusduplicate.common.test

import com.markedusduplicate.common.di.RunningUiTestFlag
import com.markedusduplicate.common.di.UiTestFlagModule
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
