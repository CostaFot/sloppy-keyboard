package com.markedusduplicate.slopboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.R
import timber.log.Timber

@SuppressLint("ViewConstructor")
class KeyboardComposeView(
    context: Context,
    private val keyboardStateHolder: KeyboardStateHolder,
) : AbstractComposeView(context) {

    init {
        id = R.id.keyboardComposeView
        addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    Timber.tag("KeyboardComposeView").d("onViewAttachedToWindow")
                }

                override fun onViewDetachedFromWindow(v: View) {
                    Timber.tag("KeyboardComposeView").d("onViewDetachedFromWindow")
                }
            }
        )
    }

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner,
        ) {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KeyboardNavHost(keyboardStateHolder)
                }

                DisposableEffect(Unit) {
                    Timber.tag("KeyboardComposeView").d("Keyboard entered composition")
                    onDispose {
                        Timber.tag("KeyboardComposeView").d("Keyboard left composition")
                    }
                }
            }
        }
    }
}

internal val FakeNavigationEventDispatcherOwner: NavigationEventDispatcherOwner =
    object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher =
            NavigationEventDispatcher()
    }
