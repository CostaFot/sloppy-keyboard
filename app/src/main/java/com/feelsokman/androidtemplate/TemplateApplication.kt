package com.feelsokman.androidtemplate

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.feelsokman.androidtemplate.core.initialize.AppInitializer
import com.feelsokman.androidtemplate.domain.JsonPlaceHolderRepository
import com.feelsokman.common.coroutine.DispatcherProvider
import com.feelsokman.logging.logDebug
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class TemplateApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var appInitializer: AppInitializer

    @Inject
    lateinit var workerConfiguration: Configuration

    override fun onCreate() {
        super.onCreate()
        appInitializer.startup()
        logDebug { "onCreate application" }
    }

    override val workManagerConfiguration: Configuration
        get() = workerConfiguration

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationEntryPoint {
    fun appInitializer(): AppInitializer

    fun dispatcherProvider(): DispatcherProvider
    fun jsonPlaceHolderRepository(): JsonPlaceHolderRepository
}
