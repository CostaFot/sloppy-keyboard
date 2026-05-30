package com.feelsokman.androidtemplate.core.initialize

import com.feelsokman.common.AppLocaleManager
import com.feelsokman.common.FlagProvider
import com.feelsokman.logging.logDebug
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppInitializer @Inject constructor(
    private val featureFlagProvider: FlagProvider,
    private val appLocaleManager: AppLocaleManager
) {

    private val isInitialized = AtomicBoolean(false)

    fun startup() {
        check(!isInitialized.get()) { "Attempted to initialize app more than once" }
        initLogger()
        appLocaleManager.initialise()
        isInitialized.set(true)
    }

    private fun initLogger() {
        if (featureFlagProvider.isDebugEnabled) {
            Timber.plant(Timber.DebugTree())
            logDebug { "Logger initialised" }
        }
    }

}
