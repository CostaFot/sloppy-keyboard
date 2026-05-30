package com.feelsokman.slopboard.keyboard

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.feelsokman.design.theme.AppTheme
import com.feelsokman.slopboard.R
import com.feelsokman.slopboard.keyboard.first.FirstScreen
import com.feelsokman.slopboard.keyboard.second.SecondScreen
import com.feelsokman.slopboard.retain.rememberRetainDecorator
import timber.log.Timber

data object First

data class Second(val id: String)

/**
 * The keyboard's root view. [AbstractComposeView] manages its own [androidx.compose.runtime.Recomposer]
 * and composition, hooking into the view-tree lifecycle owner that [SlopboardKeyboardService] sets on
 * the window decor view — so there's no manual Recomposer wiring here.
 */
class KeyboardComposeView(
    context: Context,
    private val keyboardStateHolder: KeyboardStateHolder,
) : AbstractComposeView(context) {

    init {
        id = R.id.keyboardComposeView
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
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
                    val backStack = keyboardStateHolder.backStack

                    NavDisplay(
                        entryDecorators = listOf(rememberRetainDecorator()),
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<First> {
                                FirstScreen(
                                    goNext = {
                                        backStack.add(Second("123"))
                                    }
                                )
                            }
                            entry<Second> { key ->
                                SecondScreen(
                                    goBack = {
                                        backStack.removeLastOrNull()
                                    }
                                )
                            }
                        }
                    )
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
