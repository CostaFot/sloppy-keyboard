package com.feelsokman.androidtemplate.keyboard

import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.feelsokman.androidtemplate.R
import com.feelsokman.androidtemplate.keyboard.first.FirstScreen
import com.feelsokman.androidtemplate.keyboard.second.SecondScreen
import com.feelsokman.design.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber


data object First

data class Second(val id: String)

fun createKeyboardComposeView(
    service: TemplateKeyboardService,
    keyboardStateHolder: KeyboardStateHolder
): ComposeView {
    val composeView = ComposeView(service)
    val customLifecycleOwner = CustomLifecycleOwner()

    // nice to have
    composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)


    // stop crash!
    // java.lang.IllegalStateException: You can 'consumeRestoredStateForKey' only after the corresponding component has moved to the 'CREATED' state
    customLifecycleOwner.performRestore(null)

    // nothing will show without this one!
    customLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

    composeView.setViewTreeLifecycleOwner(customLifecycleOwner)
    composeView.setViewTreeSavedStateRegistryOwner(customLifecycleOwner)

    // navigation 3 expects this
    composeView.setViewTreeNavigationEventDispatcherOwner(FakeNavigationEventDispatcherOwner)

    val coroutineContext = AndroidUiDispatcher.CurrentThread
    val recomposeScope = CoroutineScope(coroutineContext)
    val recomposer = Recomposer(coroutineContext)
    composeView.compositionContext = recomposer

    recomposeScope.launch {
        // responsible for re-drawing stuff on the screen
        recomposer.runRecomposeAndApplyChanges()
    }

    composeView.setMainContent(
        keyboardStateHolder = keyboardStateHolder
    )

    return composeView
}

private fun ComposeView.setMainContent(
    keyboardStateHolder: KeyboardStateHolder
) {
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

    setContent {
        CompositionLocalProvider(
            LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner,
            LocalCustomViewModelStoreOwner provides keyboardStateHolder,
        ) {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val backStack = keyboardStateHolder.backStack

                    NavDisplay(
                        entryDecorators = listOf(keyboardStateHolder.keyboardNavEntryDecorator),
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


private val FakeOnBackPressedDispatcherOwner: (lifecycle: Lifecycle) -> OnBackPressedDispatcherOwner =
    {
        object : OnBackPressedDispatcherOwner {
            override val onBackPressedDispatcher = OnBackPressedDispatcher()

            override val lifecycle: LifecycleRegistry
                get() = (it as LifecycleRegistry)
        }
    }


private val FakeNavigationEventDispatcherOwner: NavigationEventDispatcherOwner =
    object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher =
            NavigationEventDispatcher()
    }


