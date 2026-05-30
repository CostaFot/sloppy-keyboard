package com.feelsokman.common.di

import com.feelsokman.common.coroutine.DefaultApplicationCoroutineScope
import com.feelsokman.common.coroutine.DefaultDispatcherProvider
import com.feelsokman.common.coroutine.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Singleton
    @Provides
    @ApplicationCoroutineScope
    fun providesApplicationScope(): CoroutineScope = DefaultApplicationCoroutineScope

    @Singleton
    @Provides
    fun providesDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

}

