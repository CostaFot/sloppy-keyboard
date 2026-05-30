package com.feelsokman.androidtemplate.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class CustomViewModel {
    open val viewModelScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    open fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
inline fun <reified T : CustomViewModel> customViewModel(
    viewModelStoreOwner: CustomViewModelStoreOwner =
        checkNotNull(LocalCustomViewModelStoreOwner.current) {
            "No CustomViewModelOwner was provided via LocalCustomViewModelOwner"
        },
    key: Any = LocalKeyProvider.current!!
): T = remember(key) {
    viewModelStoreOwner.get(key)
}
