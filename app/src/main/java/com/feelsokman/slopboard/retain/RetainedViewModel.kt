package com.feelsokman.slopboard.retain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.platform.LocalContext
import com.feelsokman.logging.logDebug
import com.feelsokman.slopboard.ui.Hello
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

abstract class RetainedViewModel : RetainObserver {
    val viewModelScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onEnteredComposition() {
        logDebug { "onEnteredComposition" }
    }

    override fun onExitedComposition() {
        logDebug { "onExitedComposition" }
    }

    override fun onRetained() {
        logDebug { "onRetained" }
    }

    override fun onUnused() {
        logDebug { "onUnused" }
    }

    override fun onRetired() {
        logDebug { "onRetired" }
        clear()
    }

    private fun clear() {
        onCleared()
        viewModelScope.cancel()
    }

    protected open fun onCleared() {
        // override if necessary
    }
}

@Composable
inline fun <reified T : RetainedViewModel> rememberRetainedViewModel(noinline factory: (Context) -> T): T {
    val context = LocalContext.current
    return retain { factory(context) }
}

@EntryPoint
// can only work with ActivityComponent
@InstallIn(ActivityComponent::class)
interface SampleEntryPoint {
    fun sampleRetainedViewModel(): SampleRetainedViewModel
}

class SampleRetainedViewModel @Inject constructor(
    private val hello: Hello
) : RetainedViewModel() {
    private val _state = MutableStateFlow(false)
    val state: StateFlow<Boolean> = _state.asStateFlow()

    init {
        logDebug { "init SampleRetainedViewModel ${hashCode()}" }
    }

    override fun onCleared() {
        logDebug { "onCleared SampleRetainedViewModel ${hashCode()}" }
    }
}
