package com.markedusduplicate.slopboard.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 * An [InputMethodService] that is also a real [LifecycleOwner], with its lifecycle driven by the
 * service's own callbacks via [ServiceLifecycleDispatcher]. This lets Compose (through
 * `AbstractComposeView`) manage its own composition/recomposition tied to the service lifecycle,
 * instead of us spinning up a manual `Recomposer`.
 */
abstract class LifecycleInputMethodService : InputMethodService(), LifecycleOwner {

    protected val dispatcher = ServiceLifecycleDispatcher(this)

    @CallSuper
    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onBindInput() {
        super.onBindInput()
        dispatcher.onServicePreSuperOnBind()
    }

    // Annotated @CallSuper only. In a plain service super.onStartCommand is a no-op, but here it
    // results in dispatcher.onServicePreSuperOnStart() because super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
