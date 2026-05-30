package com.feelsokman.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.feelsokman.common.FlagProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkModule {

    @Provides
    @Singleton
    fun providesWorkManager(
        @ApplicationContext context: Context,
        flagProvider: FlagProvider,
        configuration: Configuration
    ): WorkManager {
        if (flagProvider.isRunningUiTest) {
            WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        }
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideWorkManagerConfiguration(
        flagProvider: FlagProvider,
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder().apply {
            if (flagProvider.isDebugEnabled) {
                setMinimumLoggingLevel(android.util.Log.DEBUG)
            }
            setWorkerFactory(workerFactory)
        }.build()
    }
}
