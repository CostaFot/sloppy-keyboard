package com.feelsokman.androidtemplate.di

import android.content.Context
import android.content.res.Resources
import com.feelsokman.androidtemplate.BuildConfig
import com.feelsokman.common.di.DebugFlag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesApplicationResources(@ApplicationContext context: Context): Resources {
        return context.resources
    }

    @Singleton
    @Provides
    @DebugFlag
    fun providesDebugFlag(): Boolean = BuildConfig.DEBUG
}
