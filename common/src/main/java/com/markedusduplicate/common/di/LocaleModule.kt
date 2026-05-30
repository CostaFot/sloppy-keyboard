package com.markedusduplicate.common.di

import android.app.Application
import com.markedusduplicate.common.AppLocaleManager
import com.markedusduplicate.common.ProdLocaleManager
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
