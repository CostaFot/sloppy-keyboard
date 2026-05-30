package com.feelsokman.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

interface AppLocaleManager {
    val currentLocale: StateFlow<Locale>
    fun initialise()
}

class ProdLocaleManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, AppLocaleManager {

    private val _currentLocale = MutableStateFlow(Locale("en", "US"))
    override val currentLocale: StateFlow<Locale> = _currentLocale

    override fun initialise() {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val language = activity.getString(R.string.app_language)
        val region = activity.getString(R.string.app_region)
        val locale = Locale(language, region)
        _currentLocale.update { locale }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

}
