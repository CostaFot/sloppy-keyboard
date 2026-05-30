package com.feelsokman.common.di

import android.content.Context
import com.feelsokman.common.ConnectivityManagerNetworkMonitor
import com.feelsokman.common.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Singleton
    @Provides
    fun providesNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor = ConnectivityManagerNetworkMonitor(context)

}
