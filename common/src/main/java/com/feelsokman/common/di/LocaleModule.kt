package com.feelsokman.common.di

import android.app.Application
import com.feelsokman.common.AppLocaleManager
import com.feelsokman.common.ProdLocaleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocaleModule {

    @Singleton
    @Provides
    fun providesAppLocaleManager(
        application: Application
    ): AppLocaleManager = ProdLocaleManager(application)
}
